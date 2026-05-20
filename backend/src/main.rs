use axum::{
    routing::{delete, get, post, put},
    Router,
};
use std::sync::Arc;
use tokio::sync::{Mutex, RwLock};
use tower_http::cors::{Any, CorsLayer};

mod db;
mod minecraft;
mod routes;
mod state;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt::init();

    // Initialise SQLite database
    let conn = db::init_db().expect("Impossible d'initialiser la base de données");
    tracing::info!("Base de données SQLite initialisée");

    // Restore yuyu session from DB if it exists
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

    let app_state = Arc::new(RwLock::new(state::AppState {
        db: Arc::new(Mutex::new(conn)),
        yuyu_session,
        session: None,
        download_progress: None,
        game_running: false,
        auth_device_code: None,
    }));

    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    let app = Router::new()
        // Public
        .route("/api/health", get(routes::health))
        .route("/api/versions", get(routes::versions::list_versions))
        // YuyuFrame account (no auth required)
        .route("/api/yuyu/status", get(routes::yuyu::status))
        .route("/api/yuyu/register", post(routes::yuyu::register))
        .route("/api/yuyu/login", post(routes::yuyu::login))
        .route("/api/yuyu/logout", post(routes::yuyu::logout))
        // Microsoft / Minecraft auth (yuyu token required)
        .route("/api/auth/device", post(routes::auth::start_device_auth))
        .route("/api/auth/poll", get(routes::auth::poll_auth))
        .route("/api/auth/status", get(routes::auth::auth_status))
        .route("/api/auth/logout", post(routes::auth::logout))
        // Minecraft account management (yuyu token required)
        .route("/api/mc/accounts", get(routes::mc::list_accounts))
        .route("/api/mc/switch", post(routes::mc::switch_account))
        .route("/api/mc/account/:uuid", delete(routes::mc::delete_account))
        // Game
        .route("/api/launch", post(routes::launch::launch_game))
        .route("/api/launch/progress", get(routes::launch::download_progress))
        // Mods
        .route("/api/mods", get(routes::mods::list_mods))
        .route("/api/mods/upload", post(routes::mods::upload_mod))
        .route("/api/mods/install", post(routes::mods::install_from_url))
        .route("/api/mods/:name/toggle", put(routes::mods::toggle_mod))
        .route("/api/mods/:name", delete(routes::mods::delete_mod))
        .layer(cors)
        .with_state(app_state);

    let listener = tokio::net::TcpListener::bind("127.0.0.1:3847").await?;
    tracing::info!("YuyuFrame backend sur http://127.0.0.1:3847");
    axum::serve(listener, app).await?;
    Ok(())
}
