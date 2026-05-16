use axum::{extract::State, http::StatusCode, Json};
use serde::Serialize;

use crate::minecraft::versions;
use crate::state::SharedState;

#[derive(Serialize)]
pub struct VersionEntry {
    pub id: String,
    pub version_type: String,
    pub url: String,
}

pub async fn list_versions(
    State(_state): State<SharedState>,
) -> Result<Json<Vec<VersionEntry>>, (StatusCode, String)> {
    match versions::fetch_version_list().await {
        Ok(list) => Ok(Json(
            list.into_iter()
                .map(|v| VersionEntry {
                    id: v.id,
                    version_type: v.version_type,
                    url: v.url,
                })
                .collect(),
        )),
        Err(e) => Err((StatusCode::INTERNAL_SERVER_ERROR, e.to_string())),
    }
}
