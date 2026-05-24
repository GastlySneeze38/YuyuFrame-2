use axum::{
    body::Bytes,
    extract::State,
    http::{HeaderMap, StatusCode},
    routing::post,
    Router,
};
use hmac::{Hmac, Mac};
use sha2::Sha256;

use crate::{db, state::AppState};

type ApiError = (StatusCode, String);

pub fn router() -> Router<AppState> {
    Router::new().route("/stripe/webhook", post(webhook))
}

/// Vérifie la signature Stripe-Signature d'un webhook.
/// Format: `t=<timestamp>,v1=<hmac_hex>`
fn verify_signature(payload: &[u8], sig_header: &str, secret: &str) -> Result<(), ()> {
    // Extrait timestamp et signature
    let mut timestamp = "";
    let mut v1_sig = "";
    for part in sig_header.split(',') {
        if let Some(t) = part.strip_prefix("t=") { timestamp = t; }
        if let Some(s) = part.strip_prefix("v1=") { v1_sig = s; }
    }
    if timestamp.is_empty() || v1_sig.is_empty() { return Err(()); }

    // Message signé = "<timestamp>.<payload>"
    let signed = format!("{}.{}", timestamp, String::from_utf8_lossy(payload));

    let mut mac = Hmac::<Sha256>::new_from_slice(secret.as_bytes()).map_err(|_| ())?;
    mac.update(signed.as_bytes());
    let expected = hex::encode(mac.finalize().into_bytes());

    if expected != v1_sig { return Err(()); }
    Ok(())
}

async fn webhook(
    State(state): State<AppState>,
    headers: HeaderMap,
    body: Bytes,
) -> Result<StatusCode, ApiError> {
    let stripe_secret = std::env::var("STRIPE_WEBHOOK_SECRET").unwrap_or_default();
    if stripe_secret.is_empty() {
        return Err((StatusCode::SERVICE_UNAVAILABLE, "Stripe non configuré".into()));
    }

    let sig_header = headers
        .get("Stripe-Signature")
        .and_then(|v| v.to_str().ok())
        .unwrap_or("");

    verify_signature(&body, sig_header, &stripe_secret)
        .map_err(|_| (StatusCode::BAD_REQUEST, "Signature Stripe invalide".into()))?;

    let event: serde_json::Value = serde_json::from_slice(&body)
        .map_err(|e| (StatusCode::BAD_REQUEST, e.to_string()))?;

    let event_type = event["type"].as_str().unwrap_or("");

    match event_type {
        "checkout.session.completed" | "invoice.paid" => {
            handle_payment_success(&state, &event)?;
        }
        "customer.subscription.deleted" | "invoice.payment_failed" => {
            handle_subscription_ended(&state, &event)?;
        }
        _ => {}
    }

    Ok(StatusCode::OK)
}

fn handle_payment_success(state: &AppState, event: &serde_json::Value) -> Result<(), ApiError> {
    // Récupère les metadata du checkout : user_id et plan
    let metadata = &event["data"]["object"]["metadata"];
    let user_id: i64 = metadata["user_id"]
        .as_str()
        .and_then(|s| s.parse().ok())
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "user_id manquant dans metadata".into()))?;
    let plan = metadata["plan"]
        .as_str()
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "plan manquant dans metadata".into()))?;

    // Expiration : 31 jours à partir de maintenant
    let expires_at = chrono::Utc::now().timestamp() + 31 * 24 * 3600;

    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    db::set_user_plan(&conn, user_id, plan, Some(expires_at))
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    tracing::info!("Paiement reçu : user {} → plan {} (expire dans 31 jours)", user_id, plan);
    Ok(())
}

fn handle_subscription_ended(state: &AppState, event: &serde_json::Value) -> Result<(), ApiError> {
    let metadata = &event["data"]["object"]["metadata"];
    let user_id: i64 = metadata["user_id"]
        .as_str()
        .and_then(|s| s.parse().ok())
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "user_id manquant dans metadata".into()))?;

    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    db::set_user_plan(&conn, user_id, "free", None)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    tracing::info!("Abonnement annulé/échoué : user {} → free", user_id);
    Ok(())
}
