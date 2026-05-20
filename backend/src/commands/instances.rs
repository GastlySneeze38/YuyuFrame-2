use serde::{Deserialize, Serialize};
use std::path::PathBuf;

use crate::db;
use crate::minecraft::launcher::minecraft_dir;
use crate::state::SharedState;

#[derive(Serialize, Deserialize, Clone)]
pub struct Instance {
    pub id: String,
    pub name: String,
    pub mc_version: String,
    pub loader: String,
    pub ram_mb: u32,
}

fn yuyu_dir() -> PathBuf {
    minecraft_dir()
        .parent()
        .map(|p| p.to_path_buf())
        .unwrap_or_else(|| PathBuf::from("."))
}

pub fn instance_dir(id: &str) -> PathBuf {
    yuyu_dir().join("instances").join(id)
}

pub fn instance_mods_dir(id: &str) -> PathBuf {
    instance_dir(id).join("mods")
}

fn gen_id() -> String {
    use rand::Rng;
    rand::thread_rng()
        .sample_iter(rand::distributions::Alphanumeric)
        .take(12)
        .map(char::from)
        .collect::<String>()
        .to_lowercase()
}

fn row_to_instance(r: db::InstanceRow) -> Instance {
    Instance { id: r.id, name: r.name, mc_version: r.mc_version, loader: r.loader, ram_mb: r.ram_mb }
}

#[tauri::command]
pub async fn instance_list(state: tauri::State<'_, SharedState>) -> Result<Vec<Instance>, String> {
    let s = state.read().await;
    let db = s.db.lock().await;
    db::instance_list(&db)
        .map(|rows| rows.into_iter().map(row_to_instance).collect())
        .map_err(|e| e.to_string())
}

#[tauri::command]
pub async fn instance_create(
    state: tauri::State<'_, SharedState>,
    name: String,
    mc_version: String,
    loader: String,
    ram_mb: u32,
) -> Result<Instance, String> {
    if name.trim().is_empty() {
        return Err("Le nom de l'instance est requis".into());
    }
    let id = gen_id();
    tokio::fs::create_dir_all(instance_mods_dir(&id))
        .await
        .map_err(|e| e.to_string())?;
    let name = name.trim().to_string();
    let s = state.read().await;
    let db = s.db.lock().await;
    db::instance_insert(&db, &id, &name, &mc_version, &loader, ram_mb)
        .map_err(|e| e.to_string())?;
    Ok(Instance { id, name, mc_version, loader, ram_mb })
}

#[tauri::command]
pub async fn instance_delete(
    state: tauri::State<'_, SharedState>,
    id: String,
) -> Result<(), String> {
    {
        let s = state.read().await;
        let db = s.db.lock().await;
        db::instance_delete(&db, &id).map_err(|e| e.to_string())?;
    }
    let dir = instance_dir(&id);
    if dir.exists() {
        tokio::fs::remove_dir_all(&dir).await.map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
pub async fn instance_update(
    state: tauri::State<'_, SharedState>,
    id: String,
    name: String,
    mc_version: String,
    loader: String,
    ram_mb: u32,
) -> Result<Instance, String> {
    let name = name.trim().to_string();
    let s = state.read().await;
    let db = s.db.lock().await;
    db::instance_update(&db, &id, &name, &mc_version, &loader, ram_mb)
        .map_err(|e| e.to_string())?;
    let row = db::instance_get(&db, &id)
        .map_err(|e| e.to_string())?
        .ok_or("Instance introuvable")?;
    Ok(row_to_instance(row))
}
