use axum::{
    extract::{Multipart, Path, State},
    http::StatusCode,
    Json,
};
use serde::Serialize;
use std::path::PathBuf;

use crate::minecraft::launcher::minecraft_dir;
use crate::state::SharedState;

#[derive(Serialize, Clone)]
pub struct ModInfo {
    pub name: String,
    pub size: u64,
    pub enabled: bool,
}

fn mods_dir() -> PathBuf {
    minecraft_dir().join("mods")
}

pub async fn list_mods(State(_state): State<SharedState>) -> Json<Vec<ModInfo>> {
    let dir = mods_dir();
    let mut mods = Vec::new();

    if let Ok(mut entries) = tokio::fs::read_dir(&dir).await {
        while let Ok(Some(entry)) = entries.next_entry().await {
            let path = entry.path();
            let name = path
                .file_name()
                .unwrap_or_default()
                .to_string_lossy()
                .to_string();

            let enabled = name.ends_with(".jar") && !name.ends_with(".jar.disabled");
            let disabled = name.ends_with(".jar.disabled");

            if !enabled && !disabled {
                continue;
            }

            let size = entry.metadata().await.map(|m| m.len()).unwrap_or(0);
            mods.push(ModInfo { name, size, enabled });
        }
    }

    mods.sort_by(|a, b| a.name.to_lowercase().cmp(&b.name.to_lowercase()));
    Json(mods)
}

pub async fn toggle_mod(
    State(_state): State<SharedState>,
    Path(name): Path<String>,
) -> Result<Json<ModInfo>, (StatusCode, String)> {
    let dir = mods_dir();
    let from = dir.join(&name);

    if !from.exists() {
        return Err((StatusCode::NOT_FOUND, format!("Mod '{}' introuvable", name)));
    }

    let (to_name, enabled) = if name.ends_with(".jar.disabled") {
        (name.trim_end_matches(".disabled").to_string(), true)
    } else if name.ends_with(".jar") {
        (format!("{}.disabled", name), false)
    } else {
        return Err((StatusCode::BAD_REQUEST, "Nom de mod invalide".to_string()));
    };

    let to = dir.join(&to_name);
    tokio::fs::rename(&from, &to)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    let size = tokio::fs::metadata(&to).await.map(|m| m.len()).unwrap_or(0);
    Ok(Json(ModInfo {
        name: to_name,
        size,
        enabled,
    }))
}

pub async fn delete_mod(
    State(_state): State<SharedState>,
    Path(name): Path<String>,
) -> Result<StatusCode, (StatusCode, String)> {
    let dir = mods_dir();
    let path = dir.join(&name);

    if !path.exists() {
        return Err((StatusCode::NOT_FOUND, format!("Mod '{}' introuvable", name)));
    }

    // Prevent path traversal
    let canonical = path
        .canonicalize()
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;
    let canonical_dir = dir.canonicalize().unwrap_or(dir);
    if !canonical.starts_with(&canonical_dir) {
        return Err((StatusCode::FORBIDDEN, "Accès refusé".to_string()));
    }

    tokio::fs::remove_file(&canonical)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    Ok(StatusCode::NO_CONTENT)
}

pub async fn upload_mod(
    State(_state): State<SharedState>,
    mut multipart: Multipart,
) -> Result<Json<ModInfo>, (StatusCode, String)> {
    let dir = mods_dir();
    tokio::fs::create_dir_all(&dir)
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    while let Some(field) = multipart
        .next_field()
        .await
        .map_err(|e| (StatusCode::BAD_REQUEST, e.to_string()))?
    {
        let filename = field
            .file_name()
            .map(|s| s.to_string())
            .unwrap_or_else(|| "mod.jar".to_string());

        if !filename.ends_with(".jar") {
            return Err((
                StatusCode::BAD_REQUEST,
                "Seuls les fichiers .jar sont acceptés".to_string(),
            ));
        }

        // Strip any path components — keep only the filename
        let safe_name = std::path::Path::new(&filename)
            .file_name()
            .map(|n| n.to_string_lossy().to_string())
            .unwrap_or_else(|| "mod.jar".to_string());

        let dest = dir.join(&safe_name);
        let data = field
            .bytes()
            .await
            .map_err(|e| (StatusCode::BAD_REQUEST, e.to_string()))?;

        tokio::fs::write(&dest, &data)
            .await
            .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

        let size = data.len() as u64;
        return Ok(Json(ModInfo {
            name: safe_name,
            size,
            enabled: true,
        }));
    }

    Err((StatusCode::BAD_REQUEST, "Aucun fichier fourni".to_string()))
}
