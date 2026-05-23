use serde::{Deserialize, Serialize};
use std::io::Write;
use std::path::PathBuf;

use crate::state::SharedState;
use super::instances::instance_dir;

fn api_base() -> String {
    std::env::var("YUYU_API_URL").unwrap_or_else(|_| "http://localhost:3000".into())
}

#[derive(Serialize, Deserialize, Clone)]
pub struct SyncInstance {
    pub id: i64,
    pub instance_name: String,
    pub mc_version: String,
    pub loader: String,
    pub ram_mb: u32,
    pub has_data: bool,
    pub updated_at: i64,
}

fn get_token(state: &crate::state::AppState) -> Result<String, String> {
    state
        .yuyu_session
        .as_ref()
        .map(|s| s.token.clone())
        .ok_or_else(|| "Non connecté à YuyuFrame".into())
}

// ── ZIP helpers ────────────────────────────────────────────────────────────────

fn build_instance_zip(inst_dir: PathBuf) -> Result<Vec<u8>, String> {
    use std::io::Cursor;
    use zip::write::SimpleFileOptions;

    let buf = Vec::new();
    let cursor = Cursor::new(buf);
    let mut zip = zip::ZipWriter::new(cursor);
    let options = SimpleFileOptions::default()
        .compression_method(zip::CompressionMethod::Deflated);

    let config_dir = inst_dir.join("config");
    if config_dir.is_dir() {
        add_dir_to_zip(&mut zip, &config_dir, &config_dir, "config", options)?;
    }

    let saves_dir = inst_dir.join("saves");
    if saves_dir.is_dir() {
        let mut saves = collect_saves(&saves_dir)?;
        saves.truncate(3);
        for save_dir in saves {
            let save_name = save_dir
                .file_name()
                .and_then(|n| n.to_str())
                .unwrap_or("unknown")
                .to_string();
            let prefix = format!("saves/{}", save_name);
            add_dir_to_zip(&mut zip, &save_dir, &save_dir, &prefix, options)?;
        }
    }

    let cursor = zip.finish().map_err(|e| e.to_string())?;
    Ok(cursor.into_inner())
}

fn collect_saves(saves_dir: &PathBuf) -> Result<Vec<PathBuf>, String> {
    let mut saves: Vec<(std::time::SystemTime, PathBuf)> = std::fs::read_dir(saves_dir)
        .map_err(|e| e.to_string())?
        .filter_map(|e| e.ok())
        .filter(|e| e.path().is_dir())
        .filter_map(|e| {
            let path = e.path();
            let mtime = std::fs::metadata(&path).ok()?.modified().ok()?;
            Some((mtime, path))
        })
        .collect();
    saves.sort_by(|a, b| b.0.cmp(&a.0));
    Ok(saves.into_iter().map(|(_, p)| p).collect())
}

fn add_dir_to_zip<W: Write + std::io::Seek>(
    zip: &mut zip::ZipWriter<W>,
    dir: &PathBuf,
    base: &PathBuf,
    prefix: &str,
    options: zip::write::SimpleFileOptions,
) -> Result<(), String> {
    let entries =
        std::fs::read_dir(dir).map_err(|e| e.to_string())?;
    for entry in entries {
        let entry = entry.map_err(|e| e.to_string())?;
        let path = entry.path();
        let relative = path
            .strip_prefix(base)
            .map_err(|e| e.to_string())?
            .to_string_lossy()
            .replace('\\', "/");
        let zip_path = format!("{}/{}", prefix, relative);

        if path.is_dir() {
            zip.add_directory(format!("{}/", zip_path), options)
                .map_err(|e| e.to_string())?;
            add_dir_to_zip(zip, &path, base, prefix, options)?;
        } else {
            zip.start_file(&zip_path, options)
                .map_err(|e| e.to_string())?;
            let data = std::fs::read(&path).map_err(|e| e.to_string())?;
            zip.write_all(&data).map_err(|e| e.to_string())?;
        }
    }
    Ok(())
}

fn extract_zip_to_instance(zip_bytes: Vec<u8>, inst_dir: PathBuf) -> Result<(), String> {
    use std::io::{Cursor, Read};
    let cursor = Cursor::new(zip_bytes);
    let mut archive = zip::ZipArchive::new(cursor).map_err(|e| e.to_string())?;

    for i in 0..archive.len() {
        let mut file = archive.by_index(i).map_err(|e| e.to_string())?;
        let file_name = file.name().to_string();

        // Prevent path traversal
        if file_name.contains("..") {
            continue;
        }

        let out_path = inst_dir.join(&file_name);

        if file_name.ends_with('/') {
            std::fs::create_dir_all(&out_path).map_err(|e| e.to_string())?;
        } else {
            if let Some(parent) = out_path.parent() {
                std::fs::create_dir_all(parent).map_err(|e| e.to_string())?;
            }
            let mut buf = Vec::new();
            file.read_to_end(&mut buf).map_err(|e| e.to_string())?;
            std::fs::write(&out_path, &buf).map_err(|e| e.to_string())?;
        }
    }
    Ok(())
}

