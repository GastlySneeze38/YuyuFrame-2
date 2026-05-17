use axum::{extract::State, http::StatusCode, Json};
use serde::{Deserialize, Serialize};

use crate::minecraft::launcher;
use crate::state::SharedState;

#[derive(Deserialize)]
pub struct LaunchRequest {
    pub version: String,
    pub ram: Option<u32>,
    /// "vanilla" | "fabric" | "forge" — defaults to "vanilla"
    pub loader: Option<String>,
}

#[derive(Serialize)]
pub struct LaunchResponse {
    pub success: bool,
    pub message: String,
}

#[derive(Serialize)]
pub struct ProgressResponse {
    pub downloading: bool,
    pub current: u64,
    pub total: u64,
    pub message: String,
    pub percent: f64,
}

pub async fn launch_game(
    State(state): State<SharedState>,
    Json(req): Json<LaunchRequest>,
) -> Result<Json<LaunchResponse>, (StatusCode, String)> {
    let session = {
        let s = state.read().await;
        s.session
            .clone()
            .ok_or_else(|| (StatusCode::UNAUTHORIZED, "Non connecté".to_string()))?
    };

    if state.read().await.game_running {
        return Ok(Json(LaunchResponse {
            success: false,
            message: "Le jeu est déjà en cours".into(),
        }));
    }

    let state_clone = state.clone();
    let version = req.version.clone();
    let ram = req.ram.unwrap_or(4096);
    let loader = req.loader.clone().unwrap_or_else(|| "vanilla".to_string());

    tokio::spawn(async move {
        state_clone.write().await.game_running = true;
        if let Err(e) = launcher::download_and_launch(
            &version,
            Some(&loader),
            &session,
            ram,
            state_clone.clone(),
        )
        .await
        {
            tracing::error!("Erreur de lancement: {}", e);
        }
        let mut s = state_clone.write().await;
        s.game_running = false;
        s.download_progress = None;
    });

    Ok(Json(LaunchResponse {
        success: true,
        message: "Lancement en cours...".into(),
    }))
}

pub async fn download_progress(State(state): State<SharedState>) -> Json<ProgressResponse> {
    let s = state.read().await;
    match &s.download_progress {
        Some(p) => {
            let percent = if p.total > 0 {
                p.current as f64 / p.total as f64 * 100.0
            } else {
                0.0
            };
            Json(ProgressResponse {
                downloading: true,
                current: p.current,
                total: p.total,
                message: p.message.clone(),
                percent,
            })
        }
        None => Json(ProgressResponse {
            downloading: s.game_running,
            current: 0,
            total: 0,
            message: if s.game_running {
                "Jeu en cours".into()
            } else {
                "Inactif".into()
            },
            percent: 0.0,
        }),
    }
}
