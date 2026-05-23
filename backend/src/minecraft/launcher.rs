use anyhow::{anyhow, Result};
use std::path::PathBuf;
use std::process::Stdio;
use tauri::Emitter;
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};

use crate::state::{DownloadProgress, MinecraftSession, SharedState};
use super::deps;
use super::fabric;
use super::forge;
use super::versions::{
    fetch_asset_index, fetch_version_details, fetch_version_list, Artifact, Library, VersionDetails,
};

pub fn minecraft_dir() -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("YuyuFrame")
        .join(".minecraft")
}

fn set_progress(app: &tauri::AppHandle, current: u64, total: u64, message: &str) {
    let _ = app.emit("download_progress", DownloadProgress {
        current,
        total,
        message: message.to_string(),
    });
}

/// `loader` — "vanilla" | "fabric" | "forge" (None treated as vanilla)
/// `game_dir` — instance directory (saves, mods, configs); shared assets stay in minecraft_dir()
pub async fn download_and_launch(
    version_id: &str,
    loader: Option<&str>,
    session: &MinecraftSession,
    ram_mb: u32,
    game_dir: &std::path::Path,
    app: tauri::AppHandle,
    state: SharedState,
) -> Result<()> {
    let mc_dir = minecraft_dir();
    tokio::fs::create_dir_all(game_dir).await?;
    let versions_dir = mc_dir.join("versions").join(version_id);
    let libraries_dir = mc_dir.join("libraries");
    let assets_dir = mc_dir.join("assets");
    let natives_dir = versions_dir.join("natives");

    for dir in [&versions_dir, &libraries_dir, &assets_dir, &natives_dir] {
        tokio::fs::create_dir_all(dir).await?;
    }

    // ── Vanilla download ──────────────────────────────────────────────────────

    set_progress(&app, 0, 100, "Récupération du manifest...");
    let versions = fetch_version_list().await?;
    let version_info = versions
        .iter()
        .find(|v| v.id == version_id)
        .ok_or_else(|| anyhow!("Version {} introuvable", version_id))?;

    set_progress(&app, 5, 100, "Récupération des détails...");
    let details = fetch_version_details(&version_info.url).await?;

    let client_jar = versions_dir.join(format!("{}.jar", version_id));
    if !client_jar.exists() {
        set_progress(&app, 10, 100, "Téléchargement du client Minecraft...");
        download_file(&details.downloads.client.url, &client_jar).await?;
    }

    set_progress(&app, 20, 100, "Téléchargement des bibliothèques...");
    let mut classpath = Vec::new();
    let total_libs = details.libraries.len() as u64;
    let os_key = if cfg!(target_os = "windows") {
        "windows"
    } else if cfg!(target_os = "macos") {
        "osx"
    } else {
        "linux"
    };

    for (i, lib) in details.libraries.iter().enumerate() {
        if !should_download_library(lib) {
            continue;
        }
        if let Some(ref dl) = lib.downloads {
            if let Some(ref artifact) = dl.artifact {
                let lib_path = artifact_path(&libraries_dir, artifact, &lib.name);
                if let Some(parent) = lib_path.parent() {
                    tokio::fs::create_dir_all(parent).await?;
                }
                if !lib_path.exists() {
                    download_file(&artifact.url, &lib_path).await?;
                }
                if lib.name.contains(":natives-") {
                    extract_natives(&lib_path, &natives_dir).await.ok();
                } else {
                    classpath.push(lib_path.to_string_lossy().to_string());
                }
            }

            if let (Some(ref natives_map), Some(ref classifiers)) =
                (&lib.natives, &dl.classifiers)
            {
                if let Some(classifier_key) = natives_map.get(os_key) {
                    let key = classifier_key.replace(
                        "${arch}",
                        if cfg!(target_pointer_width = "64") { "64" } else { "32" },
                    );
                    if let Some(native_artifact) = classifiers.get(&key) {
                        let native_path = artifact_path(
                            &libraries_dir,
                            native_artifact,
                            &format!("{}:{}", lib.name, key),
                        );
                        if let Some(parent) = native_path.parent() {
                            tokio::fs::create_dir_all(parent).await?;
                        }
                        if !native_path.exists() {
                            download_file(&native_artifact.url, &native_path).await?;
                        }
                        extract_natives(&native_path, &natives_dir).await.ok();
                    }
                }
            }
        }
        if i as u64 % 10 == 0 {
            set_progress(
                &app,
                20 + i as u64 * 30 / total_libs.max(1),
                100,
                &format!("Bibliothèques {}/{}", i + 1, total_libs),
            );
        }
    }

    // ── Assets ────────────────────────────────────────────────────────────────

    let asset_index_path = assets_dir
        .join("indexes")
        .join(format!("{}.json", details.asset_index.id));
    tokio::fs::create_dir_all(asset_index_path.parent().unwrap()).await?;

    if !asset_index_path.exists() {
        download_file(&details.asset_index.url, &asset_index_path).await?;
    }

    let asset_index = fetch_asset_index(&details.asset_index.url).await?;
    let objects_dir = assets_dir.join("objects");
    let total_assets = asset_index.objects.len() as u64;
    let mut asset_count = 0u64;

    for obj in asset_index.objects.values() {
        let prefix = &obj.hash[..2];
        let obj_dir = objects_dir.join(prefix);
        let obj_path = obj_dir.join(&obj.hash);
        if !obj_path.exists() {
            tokio::fs::create_dir_all(&obj_dir).await?;
            let url = format!(
                "https://resources.download.minecraft.net/{}/{}",
                prefix, obj.hash
            );
            download_file(&url, &obj_path).await.ok();
        }
        asset_count += 1;
        if asset_count % 200 == 0 {
            set_progress(
                &app,
                50 + asset_count * 20 / total_assets.max(1),
                100,
                &format!("Assets {}/{}", asset_count, total_assets),
            );
        }
    }

    // ── Loader-specific setup ────────────────────────────────────────────────

    let java = find_java()?;

    let (main_class, extra_classpath, extra_game_args, extra_jvm_args) =
        match loader.unwrap_or("vanilla") {
            "fabric" => setup_fabric(version_id, &libraries_dir, &game_dir.join("mods"), &app).await?,
            "forge" => setup_forge(version_id, &mc_dir, &libraries_dir, &java, &app).await?,
            _ => (details.main_class.clone(), vec![], vec![], vec![]),
        };

    // ── Launch ───────────────────────────────────────────────────────────────

    set_progress(&app, 95, 100, "Lancement de Minecraft...");

    let classpath_sep = if cfg!(target_os = "windows") { ";" } else { ":" };

    let mut full_classpath: Vec<String> = extra_classpath;
    full_classpath.extend(classpath);
    full_classpath.push(client_jar.to_string_lossy().to_string());
    let classpath_str = dedup_classpath(full_classpath).join(classpath_sep);

    let mut args: Vec<String> = vec![
        format!("-Xmx{}m", ram_mb),
        format!("-Xms{}m", ram_mb / 2),
        format!("-Djava.library.path={}", natives_dir.display()),
    ];
    args.extend(extra_jvm_args);
    args.extend(["-cp".to_string(), classpath_str, main_class]);
    args.extend(build_game_args(&details, session, game_dir, &assets_dir, version_id));
    args.extend(extra_game_args);

    let mut child = tokio::process::Command::new(&java)
        .args(&args)
        .current_dir(&mc_dir)
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .stdin(Stdio::null())
        .spawn()?;

    let stdout = child.stdout.take().map(BufReader::new);
    let stderr = child.stderr.take().map(BufReader::new);

    let app_out = app.clone();
    let app_err = app.clone();

    if let Some(mut reader) = stdout {
        tokio::spawn(async move {
            let mut line = String::new();
            while reader.read_line(&mut line).await.unwrap_or(0) > 0 {
                let _ = app_out.emit("game_log", serde_json::json!({ "line": line.trim_end(), "level": "out" }));
                line.clear();
            }
        });
    }

    if let Some(mut reader) = stderr {
        tokio::spawn(async move {
            let mut line = String::new();
            while reader.read_line(&mut line).await.unwrap_or(0) > 0 {
                let _ = app_err.emit("game_log", serde_json::json!({ "line": line.trim_end(), "level": "err" }));
                line.clear();
            }
        });
    }

    // Clear progress — game is now running
    state.write().await.download_progress = None;
    child.wait().await?;
    Ok(())
}

