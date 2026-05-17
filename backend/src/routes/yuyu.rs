use axum::{extract::State, http::StatusCode, Json};
use serde::{Deserialize, Serialize};

use crate::{
    db,
    state::{SharedState, YuyuSession},
};

// ── Request / response types ──────────────────────────────────────────────────

#[derive(Deserialize)]
pub struct AuthBody {
    pub username: String,
    pub password: String,
}

#[derive(Serialize)]
pub struct StatusResp {
    pub has_account: bool,
}

#[derive(Serialize)]
pub struct LoginResp {
    pub token: String,
    pub username: String,
    pub accounts: Vec<AccountInfo>,
}

#[derive(Serialize)]
pub struct AccountInfo {
    pub mc_username: String,
    pub mc_uuid: String,
    pub is_active: bool,
}

// ── Handlers ──────────────────────────────────────────────────────────────────

/// GET /api/yuyu/status
pub async fn status(State(state): State<SharedState>) -> Json<StatusResp> {
    let s = state.read().await;
    let conn = s.db.lock().await;
    let has_account = db::account_exists(&conn).unwrap_or(false);
    Json(StatusResp { has_account })
}

/// POST /api/yuyu/register
pub async fn register(
    State(state): State<SharedState>,
    Json(body): Json<AuthBody>,
) -> Result<Json<LoginResp>, (StatusCode, String)> {
    use argon2::{
        password_hash::{rand_core::OsRng, PasswordHasher, SaltString},
        Argon2,
    };

    if body.username.trim().is_empty() || body.password.len() < 4 {
        return Err((
            StatusCode::BAD_REQUEST,
            "Nom d'utilisateur ou mot de passe invalide".into(),
        ));
    }

    let s = state.read().await;
    let conn = s.db.lock().await;

    if db::account_exists(&conn).map_err(internal)? {
        return Err((
            StatusCode::CONFLICT,
            "Un compte YuyuFrame existe déjà".into(),
        ));
    }

    let salt = SaltString::generate(&mut OsRng);
    let hash = Argon2::default()
        .hash_password(body.password.as_bytes(), &salt)
        .map_err(|e| internal(anyhow::anyhow!("{}", e)))?
        .to_string();

    let user_id = db::create_user(&conn, &body.username, &hash).map_err(internal)?;

    let token = gen_token();
    drop(conn);

    let mut w = state.write().await;
    w.yuyu_session = Some(YuyuSession {
        user_id,
        username: body.username.clone(),
        token: token.clone(),
    });

    Ok(Json(LoginResp {
        token,
        username: body.username,
        accounts: vec![],
    }))
}

/// POST /api/yuyu/login
pub async fn login(
    State(state): State<SharedState>,
    Json(body): Json<AuthBody>,
) -> Result<Json<LoginResp>, (StatusCode, String)> {
    use argon2::{Argon2, PasswordHash, PasswordVerifier};
    use crate::minecraft::auth as mc_auth;

    let s = state.read().await;
    let conn = s.db.lock().await;

    let user = db::get_user(&conn, &body.username)
        .map_err(internal)?
        .ok_or((StatusCode::UNAUTHORIZED, "Compte introuvable".into()))?;

    let hash = PasswordHash::new(&user.password_hash)
        .map_err(|e| internal(anyhow::anyhow!("{}", e)))?;
    Argon2::default()
        .verify_password(body.password.as_bytes(), &hash)
        .map_err(|_| (StatusCode::UNAUTHORIZED, "Mot de passe incorrect".into()))?;

    // Load MC sessions from DB
    let rows = db::list_mc_sessions(&conn, user.id).map_err(internal)?;
    let active_uuid = db::get_active_mc_uuid(&conn, user.id).map_err(internal)?;

    let token = gen_token();

    // Build account list and resolve the active session (refresh if needed)
    let now = chrono::Utc::now().timestamp();
    let mut accounts: Vec<AccountInfo> = Vec::new();
    let mut active_session = None;

    for row in &rows {
        let is_active = active_uuid.as_deref() == Some(&row.mc_uuid);
        accounts.push(AccountInfo {
            mc_username: row.mc_username.clone(),
            mc_uuid: row.mc_uuid.clone(),
            is_active,
        });

        if is_active {
            // If token expires within 30 min, refresh proactively
            if row.expires_at - now < 1800 {
                tracing::info!("Rafraîchissement du token pour {}", row.mc_username);
                match mc_auth::refresh_session(&row.ms_refresh_token).await {
                    Ok((mc_at, mc_user, mc_uuid, new_refresh, new_exp)) => {
                        db::update_mc_tokens(
                            &conn, user.id, &mc_uuid, &mc_at, &new_refresh, new_exp,
                        )
                        .ok();
                        active_session = Some(crate::state::MinecraftSession {
                            username: mc_user,
                            uuid: mc_uuid,
                            access_token: mc_at,
                            refresh_token: Some(new_refresh),
                            expires_at: new_exp,
                        });
                    }
                    Err(e) => {
                        tracing::warn!("Échec du rafraîchissement: {}", e);
                        // Use stale session anyway — launch will fail later with a clear error
                        active_session = Some(crate::state::MinecraftSession {
                            username: row.mc_username.clone(),
                            uuid: row.mc_uuid.clone(),
                            access_token: row.access_token.clone(),
                            refresh_token: Some(row.ms_refresh_token.clone()),
                            expires_at: row.expires_at,
                        });
                    }
                }
            } else {
                active_session = Some(crate::state::MinecraftSession {
                    username: row.mc_username.clone(),
                    uuid: row.mc_uuid.clone(),
                    access_token: row.access_token.clone(),
                    refresh_token: Some(row.ms_refresh_token.clone()),
                    expires_at: row.expires_at,
                });
            }
        }
    }

    drop(conn);

    let mut w = state.write().await;
    w.yuyu_session = Some(YuyuSession {
        user_id: user.id,
        username: user.username.clone(),
        token: token.clone(),
    });
    w.session = active_session;

    Ok(Json(LoginResp {
        token,
        username: user.username,
        accounts,
    }))
}

/// POST /api/yuyu/logout
pub async fn logout(State(state): State<SharedState>) -> StatusCode {
    let mut w = state.write().await;
    w.yuyu_session = None;
    w.session = None;
    StatusCode::OK
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fn gen_token() -> String {
    use rand::Rng;
    rand::thread_rng()
        .sample_iter(rand::distributions::Alphanumeric)
        .take(48)
        .map(char::from)
        .collect()
}

fn internal<E: std::fmt::Display>(e: E) -> (StatusCode, String) {
    tracing::error!("{}", e);
    (StatusCode::INTERNAL_SERVER_ERROR, e.to_string())
}
