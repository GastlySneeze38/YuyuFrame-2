// Cet endpoint n'existe qu'en build debug (cargo build sans --release).
// En production, le routeur ne l'enregistre pas du tout.

use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    routing::post,
    Json, Router,
};
use serde::{Deserialize, Serialize};

use crate::{db, jwt, state::AppState};

type ApiError = (StatusCode, String);

#[derive(Deserialize)]
pub struct SimulateBody {
    plan: String,
}

#[derive(Serialize)]
pub struct SimulateResp {
    user_id: i64,
    plan: String,
    plan_expires_at: Option<i64>,
}

pub fn router() -> Router<AppState> {
    let r = Router::new();
    #[cfg(debug_assertions)]
    let r = r.route("/dev/simulate-payment", post(simulate_payment));
    r
}

#[cfg(debug_assertions)]
async fn simulate_payment(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<SimulateBody>,
) -> Result<Json<SimulateResp>, ApiError> {
    let token = headers
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer "))
        .ok_or_else(|| (StatusCode::UNAUTHORIZED, "Token manquant".into()))?;

    let claims = jwt::decode_jwt(token, &state.jwt_secret)
        .map_err(|_| (StatusCode::UNAUTHORIZED, "Token invalide ou expiré".into()))?;

    let valid_plans = ["free", "premium", "ultimate"];
    if !valid_plans.contains(&body.plan.as_str()) {
        return Err((
            StatusCode::BAD_REQUEST,
            format!("Plan invalide. Valeurs acceptées : {}", valid_plans.join(", ")),
        ));
    }

    let expires_at = if body.plan == "free" {
        None
    } else {
        Some(chrono::Utc::now().timestamp() + 31 * 24 * 3600)
    };

    let conn = state
        .db
        .lock()
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    db::set_user_plan(&conn, claims.sub, &body.plan, expires_at)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    tracing::info!(
        "[DEV] Paiement simulé : user {} → plan {} (expire dans 31 jours)",
        claims.sub,
        body.plan
    );

    Ok(Json(SimulateResp {
        user_id: claims.sub,
        plan: body.plan,
        plan_expires_at: expires_at,
    }))
}