// ── Loader setup helpers ──────────────────────────────────────────────────────

async fn setup_fabric(
    mc_version: &str,
    libraries_dir: &PathBuf,
    mods_dir: &PathBuf,
    app: &tauri::AppHandle,
) -> Result<(String, Vec<String>, Vec<String>, Vec<String>)> {
    set_progress(app, 72, 100, "Téléchargement Fabric Loader...");

    let profile = fabric::get_latest_profile(mc_version).await?;

    if let Err(e) = fabric::ensure_fabric_api(mc_version, mods_dir).await {
        tracing::warn!("Fabric API auto-install échoué: {}", e);
    }

    set_progress(app, 74, 100, "Résolution des dépendances des mods...");
    if let Err(e) = deps::resolve_and_install_deps(mc_version, "fabric", mods_dir, app).await {
        tracing::warn!("Résolution des dépendances échouée: {}", e);
    }

    let mut fabric_cp = Vec::new();
    let total = profile.libraries.len();
    for (i, lib) in profile.libraries.iter().enumerate() {
        if let Some(path) = fabric::download_library(lib, libraries_dir).await {
            fabric_cp.push(path.to_string_lossy().to_string());
        }
        if i % 5 == 0 {
            set_progress(app, 72 + i as u64 * 20 / total.max(1) as u64, 100, &format!("Fabric libs {}/{}", i + 1, total));
        }
    }

    let extra_jvm: Vec<String> = profile
        .arguments
        .as_ref()
        .and_then(|a| a.jvm.as_ref())
        .map(|jvm| jvm.iter().filter_map(|v| v.as_str().map(|s| s.to_string())).collect())
        .unwrap_or_default();

    Ok((profile.main_class, fabric_cp, vec![], extra_jvm))
}

