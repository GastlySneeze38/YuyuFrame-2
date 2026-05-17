use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    Json,
};
use serde::{Deserialize, Serialize};

use crate::{db, minecraft::auth as mc_auth, state};

#[derive(Serialize)]
pub struct AccountInfo {
    pub mc_username: String,
    pub mc_uuid: String,
    pub is_active: bool,
}

#[derive(Deserialize)]
pub struct SwitchBody {
    pub uuid: String,
}

/// GET /api/mc/accounts
pub async fn list_accounts(
    State(state): State<state::SharedState>,
    headers: HeaderMap,
) -> Result<Json<Vec<AccountInfo>>, StatusCode> {
    let s = state.read().await;
    let yuyu = state::extract_yuyu(&s, &headers).ok_or(StatusCode::UNAUTHORIZED)?;
    let conn = s.db.lock().await;

    let rows = db::list_mc_sessions(&conn, yuyu.user_id)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let active_uuid = db::get_active_mc_uuid(&conn, yuyu.user_id)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let accounts = rows
        .into_iter()
        .map(|r| AccountInfo {
            is_active: active_uuid.as_deref() == Some(&r.mc_uuid),
            mc_username: r.mc_username,
            mc_uuid: r.mc_uuid,
        })
        .collect();

    Ok(Json(accounts))
}

/// POST /api/mc/switch  — body: { uuid }
pub async fn switch_account(
    State(state): State<state::SharedState>,
    headers: HeaderMap,
    Json(body): Json<SwitchBody>,
) -> Result<Json<AccountInfo>, (StatusCode, String)> {
    // ── Phase 1 : lecture DB (read lock court) ────────────────────────────────
    let (yuyu_user_id, row) = {
        let s = state.read().await;
        let yuyu = state::extract_yuyu(&s, &headers)
            .ok_or((StatusCode::UNAUTHORIZED, "Non authentifié".into()))?;
        let conn = s.db.lock().await;
        let row = db::get_mc_session(&conn, yuyu.user_id, &body.uuid)
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
            .ok_or((StatusCode::NOT_FOUND, "Compte introuvable".into()))?;
        (yuyu.user_id, row)
        // conn and s dropped here
    };

    // ── Phase 2 : refresh si nécessaire (sans lock) ───────────────────────────
    let now = chrono::Utc::now().timestamp();
    let mc_session = if row.expires_at - now < 1800 {
        tracing::info!("Rafraîchissement du token pour {} lors du switch", row.mc_username);
        match mc_auth::refresh_session(&row.ms_refresh_token).await {
            Ok((mc_at, mc_user, mc_uuid, new_refresh, new_exp)) => {
                let s = state.read().await;
                let conn = s.db.lock().await;
                db::update_mc_tokens(&conn, yuyu_user_id, &mc_uuid, &mc_at, &new_refresh, new_exp).ok();
                drop(conn);
                drop(s);
                state::MinecraftSession {
                    username: mc_user,
                    uuid: mc_uuid,
                    access_token: mc_at,
                    refresh_token: Some(new_refresh),
                    expires_at: new_exp,
                }
            }
            Err(e) => {
                tracing::warn!("Échec du rafraîchissement: {}", e);
                state::MinecraftSession {
                    username: row.mc_username.clone(),
                    uuid: row.mc_uuid.clone(),
                    access_token: row.access_token.clone(),
                    refresh_token: Some(row.ms_refresh_token.clone()),
                    expires_at: row.expires_at,
                }
            }
        }
    } else {
        state::MinecraftSession {
            username: row.mc_username.clone(),
            uuid: row.mc_uuid.clone(),
            access_token: row.access_token.clone(),
            refresh_token: Some(row.ms_refresh_token.clone()),
            expires_at: row.expires_at,
        }
    };

    // Marquer actif en DB
    {
        let s = state.read().await;
        let conn = s.db.lock().await;
        db::set_active_mc(&conn, yuyu_user_id, &body.uuid)
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
        // conn and s dropped here
    }

    let info = AccountInfo {
        mc_username: mc_session.username.clone(),
        mc_uuid: mc_session.uuid.clone(),
        is_active: true,
    };

    // ── Phase 3 : écriture de l'état (write lock) ─────────────────────────────
    state.write().await.session = Some(mc_session);

    Ok(Json(info))
}

/// DELETE /api/mc/account/:uuid
pub async fn delete_account(
    State(state): State<state::SharedState>,
    headers: HeaderMap,
    Path(uuid): Path<String>,
) -> Result<StatusCode, StatusCode> {
    // ── Phase 1 : lecture + suppression DB ───────────────────────────────────
    {
        let s = state.read().await;
        let yuyu = state::extract_yuyu(&s, &headers).ok_or(StatusCode::UNAUTHORIZED)?;
        let conn = s.db.lock().await;
        db::delete_mc_session(&conn, yuyu.user_id, &uuid)
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
        db::clear_active_mc(&conn, yuyu.user_id, &uuid).ok();
        // conn and s dropped here
    }

    // ── Phase 2 : mise à jour état mémoire ───────────────────────────────────
    let mut w = state.write().await;
    if w.session.as_ref().map(|s| s.uuid.as_str()) == Some(uuid.as_str()) {
        w.session = None;
    }

    Ok(StatusCode::OK)
}
