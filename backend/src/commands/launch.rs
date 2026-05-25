use tauri::{Emitter, Manager};

use crate::commands::instances::instance_dir;
use crate::db;
use crate::minecraft::launcher;
use crate::state::SharedState;

#[tauri::command]
pub async fn launch_game(
    state: tauri::State<'_, SharedState>,
    app: tauri::AppHandle,
    instance_id: String,
    p2p: Option<bool>,
) -> Result<(), String> {
    let session = {
        let s = state.read().await;
        s.session.clone().ok_or("Non connecté à Minecraft")?
    };

    if state.read().await.game_running {
        return Err("Le jeu est déjà en cours".into());
    }

    let yuyu_user_id = {
        let s = state.read().await;
        s.yuyu_session.as_ref().map(|y| y.user_id).unwrap_or(0)
    };

    let instance = {
        let s = state.read().await;
        let db = s.db.lock().await;
        db::instance_get(&db, &instance_id, yuyu_user_id)
            .map_err(|e| e.to_string())?
            .ok_or("Instance introuvable")?
    };
    let instance = crate::commands::instances::Instance {
        id: instance.id,
        name: instance.name,
        mc_version: instance.mc_version,
        loader: instance.loader,
        ram_mb: instance.ram_mb,
        favorite: instance.favorite,
    };

    let game_dir = instance_dir(&instance_id);
    tokio::fs::create_dir_all(&game_dir).await.map_err(|e| e.to_string())?;

    // Open or reopen the console window
    if let Some(existing) = app.get_webview_window("minecraft-console") {
        let _ = existing.close();
    }
    let _ = tauri::WebviewWindowBuilder::new(
        &app,
        "minecraft-console",
        tauri::WebviewUrl::App(std::path::PathBuf::from("#/console")),
    )
    .title("Console Minecraft")
    .inner_size(960.0, 620.0)
    .decorations(false)
    .build();

    let state_clone = state.inner().clone();

    tokio::spawn(async move {
        let started_at = chrono::Utc::now().timestamp();
        state_clone.write().await.game_running = true;
        let _ = app.emit("game_state", serde_json::json!({ "running": true }));

        let session_id: Option<i64> = {
            let s = state_clone.read().await;
            let db = s.db.lock().await;
            db::session_start(
                &db,
                yuyu_user_id,
                &instance.id,
                &instance.name,
                &instance.mc_version,
                &instance.loader,
            )
            .ok()
        };

        if let Err(e) = launcher::download_and_launch(
            &instance.mc_version,
            Some(&instance.loader),
            &session,
            instance.ram_mb,
            &game_dir,
            app.clone(),
            state_clone.clone(),
            p2p.unwrap_or(false),
        )
        .await
        {
            tracing::error!("Erreur de lancement: {}", e);
            let _ = app.emit("launch_error", e.to_string());
        }

        if let Some(sid) = session_id {
            let duration = chrono::Utc::now().timestamp() - started_at;
            let s = state_clone.read().await;
            let db = s.db.lock().await;
            let _ = db::session_end(&db, sid, duration);
        }

        state_clone.write().await.game_running = false;
        let _ = app.emit("game_state", serde_json::json!({ "running": false }));
    });

    Ok(())
}
