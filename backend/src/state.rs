use rusqlite::Connection;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::{Mutex, RwLock};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MinecraftSession {
    pub username: String,
    pub uuid: String,
    pub access_token: String,
    /// Microsoft OAuth refresh token (long-lived, stored in DB)
    pub refresh_token: Option<String>,
    pub expires_at: i64,
}

#[derive(Debug, Clone)]
pub struct YuyuSession {
    pub user_id: i64,
    pub username: String,
    /// Random 48-char token sent by client in X-Yuyu-Token header
    pub token: String,
}

#[derive(Debug, Clone, Serialize, Default)]
pub struct DownloadProgress {
    pub current: u64,
    pub total: u64,
    pub message: String,
}

#[derive(Debug, Clone)]
pub struct AuthDeviceCode {
    pub device_code: String,
    pub user_code: String,
    pub verification_uri: String,
    pub expires_at: i64,
}

pub struct AppState {
    pub db: Arc<Mutex<Connection>>,
    pub yuyu_session: Option<YuyuSession>,
    pub session: Option<MinecraftSession>,
    pub download_progress: Option<DownloadProgress>,
    pub game_running: bool,
    pub auth_device_code: Option<AuthDeviceCode>,
}

pub type SharedState = Arc<RwLock<AppState>>;

// ── Helper: extract & validate YuyuFrame token from headers ───────────────────

pub fn extract_yuyu(state: &AppState, headers: &axum::http::HeaderMap) -> Option<YuyuSession> {
    let token = headers.get("X-Yuyu-Token")?.to_str().ok()?;
    let sess = state.yuyu_session.as_ref()?;
    if sess.token == token {
        Some(sess.clone())
    } else {
        None
    }
}