async fn setup_forge(
    mc_version: &str,
    mc_dir: &PathBuf,
    libraries_dir: &PathBuf,
    java: &str,
    app: &tauri::AppHandle,
) -> Result<(String, Vec<String>, Vec<String>, Vec<String>)> {
    set_progress(app, 70, 100, "Recherche de la version Forge...");

    let forge_ver = forge::fetch_latest_version(mc_version).await?;
    tracing::info!("Forge {} pour MC {}", forge_ver, mc_version);

    if !forge::is_installed(mc_version, &forge_ver, mc_dir) {
        set_progress(app, 72, 100, "Téléchargement de l'installeur Forge...");
        forge::install(mc_version, &forge_ver, mc_dir, java).await?;
    }

    let forge_json = forge::read_version_json(mc_version, &forge_ver, mc_dir)?;
    let mut forge_cp = Vec::new();

    if let Some(libs) = &forge_json.libraries {
        let total = libs.len();
        for (i, lib) in libs.iter().enumerate() {
            if let Some(path) = forge::download_library(lib, libraries_dir).await {
                forge_cp.push(path.to_string_lossy().to_string());
            }
            if i % 5 == 0 {
                set_progress(app, 80 + i as u64 * 12 / total.max(1) as u64, 100, &format!("Forge libs {}/{}", i + 1, total));
            }
        }
    }

    let extra_game: Vec<String> = forge_json
        .arguments.as_ref().and_then(|a| a.game.as_ref())
        .map(|g| g.iter().filter_map(|v| v.as_str().map(|s| s.to_string())).collect())
        .unwrap_or_default();

    let extra_jvm: Vec<String> = forge_json
        .arguments.as_ref().and_then(|a| a.jvm.as_ref())
        .map(|j| j.iter().filter_map(|v| v.as_str().map(|s| s.to_string())).collect())
        .unwrap_or_default();

    Ok((forge_json.main_class, forge_cp, extra_game, extra_jvm))
}

// ── Helpers ──────────────────────────────────────────────────────────────────

fn dedup_classpath(entries: Vec<String>) -> Vec<String> {
    let mut seen = std::collections::HashSet::new();
    let mut result = Vec::with_capacity(entries.len());
    for entry in entries {
        let key = artifact_key(&entry);
        if seen.insert(key) {
            result.push(entry);
        }
    }
    result
}

fn artifact_key(path: &str) -> String {
    let normalized = path.replace('\\', "/");
    let marker = "/libraries/";
    if let Some(idx) = normalized.rfind(marker) {
        let rel = &normalized[idx + marker.len()..];
        let parts: Vec<&str> = rel.split('/').collect();
        if parts.len() >= 3 {
            return parts[..parts.len() - 2].join("/");
        }
    }
    normalized
}

fn should_download_library(lib: &Library) -> bool {
    let os_name = if cfg!(target_os = "windows") { "windows" } else if cfg!(target_os = "macos") { "osx" } else { "linux" };
    let Some(rules) = &lib.rules else { return true };
    let mut allowed = true;
    for rule in rules {
        let action = rule.get("action").and_then(|a| a.as_str()).unwrap_or("allow");
        if let Some(os) = rule.get("os") {
            if let Some(name) = os.get("name").and_then(|n| n.as_str()) {
                if name == os_name { allowed = action == "allow"; } else if action == "allow" { allowed = false; }
            }
        } else {
            allowed = action == "allow";
        }
    }
    allowed
}

