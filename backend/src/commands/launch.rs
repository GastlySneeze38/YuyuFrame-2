use tauri::Emitter;

use crate::commands::instances::{instance_dir, load_instances_pub};
use crate::minecraft::launcher;
use crate::state::SharedState;

#[tauri::command]
pub async fn launch_game(
    state: tauri::State<'_, SharedState>,
    app: tauri::AppHandle,
    instance_id: String,
) -> Result<(), String> {
    let session = {
        let s = state.read().await;
        s.session.clone().ok_or("Non connecté à Minecraft")?
    };

    if state.read().await.game_running {
        return Err("Le jeu est déjà en cours".into());
    }

    let instances = load_instances_pub();
    let instance = instances
        .into_iter()
        .find(|i| i.id == instance_id)
        .ok_or("Instance introuvable")?;

    let game_dir = instance_dir(&instance_id);
    tokio::fs::create_dir_all(&game_dir).await.map_err(|e| e.to_string())?;

    let state_clone = state.inner().clone();

    tokio::spawn(async move {
        state_clone.write().await.game_running = true;
        let _ = app.emit("game_state", serde_json::json!({ "running": true }));

        if let Err(e) = launcher::download_and_launch(
            &instance.mc_version,
            Some(&instance.loader),
            &session,
            instance.ram_mb,
            &game_dir,
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
