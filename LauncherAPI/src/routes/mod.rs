mod admin;
mod auth;
mod health;
mod sync;

use axum::Router;

use crate::state::AppState;

pub fn router() -> Router<AppState> {
    Router::new()
        .merge(health::router())
        .merge(auth::router())
        .merge(sync::router())
        .merge(admin::router())
}
