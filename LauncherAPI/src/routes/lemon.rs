use axum::{
    body::Bytes,
    extract::State,
    http::{HeaderMap, StatusCode},
    routing::post,
    Router,
};
use hmac::{Hmac, Mac};
use rusqlite::Connection;
use sha2::Sha256;

use crate::{db, state::AppState};

type ApiError = (StatusCode, String);

pub fn router() -> Router<AppState> {
    Router::new().route("/lemon/webhook", post(webhook))
}

/// Vérifie la signature Lemon Squeezy : HMAC-SHA256(secret, raw_body) encodé en hex.
/// L'en-tête utilisé est `X-Signature`.
fn verify_signature(payload: &[u8], sig_header: &str, secret: &str) -> Result<(), ()> {
    let mut mac = Hmac::<Sha256>::new_from_slice(secret.as_bytes()).map_err(|_| ())?;
    mac.update(payload);
    let expected = hex::encode(mac.finalize().into_bytes());
    if expected != sig_header {
        return Err(());
    }
    Ok(())
}

async fn webhook(
    State(state): State<AppState>,
    headers: HeaderMap,
    body: Bytes,
) -> Result<StatusCode, ApiError> {
    let secret = std::env::var("LEMON_SQUEEZY_WEBHOOK_SECRET").unwrap_or_default();
    if secret.is_empty() {
        return Err((StatusCode::SERVICE_UNAVAILABLE, "Lemon Squeezy non configuré".into()));
    }

    let sig = headers
        .get("X-Signature")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");

    verify_signature(&body, sig, &secret)
        .map_err(|_| (StatusCode::BAD_REQUEST, "Signature Lemon Squeezy invalide".into()))?;

    let event: serde_json::Value = serde_json::from_slice(&body)
        .map_err(|e| (StatusCode::BAD_REQUEST, e.to_string()))?;

    let event_name = event["meta"]["event_name"].as_str().unwrap_or("");

    // L'id peut être un entier ou une chaîne selon le type d'objet
    let data_id = event["data"]["id"]
        .as_str()
        .map(|s| s.to_string())
        .or_else(|| event["data"]["id"].as_i64().map(|n| n.to_string()))
        .unwrap_or_else(|| "unknown".to_string());

    let event_id = format!("{}:{}", event_name, data_id);

    let conn = state
        .db
        .lock()
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    // Idempotency : ignorer si déjà traité
    if db::webhook_event_exists(&conn, &event_id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
    {
        tracing::info!("Webhook LS dupliqué ignoré : {}", event_id);
        return Ok(StatusCode::OK);
    }

    match event_name {
        "order_created" | "subscription_created" | "subscription_payment_success" => {
            handle_payment_success(&conn, &event)?;
        }
        "subscription_expired" | "subscription_cancelled" => {
            handle_subscription_ended(&conn, &event)?;
        }
        _ => {
            tracing::debug!("Événement Lemon Squeezy ignoré : {}", event_name);
        }
    }

    db::insert_webhook_event(&conn, &event_id, "lemon_squeezy", event_name)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    Ok(StatusCode::OK)
}

fn handle_payment_success(conn: &Connection, event: &serde_json::Value) -> Result<(), ApiError> {
    let custom_data = &event["meta"]["custom_data"];

    let user_id: i64 = custom_data["user_id"]
        .as_str()
        .and_then(|s| s.parse().ok())
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "user_id manquant dans custom_data".into()))?;

    let plan = custom_data["plan"]
        .as_str()
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "plan manquant dans custom_data".into()))?;

    let expires_at = chrono::Utc::now().timestamp() + 31 * 24 * 3600;

    db::set_user_plan(conn, user_id, plan, Some(expires_at))
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    tracing::info!(
        "Paiement LS reçu : user {} → plan {} (expire dans 31 jours)",
        user_id,
        plan
    );
    Ok(())
}

fn handle_subscription_ended(
    conn: &Connection,
    event: &serde_json::Value,
) -> Result<(), ApiError> {
    let custom_data = &event["meta"]["custom_data"];

    let user_id: i64 = custom_data["user_id"]
        .as_str()
        .and_then(|s| s.parse().ok())
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "user_id manquant dans custom_data".into()))?;

    db::set_user_plan(conn, user_id, "free", None)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    tracing::info!("Abonnement LS annulé/expiré : user {} → free", user_id);
    Ok(())
}
