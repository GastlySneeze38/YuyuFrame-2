use axum::{
    extract::State,
    http::{HeaderMap, StatusCode},
    routing::post,
    Json, Router,
};
use serde::{Deserialize, Serialize};

use crate::{jwt, state::AppState};

type ApiError = (StatusCode, String);

#[derive(Deserialize)]
pub struct CheckoutBody {
    plan: String,
}

#[derive(Serialize)]
pub struct CheckoutResp {
    checkout_url: String,
}

pub fn router() -> Router<AppState> {
    Router::new().route("/payments/create-checkout", post(create_checkout))
}

async fn create_checkout(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<CheckoutBody>,
) -> Result<Json<CheckoutResp>, ApiError> {
    // Authentification JWT
    let token = headers
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer "))
        .ok_or_else(|| (StatusCode::UNAUTHORIZED, "Token manquant".into()))?;

    let claims = jwt::decode_jwt(token, &state.jwt_secret)
        .map_err(|_| (StatusCode::UNAUTHORIZED, "Token invalide ou expiré".into()))?;

    // Résolution du variant_id Lemon Squeezy selon le plan
    let variant_id = match body.plan.as_str() {
        "premium" => std::env::var("LEMON_SQUEEZY_VARIANT_PREMIUM").map_err(|_| {
            (
                StatusCode::SERVICE_UNAVAILABLE,
                "LEMON_SQUEEZY_VARIANT_PREMIUM non défini".into(),
            )
        })?,
        "ultimate" => std::env::var("LEMON_SQUEEZY_VARIANT_ULTIMATE").map_err(|_| {
            (
                StatusCode::SERVICE_UNAVAILABLE,
                "LEMON_SQUEEZY_VARIANT_ULTIMATE non défini".into(),
            )
        })?,
        _ => return Err((StatusCode::BAD_REQUEST, "Plan invalide. Valeurs acceptées : premium, ultimate".into())),
    };

    let api_key = std::env::var("LEMON_SQUEEZY_API_KEY").map_err(|_| {
        (
            StatusCode::SERVICE_UNAVAILABLE,
            "LEMON_SQUEEZY_API_KEY non défini".into(),
        )
    })?;

    let store_id = std::env::var("LEMON_SQUEEZY_STORE_ID").map_err(|_| {
        (
            StatusCode::SERVICE_UNAVAILABLE,
            "LEMON_SQUEEZY_STORE_ID non défini".into(),
        )
    })?;

    // Création de la session checkout via l'API Lemon Squeezy
    // custom_data est transmis tel quel dans meta.custom_data de chaque webhook
    let ls_payload = serde_json::json!({
        "data": {
            "type": "checkouts",
            "attributes": {
                "checkout_data": {
                    "custom": {
                        "user_id": claims.sub.to_string(),
                        "plan": body.plan
                    }
                }
            },
            "relationships": {
                "store": {
                    "data": { "type": "stores", "id": store_id }
                },
                "variant": {
                    "data": { "type": "variants", "id": variant_id }
                }
            }
        }
    });

    let client = reqwest::Client::new();
    let resp = client
        .post("https://api.lemonsqueezy.com/v1/checkouts")
        .header("Authorization", format!("Bearer {}", api_key))
        .header("Accept", "application/vnd.api+json")
        .header("Content-Type", "application/vnd.api+json")
        .json(&ls_payload)
        .send()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, format!("Lemon Squeezy inaccessible : {e}")))?;

    if !resp.status().is_success() {
        let msg = resp.text().await.unwrap_or_default();
        return Err((
            StatusCode::BAD_GATEWAY,
            format!("Erreur Lemon Squeezy : {}", msg),
        ));
    }

    let data: serde_json::Value = resp
        .json()
        .await
        .map_err(|e| (StatusCode::BAD_GATEWAY, e.to_string()))?;

    let checkout_url = data["data"]["attributes"]["url"]
        .as_str()
        .ok_or_else(|| {
            (
                StatusCode::BAD_GATEWAY,
                "URL de checkout absente dans la réponse Lemon Squeezy".into(),
            )
        })?
        .to_string();

    tracing::info!(
        "Checkout LS créé pour user {} (plan {}) : {}",
        claims.sub,
        body.plan,
        checkout_url
    );

    Ok(Json(CheckoutResp { checkout_url }))
}
