use axum::{
    body::Bytes,
    extract::{Path, State},
    http::{HeaderMap, StatusCode},
    response::Response,
    routing::{delete, get, post},
    Json, Router,
};
use serde::{Deserialize, Serialize};

use crate::{db, jwt, state::AppState};

type ApiError = (StatusCode, String);

#[derive(Deserialize)]
pub struct SyncInstanceBody {
    instance_name: String,
    mc_version: String,
    loader: String,
    ram_mb: u32,
    #[serde(default)]
    save_count: u32,
}

#[derive(Serialize)]
pub struct SyncInstanceResp {
    pub id: i64,
    pub instance_name: String,
    pub mc_version: String,
    pub loader: String,
    pub ram_mb: u32,
    pub save_count: u32,
    pub has_data: bool,
    pub updated_at: i64,
}

/// (max_saves_total, max_instances_with_saves, max_instances_no_saves)
fn plan_quotas(plan: &str) -> (i64, i64, i64) {
    match plan {
        "ultimate" => (10, 10, 10),
        "premium"  => (3, 3, 4),
        _          => (0, 0, 0),
    }
}

pub fn router() -> Router<AppState> {
    Router::new()
        .route("/sync/instances", get(list_instances))
        .route("/sync/instances", post(upsert_instance))
        .route("/sync/instances/{id}", delete(delete_instance))
        .route("/sync/instances/{id}/data", post(push_data))
        .route("/sync/instances/{id}/data", get(pull_data))
}

fn extract_user_id(headers: &HeaderMap, jwt_secret: &str) -> Result<i64, ApiError> {
    let token = headers
        .get("Authorization")
        .and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer "))
        .ok_or_else(|| (StatusCode::UNAUTHORIZED, "Token manquant".into()))?;
    let claims = jwt::decode_jwt(token, jwt_secret)
        .map_err(|_| (StatusCode::UNAUTHORIZED, "Token invalide ou expiré".into()))?;
    Ok(claims.sub)
}

/// Vérifie dans la DB que l'utilisateur a un plan premium ou ultimate actif.
fn require_premium(conn: &rusqlite::Connection, user_id: i64) -> Result<(), ApiError> {
    let is_premium = db::check_premium(conn, user_id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    if !is_premium {
        return Err((
            StatusCode::PAYMENT_REQUIRED,
            "Abonnement Premium requis pour cette fonctionnalité".into(),
        ));
    }
    Ok(())
}

/// Vérifie que l'utilisateur a un plan ultimate actif.
#[allow(dead_code)]
fn require_ultimate(conn: &rusqlite::Connection, user_id: i64) -> Result<(), ApiError> {
    let plan = db::get_active_plan(conn, user_id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    if plan != "ultimate" {
        return Err((
            StatusCode::PAYMENT_REQUIRED,
            "Abonnement Ultimate requis pour cette fonctionnalité".into(),
        ));
    }
    Ok(())
}

fn row_to_resp(row: db::SyncInstanceRow) -> SyncInstanceResp {
    SyncInstanceResp {
        id: row.id,
        instance_name: row.instance_name,
        mc_version: row.mc_version,
        loader: row.loader,
        ram_mb: row.ram_mb,
        save_count: row.save_count,
        has_data: row.has_data,
        updated_at: row.updated_at,
    }
}

async fn list_instances(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<Json<Vec<SyncInstanceResp>>, ApiError> {
    let user_id = extract_user_id(&headers, &state.jwt_secret)?;
    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    require_premium(&conn, user_id)?;
    let instances = db::sync_list(&conn, user_id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    Ok(Json(instances.into_iter().map(row_to_resp).collect()))
}

async fn upsert_instance(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<SyncInstanceBody>,
) -> Result<Json<SyncInstanceResp>, ApiError> {
    let user_id = extract_user_id(&headers, &state.jwt_secret)?;
    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    require_premium(&conn, user_id)?;

    let plan = db::get_active_plan(&conn, user_id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    let (quota_saves, quota_inst_with, quota_inst_without) = plan_quotas(&plan);

    let existing = db::sync_get_by_name(&conn, user_id, &body.instance_name)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    let (total_instances, total_saves) = db::sync_count_user(&conn, user_id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    let new_save_count = body.save_count as i64;

    if let Some(ref ex) = existing {
        let delta = new_save_count - ex.save_count as i64;
        let projected_saves = total_saves + delta;
        if projected_saves > quota_saves {
            return Err((
                StatusCode::UNPROCESSABLE_ENTITY,
                format!("Quota de saves dépassé : {}/{} utilisées (plan {})", total_saves, quota_saves, plan),
            ));
        }
    } else {
        let projected_saves = total_saves + new_save_count;
        let max_instances = if projected_saves > 0 { quota_inst_with } else { quota_inst_without };
        if total_instances >= max_instances {
            return Err((
                StatusCode::UNPROCESSABLE_ENTITY,
                format!(
                    "Quota d'instances atteint : {}/{} (plan {})",
                    total_instances, max_instances, plan
                ),
            ));
        }
        if projected_saves > quota_saves {
            return Err((
                StatusCode::UNPROCESSABLE_ENTITY,
                format!("Quota de saves dépassé : {}/{} utilisées (plan {})", total_saves, quota_saves, plan),
            ));
        }
    }

    let id = db::sync_upsert(
        &conn,
        user_id,
        &body.instance_name,
        &body.mc_version,
        &body.loader,
        body.ram_mb,
        body.save_count,
    )
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    let row = db::sync_get(&conn, id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::INTERNAL_SERVER_ERROR, "Introuvable après insertion".into()))?;
    Ok(Json(row_to_resp(row)))
}

async fn delete_instance(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<i64>,
) -> Result<StatusCode, ApiError> {
    let user_id = extract_user_id(&headers, &state.jwt_secret)?;
    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    require_premium(&conn, user_id)?;
    let row = db::sync_get(&conn, id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::NOT_FOUND, "Instance sync introuvable".into()))?;
    if row.user_id != user_id {
        return Err((StatusCode::FORBIDDEN, "Non autorisé".into()));
    }
    db::sync_delete(&conn, id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    Ok(StatusCode::NO_CONTENT)
}

async fn push_data(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<i64>,
    body: Bytes,
) -> Result<Json<SyncInstanceResp>, ApiError> {
    let user_id = extract_user_id(&headers, &state.jwt_secret)?;
    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    require_premium(&conn, user_id)?;
    let row = db::sync_get(&conn, id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::NOT_FOUND, "Instance sync introuvable".into()))?;
    if row.user_id != user_id {
        return Err((StatusCode::FORBIDDEN, "Non autorisé".into()));
    }
    db::sync_push_data(&conn, id, &body)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    let updated = db::sync_get(&conn, id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .unwrap();
    Ok(Json(row_to_resp(updated)))
}

async fn pull_data(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<i64>,
) -> Result<Response, ApiError> {
    let user_id = extract_user_id(&headers, &state.jwt_secret)?;
    let conn = state.db.lock().map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    require_premium(&conn, user_id)?;
    let row = db::sync_get(&conn, id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::NOT_FOUND, "Instance sync introuvable".into()))?;
    if row.user_id != user_id {
        return Err((StatusCode::FORBIDDEN, "Non autorisé".into()));
    }
    let data = db::sync_pull_data(&conn, id)
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?
        .ok_or_else(|| (StatusCode::NOT_FOUND, "Aucune donnée sync disponible".into()))?;

    use axum::http::header;
    let response = Response::builder()
        .status(StatusCode::OK)
        .header(header::CONTENT_TYPE, "application/octet-stream")
        .body(axum::body::Body::from(data))
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    Ok(response)
}
