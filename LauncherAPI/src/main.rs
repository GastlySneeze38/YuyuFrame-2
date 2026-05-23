mod db;
mod jwt;
mod routes;
mod state;

use axum::{extract::DefaultBodyLimit, Router};
use rusqlite::Connection;
use state::AppState;
use std::sync::{Arc, Mutex};
use tower_http::cors::CorsLayer;
use tower_http::trace::TraceLayer;
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};

#[tokio::main]
async fn main() {
    dotenvy::dotenv().ok();
    tracing_subscriber::registry()
        .with(tracing_subscriber::EnvFilter::new(
            std::env::var("RUST_LOG")
                .unwrap_or_else(|_| "launcher_api=debug,tower_http=debug".into()),
        ))
        .with(tracing_subscriber::fmt::layer())
        .init();

    let db_path = std::env::var("DB_PATH").unwrap_or_else(|_| "launcher.db".into());
    let conn = Connection::open(&db_path).expect("Impossible d'ouvrir la base de données");
    db::init(&conn).expect("Impossible d'initialiser le schéma");
    tracing::info!("Base de données : {}", db_path);

    let jwt_secret = std::env::var("JWT_SECRET")
        .unwrap_or_else(|_| "changeme-set-JWT_SECRET-in-prod".into());
    let admin_secret = std::env::var("ADMIN_SECRET")
        .unwrap_or_else(|_| "changeme-set-ADMIN_SECRET-in-prod".into());

    let app_state = AppState {
        db: Arc::new(Mutex::new(conn)),
        jwt_secret,
        admin_secret,
    };

    let app = Router::new()
        .merge(routes::router())
        .with_state(app_state)
        .layer(DefaultBodyLimit::max(200 * 1024 * 1024)) // 200 Mo max pour les ZIPs de sync
        .layer(CorsLayer::permissive())
        .layer(TraceLayer::new_for_http());

    let addr = std::env::var("LISTEN_ADDR").unwrap_or_else(|_| "0.0.0.0:3000".into());
    let listener = tokio::net::TcpListener::bind(&addr).await.unwrap();
    tracing::info!("Launcher API running on http://{}", addr);
    axum::serve(listener, app).await.unwrap();
}
