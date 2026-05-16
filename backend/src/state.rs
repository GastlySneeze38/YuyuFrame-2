use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::RwLock;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MinecraftSession {
    pub username: String,
    pub uuid: String,
    pub access_token: String,
    pub refresh_token: Option<String>,
    pub expires_at: i64,
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

#[derive(Debug, Default)]
pub struct AppState {
    pub session: Option<MinecraftSession>,
    pub download_progress: Option<DownloadProgress>,
    pub game_running: bool,
    pub auth_device_code: Option<AuthDeviceCode>,
}

pub type SharedState = Arc<RwLock<AppState>>;
