use base64::Engine as _;
use serde::Serialize;

use crate::commands::instances::instance_mods_dir;

#[derive(Serialize, Clone)]
pub struct ModInfo {
    pub name: String,
    pub size: u64,
    pub enabled: bool,
}

#[tauri::command]
pub async fn mods_list(instance_id: String) -> Result<Vec<ModInfo>, String> {
    let dir = instance_mods_dir(&instance_id);
    let mut mods = Vec::new();

    if let Ok(mut entries) = tokio::fs::read_dir(&dir).await {
        while let Ok(Some(entry)) = entries.next_entry().await {
            let path = entry.path();
            let name = path.file_name().unwrap_or_default().to_string_lossy().to_string();
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
    Ok(mods)
}

#[tauri::command]
pub async fn mods_toggle(instance_id: String, name: String) -> Result<ModInfo, String> {
    let dir = instance_mods_dir(&instance_id);
    let from = dir.join(&name);

    if !from.exists() {
        return Err(format!("Mod '{}' introuvable", name));
    }

    let (to_name, enabled) = if name.ends_with(".jar.disabled") {
        (name.trim_end_matches(".disabled").to_string(), true)
    } else if name.ends_with(".jar") {
        (format!("{}.disabled", name), false)
    } else {
        return Err("Nom de mod invalide".into());
    };

    let to = dir.join(&to_name);
    tokio::fs::rename(&from, &to).await.map_err(|e| e.to_string())?;

    let size = tokio::fs::metadata(&to).await.map(|m| m.len()).unwrap_or(0);
    Ok(ModInfo { name: to_name, size, enabled })
}

#[tauri::command]
pub async fn mods_delete(instance_id: String, name: String) -> Result<(), String> {
    let dir = instance_mods_dir(&instance_id);
    let path = dir.join(&name);

    if !path.exists() {
        return Err(format!("Mod '{}' introuvable", name));
    }

    let canonical = path.canonicalize().map_err(|e| e.to_string())?;
    let canonical_dir = dir.canonicalize().unwrap_or(dir);
    if !canonical.starts_with(&canonical_dir) {
        return Err("Accès refusé".into());
    }

    tokio::fs::remove_file(&canonical).await.map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
pub async fn mods_install(
    instance_id: String,
    url: String,
    filename: String,
) -> Result<ModInfo, String> {
    if !url.starts_with("https://cdn.modrinth.com/") {
        return Err("URL non autorisée".into());
    }

    let safe_name = std::path::Path::new(&filename)
        .file_name()
        .map(|n| n.to_string_lossy().to_string())
        .unwrap_or_else(|| "mod.jar".to_string());

    if !safe_name.ends_with(".jar") {
        return Err("Seuls les fichiers .jar sont acceptés".into());
    }

    let dir = instance_mods_dir(&instance_id);
    tokio::fs::create_dir_all(&dir).await.map_err(|e| e.to_string())?;

    let client = reqwest::Client::builder()
        .user_agent("YuyuFrame/1.0")
        .build()
        .map_err(|e| e.to_string())?;

    let resp = client.get(&url).send().await.map_err(|e| e.to_string())?;
    if !resp.status().is_success() {
        return Err(format!("Téléchargement échoué: {}", resp.status()));
    }

    let bytes = resp.bytes().await.map_err(|e| e.to_string())?;
    let dest = dir.join(&safe_name);
    tokio::fs::write(&dest, &bytes).await.map_err(|e| e.to_string())?;

    Ok(ModInfo { name: safe_name, size: bytes.len() as u64, enabled: true })
}

/// Extrait l'icône d'un mod directement depuis son JAR et la retourne en data URL base64.
/// Lit le champ `icon` de fabric.mod.json, avec repli sur pack.png.
#[tauri::command]
pub async fn mod_icon(instance_id: String, name: String) -> Result<String, String> {
    use std::io::Read;

    let dir = instance_mods_dir(&instance_id);
    let path = dir.join(&name);
    if !path.exists() {
        return Err("Mod introuvable".into());
    }

    let bytes = std::fs::read(&path).map_err(|e| e.to_string())?;

    // Lire fabric.mod.json pour trouver le chemin de l'icône
    let icon_path: Option<String> = {
        let cursor = std::io::Cursor::new(&bytes);
        if let Ok(mut archive) = zip::ZipArchive::new(cursor) {
            if let Ok(mut entry) = archive.by_name("fabric.mod.json") {
                let mut content = String::new();
                let _ = entry.read_to_string(&mut content);
                serde_json::from_str::<serde_json::Value>(&content)
                    .ok()
                    .and_then(|v| v.get("icon").and_then(|i| i.as_str()).map(|s| s.to_string()))
            } else {
                None
            }
        } else {
            None
        }
    };

    let cursor = std::io::Cursor::new(&bytes);
    let mut archive = zip::ZipArchive::new(cursor).map_err(|e| e.to_string())?;

    let candidates: Vec<&str> = if let Some(ref p) = icon_path {
        vec![p.as_str(), "pack.png"]
    } else {
        vec!["pack.png"]
    };

    for candidate in candidates {
        if let Ok(mut entry) = archive.by_name(candidate) {
            let mut icon_bytes = Vec::new();
            if entry.read_to_end(&mut icon_bytes).is_ok() && !icon_bytes.is_empty() {
                let mime = if icon_bytes.starts_with(&[0x89, 0x50, 0x4E, 0x47]) {
                    "image/png"
                } else if icon_bytes.starts_with(&[0xFF, 0xD8, 0xFF]) {
                    "image/jpeg"
                } else {
                    "image/png"
                };
                let b64 = base64::engine::general_purpose::STANDARD.encode(&icon_bytes);
                return Ok(format!("data:{};base64,{}", mime, b64));
            }
        }
    }

    Err("Pas d'icône dans ce JAR".into())
}

#[tauri::command]
pub async fn mods_upload(
    instance_id: String,
    filename: String,
    data: Vec<u8>,
) -> Result<ModInfo, String> {
    let safe_name = std::path::Path::new(&filename)
        .file_name()
        .map(|n| n.to_string_lossy().to_string())
        .unwrap_or_else(|| "mod.jar".to_string());

    if !safe_name.ends_with(".jar") {
        return Err("Seuls les fichiers .jar sont acceptés".into());
    }

    let dir = instance_mods_dir(&instance_id);
    tokio::fs::create_dir_all(&dir).await.map_err(|e| e.to_string())?;

    let dest = dir.join(&safe_name);
    let size = data.len() as u64;
    tokio::fs::write(&dest, &data).await.map_err(|e| e.to_string())?;

    Ok(ModInfo { name: safe_name, size, enabled: true })
}
