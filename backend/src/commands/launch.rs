use tauri::Emitter;

use crate::minecraft::launcher;
use crate::state::SharedState;

#[tauri::command]
pub async fn launch_game(
    state: tauri::State<'_, SharedState>,
    app: tauri::AppHandle,
    version: String,
    ram: Option<u32>,
    loader: Option<String>,
) -> Result<(), String> {
    let session = {
        let s = state.read().await;
        s.session.clone().ok_or("Non connecté à Minecraft")?
    };

    if state.read().await.game_running {
        return Err("Le jeu est déjà en cours".into());
    }

    let state_clone = state.inner().clone();
    let ram = ram.unwrap_or(4096);
    let loader = loader.unwrap_or_else(|| "vanilla".to_string());

    tokio::spawn(async move {
        state_clone.write().await.game_running = true;
        let _ = app.emit("game_state", serde_json::json!({ "running": true }));

        if let Err(e) = launcher::download_and_launch(
            &version,
            Some(&loader),
            &session,
            ram,
            app.clone(),
            state_clone.clone(),
        )
        .await
        {
            tracing::error!("Erreur de lancement: {}", e);
            let _ = app.emit("launch_error", e.to_string());
        }

        state_clone.write().await.game_running = false;
        let _ = app.emit("game_state", serde_json::json!({ "running": false }));
    });

    Ok(())
}