// ── Tauri commands ─────────────────────────────────────────────────────────────

#[tauri::command]
pub async fn sync_list_instances(
    state: tauri::State<'_, SharedState>,
) -> Result<Vec<SyncInstance>, String> {
    let token = {
        let s = state.read().await;
        get_token(&s)?
    };

    let client = reqwest::Client::new();
    let resp = client
        .get(format!("{}/sync/instances", api_base()))
        .bearer_auth(&token)
        .send()
        .await
        .map_err(|e| format!("Serveur inaccessible : {e}"))?;

    if !resp.status().is_success() {
        return Err(resp.text().await.unwrap_or_default());
    }

    resp.json::<Vec<SyncInstance>>().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn sync_push_instance(
    state: tauri::State<'_, SharedState>,
    instance_id: String,
) -> Result<SyncInstance, String> {
    use crate::db;

    let (token, instance) = {
        let s = state.read().await;
        let token = get_token(&s)?;
        let conn = s.db.lock().await;
        let row = db::instance_get(&conn, &instance_id)
            .map_err(|e| e.to_string())?
            .ok_or("Instance introuvable")?;
        (token, row)
    };

    let client = reqwest::Client::new();

    // Enregistre / met à jour les métadonnées
    let meta_resp = client
        .post(format!("{}/sync/instances", api_base()))
        .bearer_auth(&token)
        .json(&serde_json::json!({
            "instance_name": instance.name,
            "mc_version":    instance.mc_version,
            "loader":        instance.loader,
            "ram_mb":        instance.ram_mb,
        }))
        .send()
        .await
        .map_err(|e| format!("Serveur inaccessible : {e}"))?;

    if !meta_resp.status().is_success() {
        return Err(meta_resp.text().await.unwrap_or_default());
    }

    let sync_inst: SyncInstance = meta_resp.json().await.map_err(|e| e.to_string())?;
    let sync_id = sync_inst.id;

    // Compression ZIP dans un thread bloquant
    let dir = instance_dir(&instance_id);
    let zip_bytes = tokio::task::spawn_blocking(move || build_instance_zip(dir))
        .await
        .map_err(|e| e.to_string())??;

    // Upload des données
    let data_resp = client
        .post(format!("{}/sync/instances/{}/data", api_base(), sync_id))
        .bearer_auth(&token)
        .header("Content-Type", "application/octet-stream")
        .body(zip_bytes)
        .send()
        .await
        .map_err(|e| format!("Serveur inaccessible : {e}"))?;

    if !data_resp.status().is_success() {
        return Err(data_resp.text().await.unwrap_or_default());
    }

    data_resp.json::<SyncInstance>().await.map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn sync_pull_instance(
    state: tauri::State<'_, SharedState>,
    sync_id: i64,
    instance_id: String,
) -> Result<(), String> {
    let token = {
        let s = state.read().await;
        get_token(&s)?
    };

    let client = reqwest::Client::new();
    let resp = client
        .get(format!("{}/sync/instances/{}/data", api_base(), sync_id))
        .bearer_auth(&token)
        .send()
        .await
        .map_err(|e| format!("Serveur inaccessible : {e}"))?;

    if !resp.status().is_success() {
        return Err(resp.text().await.unwrap_or_default());
    }

    let zip_bytes = resp.bytes().await.map_err(|e| e.to_string())?.to_vec();
    let dir = instance_dir(&instance_id);

    tokio::task::spawn_blocking(move || extract_zip_to_instance(zip_bytes, dir))
        .await
        .map_err(|e| e.to_string())?
}

#[tauri::command]
pub async fn sync_delete_instance(
    state: tauri::State<'_, SharedState>,
    sync_id: i64,
) -> Result<(), String> {
    let token = {
        let s = state.read().await;
        get_token(&s)?
    };

    let client = reqwest::Client::new();
    let resp = client
        .delete(format!("{}/sync/instances/{}", api_base(), sync_id))
        .bearer_auth(&token)
        .send()
        .await
        .map_err(|e| format!("Serveur inaccessible : {e}"))?;

    if !resp.status().is_success() {
        return Err(resp.text().await.unwrap_or_default());
    }

    Ok(())
}