fn artifact_path(base: &PathBuf, artifact: &Artifact, name: &str) -> PathBuf {
    if let Some(ref p) = artifact.path { return base.join(p); }
    library_jar_path(base, name)
}

fn library_jar_path(base: &PathBuf, name: &str) -> PathBuf {
    let parts: Vec<&str> = name.split(':').collect();
    if parts.len() < 3 { return base.join(name); }
    let group_path = parts[0].replace('.', "/");
    let artifact = parts[1];
    let version = parts[2];
    let classifier = parts.get(3).copied().unwrap_or("");
    let filename = if classifier.is_empty() {
        format!("{}-{}.jar", artifact, version)
    } else {
        format!("{}-{}-{}.jar", artifact, version, classifier)
    };
    base.join(group_path).join(artifact).join(version).join(filename)
}

async fn extract_natives(jar_path: &PathBuf, natives_dir: &PathBuf) -> Result<()> {
    let jar_bytes = tokio::fs::read(jar_path).await?;
    let cursor = std::io::Cursor::new(jar_bytes);
    let mut archive = zip::ZipArchive::new(cursor)?;
    for i in 0..archive.len() {
        let mut entry = archive.by_index(i)?;
        let name = entry.name().to_string();
        if name.starts_with("META-INF") || name.ends_with('/') { continue; }
        let is_native = name.ends_with(".dll") || name.ends_with(".so") || name.ends_with(".dylib") || name.ends_with(".jnilib");
        if !is_native { continue; }
        let file_name = std::path::Path::new(&name).file_name().unwrap_or_default().to_string_lossy().to_string();
        let out_path = natives_dir.join(&file_name);
        if !out_path.exists() {
            let mut out = std::fs::File::create(&out_path)?;
            std::io::copy(&mut entry, &mut out)?;
        }
    }
    Ok(())
}

fn build_game_args(
    details: &VersionDetails,
    session: &MinecraftSession,
    game_dir: &std::path::Path,
    assets_dir: &PathBuf,
    version_id: &str,
) -> Vec<String> {
    let replacements: &[(&str, &str)] = &[
        ("${auth_player_name}", &session.username),
        ("${version_name}", version_id),
        ("${game_directory}", &game_dir.to_string_lossy()),
        ("${assets_root}", &assets_dir.to_string_lossy()),
        ("${assets_index_name}", &details.asset_index.id),
        ("${auth_uuid}", &session.uuid),
        ("${auth_access_token}", &session.access_token),
        ("${user_type}", "msa"),
        ("${version_type}", "release"),
    ];

    let apply = |s: &str| -> String {
        let mut out = s.to_string();
        for (k, v) in replacements { out = out.replace(k, v); }
        out
    };

    let mut args = Vec::new();
    if let Some(ref mc_args) = details.minecraft_arguments {
        for part in mc_args.split_whitespace() { args.push(apply(part)); }
    } else if let Some(ref arguments) = details.arguments {
        for val in &arguments.game {
            if let serde_json::Value::String(s) = val { args.push(apply(s)); }
        }
    }
    args
}

pub fn find_java() -> Result<String> {
    #[cfg(target_os = "windows")]
    {
        let candidates = [
            r"C:\Program Files\Java\jre-21\bin\java.exe",
            r"C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe",
            r"C:\Program Files\Microsoft\jdk-21\bin\java.exe",
            r"C:\Program Files\Java\jdk-21\bin\java.exe",
            r"C:\Program Files\Java\jre-17\bin\java.exe",
            r"C:\Program Files\Eclipse Adoptium\jdk-17\bin\java.exe",
            r"C:\Program Files\Microsoft\jdk-17\bin\java.exe",
            r"C:\Program Files\Java\jdk-17\bin\java.exe",
        ];
        for path in &candidates {
            if std::path::Path::new(path).exists() { return Ok(path.to_string()); }
        }
    }
    Ok("java".to_string())
}

async fn download_file(url: &str, path: &PathBuf) -> Result<()> {
    let client = reqwest::Client::new();
    let resp = client.get(url).send().await?;
    if !resp.status().is_success() {
        return Err(anyhow!("Download failed {}: {}", url, resp.status()));
    }
    let bytes = resp.bytes().await?;
    let mut file = tokio::fs::File::create(path).await?;
    file.write_all(&bytes).await?;
    Ok(())
}
