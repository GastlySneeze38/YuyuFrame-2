use axum::{extract::State, http::StatusCode, Json};
use serde::Serialize;

use crate::minecraft::auth;
use crate::state::{AuthDeviceCode, SharedState};

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

pub async fn start_device_auth(
    State(state): State<SharedState>,
) -> Result<Json<DeviceAuthResponse>, (StatusCode, String)> {
    match auth::start_device_auth().await {
        Ok(resp) => {
            let expires_at = chrono::Utc::now().timestamp() + resp.expires_in;
            state.write().await.auth_device_code = Some(AuthDeviceCode {
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

pub async fn poll_auth(State(state): State<SharedState>) -> Json<PollResponse> {
    let device_code = state.read().await.auth_device_code.clone();

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
            auth::save_session(&session).await.ok();
            let mut s = state.write().await;
            s.session = Some(session);
            s.auth_device_code = None;
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

pub async fn auth_status(State(state): State<SharedState>) -> Json<AuthStatusResponse> {
    let s = state.read().await;
    match &s.session {
        Some(sess) => Json(AuthStatusResponse {
            authenticated: true,
            username: Some(sess.username.clone()),
            uuid: Some(sess.uuid.clone()),
        }),
        None => Json(AuthStatusResponse {
            authenticated: false,
            username: None,
            uuid: None,
        }),
    }
}

pub async fn logout(State(state): State<SharedState>) -> StatusCode {
    state.write().await.session = None;
    auth::delete_session().await.ok();
    StatusCode::OK
}
