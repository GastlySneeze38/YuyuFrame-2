mod admin;
mod auth;
mod checkout;
mod dev;
mod health;
mod lemon;
mod stripe;
mod sync;

use axum::Router;

use crate::state::AppState;

pub fn router() -> Router<AppState> {
    Router::new()
        .merge(health::router())
        .merge(auth::router())
        .merge(sync::router())
        .merge(admin::router())
        .merge(stripe::router())
        .merge(lemon::router())
        .merge(checkout::router())
        .merge(dev::router())
}
