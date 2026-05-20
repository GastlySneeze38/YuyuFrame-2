use serde::Serialize;

use crate::{db, state::SharedState};

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

// ── Commands ──────────────────────────────────────────────────────────────────

#[tauri::command]
pub async fn yuyu_status(state: tauri::State<'_, SharedState>) -> Result<StatusResp, String> {
    let s = state.read().await;
    let conn = s.db.lock().await;
    let has_account = db::account_exists(&conn).unwrap_or(false);
    Ok(StatusResp { has_account })
}

#[tauri::command]
pub async fn yuyu_register(
    state: tauri::State<'_, SharedState>,
    username: String,
    password: String,
) -> Result<LoginResp, String> {
    use argon2::{
        password_hash::{rand_core::OsRng, PasswordHasher, SaltString},
        Argon2,
    };

    if username.trim().is_empty() || password.len() < 4 {
        return Err("Nom d'utilisateur ou mot de passe invalide".into());
    }

    let (user_id, token) = {
        let s = state.read().await;
        let conn = s.db.lock().await;

        if db::account_exists(&conn).map_err(|e| e.to_string())? {
            return Err("Un compte YuyuFrame existe déjà".into());
        }

        let salt = SaltString::generate(&mut OsRng);
        let hash = Argon2::default()
            .hash_password(password.as_bytes(), &salt)
            .map_err(|e| e.to_string())?
            .to_string();

        let user_id = db::create_user(&conn, &username, &hash).map_err(|e| e.to_string())?;
        let token = gen_token();
        db::save_yuyu_token(&conn, user_id, &token).map_err(|e| e.to_string())?;
        (user_id, token)
    };

    state.write().await.yuyu_session = Some(crate::state::YuyuSession {
        user_id,
        username: username.clone(),
        token: token.clone(),
    });

    Ok(LoginResp { token, username, accounts: vec![] })
}

#[tauri::command]
pub async fn yuyu_login(
    state: tauri::State<'_, SharedState>,
    username: String,
    password: String,
) -> Result<LoginResp, String> {
    use argon2::{Argon2, PasswordHash, PasswordVerifier};
    use crate::minecraft::auth as mc_auth;
    use crate::state::MinecraftSession;

    let (user_id, db_username, rows, active_uuid) = {
        let s = state.read().await;
        let conn = s.db.lock().await;

        let user = db::get_user(&conn, &username)
            .map_err(|e| e.to_string())?
            .ok_or("Compte introuvable")?;

        let hash = PasswordHash::new(&user.password_hash).map_err(|e| e.to_string())?;
        Argon2::default()
            .verify_password(password.as_bytes(), &hash)
            .map_err(|_| "Mot de passe incorrect")?;

        let rows = db::list_mc_sessions(&conn, user.id).map_err(|e| e.to_string())?;
        let active_uuid = db::get_active_mc_uuid(&conn, user.id).map_err(|e| e.to_string())?;
        (user.id, user.username, rows, active_uuid)
    };

    let now = chrono::Utc::now().timestamp();
    let mut accounts: Vec<AccountInfo> = Vec::new();
    let mut active_session: Option<MinecraftSession> = None;

    for row in &rows {
        let is_active = active_uuid.as_deref() == Some(&row.mc_uuid);
        accounts.push(AccountInfo {
            mc_username: row.mc_username.clone(),
            mc_uuid: row.mc_uuid.clone(),
            is_active,
        });

        if is_active {
            if row.expires_at - now < 1800 {
                tracing::info!("Rafraîchissement du token pour {}", row.mc_username);
                match mc_auth::refresh_session(&row.ms_refresh_token).await {
                    Ok((mc_at, mc_user, mc_uuid, new_refresh, new_exp)) => {
                        let s = state.read().await;
                        let conn = s.db.lock().await;
                        db::update_mc_tokens(&conn, user_id, &mc_uuid, &mc_at, &new_refresh, new_exp).ok();
                        drop(conn);
                        drop(s);
                        active_session = Some(MinecraftSession {
                            username: mc_user,
                            uuid: mc_uuid,
                            access_token: mc_at,
                            refresh_token: Some(new_refresh),
                            expires_at: new_exp,
                        });
                    }
                    Err(e) => {
                        tracing::warn!("Échec du rafraîchissement: {}", e);
                        active_session = Some(MinecraftSession {
                            username: row.mc_username.clone(),
                            uuid: row.mc_uuid.clone(),
                            access_token: row.access_token.clone(),
                            refresh_token: Some(row.ms_refresh_token.clone()),
                            expires_at: row.expires_at,
                        });
                    }
                }
            } else {
                active_session = Some(MinecraftSession {
                    username: row.mc_username.clone(),
                    uuid: row.mc_uuid.clone(),
                    access_token: row.access_token.clone(),
                    refresh_token: Some(row.ms_refresh_token.clone()),
                    expires_at: row.expires_at,
                });
            }
        }
    }

    let token = gen_token();
    {
        let s = state.read().await;
        let conn = s.db.lock().await;
        db::save_yuyu_token(&conn, user_id, &token).map_err(|e| e.to_string())?;
    }
    {
        let mut w = state.write().await;
        w.yuyu_session = Some(crate::state::YuyuSession {
            user_id,
            username: db_username.clone(),
            token: token.clone(),
        });
        w.session = active_session;
    }

    Ok(LoginResp { token, username: db_username, accounts })
}

#[tauri::command]
pub async fn yuyu_logout(state: tauri::State<'_, SharedState>) -> Result<(), String> {
    let user_id = {
        let w = state.read().await;
        w.yuyu_session.as_ref().map(|s| s.user_id)
    };
    if let Some(uid) = user_id {
        let s = state.read().await;
        let conn = s.db.lock().await;
        db::delete_yuyu_token(&conn, uid).ok();
    }
    let mut w = state.write().await;
    w.yuyu_session = None;
    w.session = None;
    Ok(())
}

// ── Helper ────────────────────────────────────────────────────────────────────

fn gen_token() -> String {
    use rand::Rng;
    rand::thread_rng()
        .sample_iter(rand::distributions::Alphanumeric)
        .take(48)
        .map(char::from)
        .collect()
}
