use axum::{
    routing::{get, post},
    Router,
};
use std::sync::Arc;
use tokio::sync::RwLock;
use tower_http::cors::{Any, CorsLayer};

mod minecraft;
mod routes;
mod state;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt::init();

    let app_state = Arc::new(RwLock::new(state::AppState::default()));

    // Restore session from disk if available
    if let Ok(session) = minecraft::auth::load_session().await {
        tracing::info!("Session restaurée pour {}", session.username);
        app_state.write().await.session = Some(session);
    }

    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    let app = Router::new()
        .route("/api/health", get(routes::health))
        .route("/api/versions", get(routes::versions::list_versions))
        .route("/api/auth/device", post(routes::auth::start_device_auth))
        .route("/api/auth/poll", get(routes::auth::poll_auth))
        .route("/api/auth/status", get(routes::auth::auth_status))
        .route("/api/auth/logout", post(routes::auth::logout))
        .route("/api/launch", post(routes::launch::launch_game))
        .route("/api/launch/progress", get(routes::launch::download_progress))
        .layer(cors)
        .with_state(app_state);

    let listener = tokio::net::TcpListener::bind("127.0.0.1:3847").await?;
    tracing::info!("YuyuFrame backend sur http://127.0.0.1:3847");
    axum::serve(listener, app).await?;
    Ok(())
}
