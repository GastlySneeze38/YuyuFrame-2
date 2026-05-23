use axum::{
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    routing::put,
    Json, Router,
};
use serde::Deserialize;

use crate::{db, state::AppState};

type ApiError = (StatusCode, String);

#[derive(Deserialize)]
pub struct SetPlanBody {
    plan: String,
    /// Timestamp Unix d'expiration (None = permanent)
    expires_at: Option<i64>,
}

pub fn router() -> Router<AppState> {
    Router::new().route("/admin/users/{id}/plan", put(set_plan))
}

fn check_admin(headers: &HeaderMap, admin_secret: &str) -> Result<(), ApiError> {
    let provided = headers
        .get("X-Admin-Secret")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");
    if provided != admin_secret {
        return Err((StatusCode::FORBIDDEN, "Accès refusé".into()));
    }
    Ok(())
}

async fn set_plan(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(user_id): Path<i64>,
    Json(body): Json<SetPlanBody>,
) -> Result<StatusCode, ApiError> {
    check_admin(&headers, &state.admin_secret)?;

    let valid_plans = ["free", "premium", "ultimate"];
    if !valid_plans.contains(&body.plan.as_str()) {
        return Err((
            StatusCode::BAD_REQUEST,
            format!("Plan invalide. Valeurs acceptées : {}", valid_plans.join(", ")),
        ));
    }

    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    // Vérifie que l'utilisateur existe
    db::get_user_by_id(&conn, user_id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::NOT_FOUND, "Utilisateur introuvable".into()))?;

    db::set_user_plan(&conn, user_id, &body.plan, body.expires_at)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    tracing::info!(
        "Plan de l'utilisateur {} mis à jour : {} (expire : {:?})",
        user_id, body.plan, body.expires_at
    );

    Ok(StatusCode::NO_CONTENT)
}
