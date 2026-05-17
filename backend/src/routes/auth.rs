use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::Serialize;

use crate::{db, minecraft::auth, state};

#[derive(Serialize)]
pub struct DeviceAuthResponse {
    pub user_code: String,
    pub verification_uri: String,
    pub expires_in: i64,
}

#[derive(Serialize)]
pub struct AuthStatusResponse {
    pub authenticated: bool,
    pub username: Option<String>,
    pub uuid: Option<String>,
}

#[derive(Serialize)]
pub struct PollResponse {
    pub status: String,
    pub username: Option<String>,
    pub error: Option<String>,
}

/// POST /api/auth/device  — start Microsoft device code flow
pub async fn start_device_auth(
    State(state): State<state::SharedState>,
    headers: HeaderMap,
) -> Result<Json<DeviceAuthResponse>, (StatusCode, String)> {
    let s = state.read().await;
    state::extract_yuyu(&s, &headers)
        .ok_or((StatusCode::UNAUTHORIZED, "Non authentifié sur YuyuFrame".into()))?;
    drop(s);

    match auth::start_device_auth().await {
        Ok(resp) => {
            let expires_at = chrono::Utc::now().timestamp() + resp.expires_in;
            state.write().await.auth_device_code = Some(state::AuthDeviceCode {
                device_code: resp.device_code,
                user_code: resp.user_code.clone(),
                verification_uri: resp.verification_uri.clone(),
                expires_at,
            });
            Ok(Json(DeviceAuthResponse {
                user_code: resp.user_code,
                verification_uri: resp.verification_uri,
                expires_in: resp.expires_in,
            }))
        }
        Err(e) => Err((StatusCode::INTERNAL_SERVER_ERROR, e.to_string())),
    }
}

/// GET /api/auth/poll  — poll Microsoft for device code approval
pub async fn poll_auth(
    State(state): State<state::SharedState>,
    headers: HeaderMap,
) -> Json<PollResponse> {
    let s = state.read().await;
    let yuyu = match state::extract_yuyu(&s, &headers) {
        Some(y) => y,
        None => {
            return Json(PollResponse {
                status: "error".into(),
                username: None,
                error: Some("Non authentifié".into()),
            })
        }
    };
    let device_code = s.auth_device_code.clone();
    drop(s);

    let Some(dc) = device_code else {
        return Json(PollResponse {
            status: "error".into(),
            username: None,
            error: Some("Pas d'authentification en cours".into()),
        });
    };

    if chrono::Utc::now().timestamp() > dc.expires_at {
        state.write().await.auth_device_code = None;
        return Json(PollResponse {
            status: "error".into(),
            username: None,
            error: Some("Code expiré".into()),
        });
    }

    match auth::poll_device_auth(&dc.device_code).await {
        Ok(Some(session)) => {
            let username = session.username.clone();
            let uuid = session.uuid.clone();
            let ms_refresh = session.refresh_token.clone().unwrap_or_default();
            let expires_at = session.expires_at;

            // Persist to DB
            {
                let s = state.read().await;
                let conn = s.db.lock().await;
                db::upsert_mc_session(
                    &conn,
                    yuyu.user_id,
                    &username,
                    &uuid,
                    &session.access_token,
                    &ms_refresh,
                    expires_at,
                )
                .ok();
                db::set_active_mc(&conn, yuyu.user_id, &uuid).ok();
            }

            let mut w = state.write().await;
            w.session = Some(session);
            w.auth_device_code = None;

            Json(PollResponse {
                status: "success".into(),
                username: Some(username),
                error: None,
            })
        }
        Ok(None) => Json(PollResponse {
            status: "pending".into(),
            username: None,
            error: None,
        }),
        Err(e) => Json(PollResponse {
            status: "error".into(),
            username: None,
            error: Some(e.to_string()),
        }),
    }
}

/// GET /api/auth/status
pub async fn auth_status(
    State(state): State<state::SharedState>,
    headers: HeaderMap,
) -> Json<AuthStatusResponse> {
    let s = state.read().await;

    // Yuyu auth required
    let yuyu = match state::extract_yuyu(&s, &headers) {
        Some(y) => y,
        None => {
            return Json(AuthStatusResponse {
                authenticated: false,
                username: None,
                uuid: None,
            })
        }
    };

    let session = s.session.clone();
    drop(s);

    match session {
        None => Json(AuthStatusResponse {
            authenticated: false,
            username: None,
            uuid: None,
        }),
        Some(sess) => {
            // Auto-refresh if expiring within 30 minutes
            let now = chrono::Utc::now().timestamp();
            if sess.expires_at - now < 1800 {
                if let Some(ms_ref) = &sess.refresh_token {
                    tracing::info!("Auto-rafraîchissement du token MC pour {}", sess.username);
                    if let Ok((mc_at, mc_user, mc_uuid, new_ref, new_exp)) =
                        auth::refresh_session(ms_ref).await
                    {
                        let s = state.read().await;
                        let conn = s.db.lock().await;
                        db::update_mc_tokens(
                            &conn, yuyu.user_id, &mc_uuid, &mc_at, &new_ref, new_exp,
                        )
                        .ok();
                        drop(conn);
                        drop(s);

                        let new_sess = state::MinecraftSession {
                            username: mc_user.clone(),
                            uuid: mc_uuid.clone(),
                            access_token: mc_at,
                            refresh_token: Some(new_ref),
                            expires_at: new_exp,
                        };
                        state.write().await.session = Some(new_sess);
                        return Json(AuthStatusResponse {
                            authenticated: true,
                            username: Some(mc_user),
                            uuid: Some(mc_uuid),
                        });
                    }
                }
            }

            Json(AuthStatusResponse {
                authenticated: true,
                username: Some(sess.username),
                uuid: Some(sess.uuid),
            })
        }
    }
}

/// POST /api/auth/logout  — clears in-memory MC session (keeps in DB)
pub async fn logout(
    State(state): State<state::SharedState>,
    headers: HeaderMap,
) -> StatusCode {
    let s = state.read().await;
    if state::extract_yuyu(&s, &headers).is_none() {
        return StatusCode::UNAUTHORIZED;
    }
    drop(s);
    state.write().await.session = None;
    StatusCode::OK
}
