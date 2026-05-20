mod commands;
mod db;
mod minecraft;
mod state;

use std::sync::Arc;
use tauri::Manager;
use tokio::sync::{Mutex, RwLock};

pub fn run() {
    tracing_subscriber::fmt::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .setup(|app| {
            let db_path = if cfg!(dev) {
                // Dev : garde la DB dans Backend/ à côté du code source
                std::path::Path::new(env!("CARGO_MANIFEST_DIR")).join("yuyu.db")
            } else {
                // Prod : à côté de l'exécutable
                std::env::current_exe()
                    .ok()
                    .and_then(|p| p.parent().map(|d| d.join("yuyu.db")))
                    .unwrap_or_else(|| std::path::PathBuf::from("yuyu.db"))
            };

            let conn = db::init_db(&db_path).expect("Impossible d'initialiser la base de données");
            tracing::info!("Base de données : {}", db_path.display());

            let yuyu_session = db::load_yuyu_token(&conn)
                .ok()
                .flatten()
                .map(|row| {
                    tracing::info!("Session YuyuFrame restaurée pour {}", row.username);
                    state::YuyuSession {
                        user_id: row.user_id,
                        username: row.username,
                        token: row.token,
                    }
                });

            let app_state: state::SharedState = Arc::new(RwLock::new(state::AppState {
                db: Arc::new(Mutex::new(conn)),
                yuyu_session,
                session: None,
                download_progress: None,
                game_running: false,
                auth_device_code: None,
            }));

            app.manage(app_state);
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            commands::yuyu::yuyu_status,
            commands::yuyu::yuyu_register,
            commands::yuyu::yuyu_login,
            commands::yuyu::yuyu_logout,
            commands::auth::auth_start_device,
            commands::auth::auth_poll,
            commands::auth::auth_status,
            commands::auth::auth_logout,
            commands::mc::mc_list_accounts,
            commands::mc::mc_switch,
            commands::mc::mc_delete,
            commands::versions::list_versions,
            commands::launch::launch_game,
            commands::mods::mods_list,
            commands::mods::mods_toggle,
            commands::mods::mods_delete,
            commands::mods::mods_install,
            commands::mods::mods_upload,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
