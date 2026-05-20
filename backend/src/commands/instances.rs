use serde::{Deserialize, Serialize};
use std::path::PathBuf;

use crate::minecraft::launcher::minecraft_dir;

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

fn instances_file() -> PathBuf {
    yuyu_dir().join("instances.json")
}

pub fn instance_dir(id: &str) -> PathBuf {
    yuyu_dir().join("instances").join(id)
}

pub fn instance_mods_dir(id: &str) -> PathBuf {
    instance_dir(id).join("mods")
}

pub fn load_instances_pub() -> Vec<Instance> {
    load_instances()
}

fn load_instances() -> Vec<Instance> {
    let path = instances_file();
    let Ok(data) = std::fs::read_to_string(&path) else {
        return vec![];
    };
    serde_json::from_str(&data).unwrap_or_default()
}

fn save_instances(instances: &[Instance]) -> Result<(), String> {
    let path = instances_file();
    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    }
    let json = serde_json::to_string_pretty(instances).map_err(|e| e.to_string())?;
    std::fs::write(&path, json).map_err(|e| e.to_string())?;
    Ok(())
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

#[tauri::command]
pub async fn instance_list() -> Result<Vec<Instance>, String> {
    Ok(load_instances())
}

#[tauri::command]
pub async fn instance_create(
    name: String,
    mc_version: String,
    loader: String,
    ram_mb: u32,
) -> Result<Instance, String> {
    if name.trim().is_empty() {
        return Err("Le nom de l'instance est requis".into());
    }
    let mut instances = load_instances();
    let id = gen_id();
    tokio::fs::create_dir_all(instance_mods_dir(&id))
        .await
        .map_err(|e| e.to_string())?;
    let instance = Instance { id, name: name.trim().to_string(), mc_version, loader, ram_mb };
    instances.push(instance.clone());
    save_instances(&instances)?;
    Ok(instance)
}

#[tauri::command]
pub async fn instance_delete(id: String) -> Result<(), String> {
    let mut instances = load_instances();
    instances.retain(|i| i.id != id);
    save_instances(&instances)?;
    let dir = instance_dir(&id);
    if dir.exists() {
        tokio::fs::remove_dir_all(&dir)
            .await
            .map_err(|e| e.to_string())?;
    }
    Ok(())
}

#[tauri::command]
pub async fn instance_update(
    id: String,
    name: String,
    mc_version: String,
    loader: String,
    ram_mb: u32,
) -> Result<Instance, String> {
    let mut instances = load_instances();
    let inst = instances
        .iter_mut()
        .find(|i| i.id == id)
        .ok_or("Instance introuvable")?;
    inst.name = name.trim().to_string();
    inst.mc_version = mc_version;
    inst.loader = loader;
    inst.ram_mb = ram_mb;
    let updated = inst.clone();
    save_instances(&instances)?;
    Ok(updated)
}
