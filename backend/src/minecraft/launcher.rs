use anyhow::{anyhow, Result};
use std::path::PathBuf;
use tokio::io::AsyncWriteExt;

use crate::state::{DownloadProgress, MinecraftSession, SharedState};
use super::versions::{
    fetch_asset_index, fetch_version_details, fetch_version_list, Library, VersionDetails,
};

fn minecraft_dir() -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("YuyuFrame")
        .join(".minecraft")
}

async fn set_progress(state: &SharedState, current: u64, total: u64, message: &str) {
    state.write().await.download_progress = Some(DownloadProgress {
        current,
        total,
        message: message.to_string(),
    });
}

pub async fn download_and_launch(
    version_id: &str,
    session: &MinecraftSession,
    ram_mb: u32,
    state: SharedState,
) -> Result<()> {
    let mc_dir = minecraft_dir();
    let versions_dir = mc_dir.join("versions").join(version_id);
    let libraries_dir = mc_dir.join("libraries");
    let assets_dir = mc_dir.join("assets");
    let natives_dir = versions_dir.join("natives");

    for dir in [&versions_dir, &libraries_dir, &assets_dir, &natives_dir] {
        tokio::fs::create_dir_all(dir).await?;
    }

    // Resolve version URL
    set_progress(&state, 0, 100, "Récupération du manifest...").await;
    let versions = fetch_version_list().await?;
    let version_info = versions
        .iter()
        .find(|v| v.id == version_id)
        .ok_or_else(|| anyhow!("Version {} introuvable", version_id))?;

    set_progress(&state, 5, 100, "Récupération des détails...").await;
    let details = fetch_version_details(&version_info.url).await?;

    // Client JAR
    let client_jar = versions_dir.join(format!("{}.jar", version_id));
    if !client_jar.exists() {
        set_progress(&state, 10, 100, "Téléchargement du client Minecraft...").await;
        download_file(&details.downloads.client.url, &client_jar).await?;
    }

    // Libraries
    set_progress(&state, 20, 100, "Téléchargement des bibliothèques...").await;
    let mut classpath = vec![client_jar.to_string_lossy().to_string()];
    let total_libs = details.libraries.len() as u64;

    for (i, lib) in details.libraries.iter().enumerate() {
        if !should_download_library(lib) {
            continue;
        }
        if let Some(ref dl) = lib.downloads {
            if let Some(ref artifact) = dl.artifact {
                let lib_path = library_jar_path(&libraries_dir, &lib.name);
                if let Some(parent) = lib_path.parent() {
                    tokio::fs::create_dir_all(parent).await?;
                }
                if !lib_path.exists() {
                    download_file(&artifact.url, &lib_path).await?;
                }
                classpath.push(lib_path.to_string_lossy().to_string());
            }
        }
        if i as u64 % 10 == 0 {
            set_progress(
                &state,
                20 + i as u64 * 40 / total_libs.max(1),
                100,
                &format!("Bibliothèques {}/{}", i + 1, total_libs),
            )
            .await;
        }
    }

    // Assets
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
                &state,
                60 + asset_count * 30 / total_assets.max(1),
                100,
                &format!("Assets {}/{}", asset_count, total_assets),
            )
            .await;
        }
    }

    // Build and run
    set_progress(&state, 95, 100, "Lancement de Minecraft...").await;
    let java = find_java()?;

    let classpath_sep = if cfg!(target_os = "windows") { ";" } else { ":" };
    let classpath_str = classpath.join(classpath_sep);

    let mut args = vec![
        format!("-Xmx{}m", ram_mb),
        format!("-Xms{}m", ram_mb / 2),
        format!("-Djava.library.path={}", natives_dir.display()),
        "-cp".to_string(),
        classpath_str,
        details.main_class.clone(),
    ];

    args.extend(build_game_args(&details, session, &mc_dir, &assets_dir, version_id));

    let mut child = tokio::process::Command::new(&java)
        .args(&args)
        .current_dir(&mc_dir)
        .spawn()?;

    state.write().await.download_progress = None;
    child.wait().await?;
    Ok(())
}

fn should_download_library(lib: &Library) -> bool {
    let os_name = if cfg!(target_os = "windows") {
        "windows"
    } else if cfg!(target_os = "macos") {
        "osx"
    } else {
        "linux"
    };

    let Some(rules) = &lib.rules else {
        return true;
    };

    let mut allowed = true;
    for rule in rules {
        let action = rule.get("action").and_then(|a| a.as_str()).unwrap_or("allow");
        if let Some(os) = rule.get("os") {
            if let Some(name) = os.get("name").and_then(|n| n.as_str()) {
                if name == os_name {
                    allowed = action == "allow";
                } else if action == "allow" {
                    allowed = false;
                }
            }
        } else {
            allowed = action == "allow";
        }
    }
    allowed
}

fn library_jar_path(base: &PathBuf, name: &str) -> PathBuf {
    // "group.id:artifact:version"
    let parts: Vec<&str> = name.split(':').collect();
    if parts.len() < 3 {
        return base.join(name);
    }
    let group_path = parts[0].replace('.', "/");
    let artifact = parts[1];
    let version = parts[2];
    base.join(group_path)
        .join(artifact)
        .join(version)
        .join(format!("{}-{}.jar", artifact, version))
}

fn build_game_args(
    details: &VersionDetails,
    session: &MinecraftSession,
    game_dir: &PathBuf,
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
        for (k, v) in replacements {
            out = out.replace(k, v);
        }
        out
    };

    let mut args = Vec::new();

    if let Some(ref mc_args) = details.minecraft_arguments {
        for part in mc_args.split_whitespace() {
            args.push(apply(part));
        }
    } else if let Some(ref arguments) = details.arguments {
        for val in &arguments.game {
            if let serde_json::Value::String(s) = val {
                args.push(apply(s));
            }
        }
    }
    args
}

fn find_java() -> Result<String> {
    #[cfg(target_os = "windows")]
    {
        let candidates = [
            r"C:\Program Files\Java\jre-21\bin\java.exe",
            r"C:\Program Files\Eclipse Adoptium\jdk-21\bin\java.exe",
            r"C:\Program Files\Microsoft\jdk-21\bin\java.exe",
            r"C:\Program Files\Java\jdk-21\bin\java.exe",
        ];
        for path in &candidates {
            if std::path::Path::new(path).exists() {
                return Ok(path.to_string());
            }
        }
    }
    // Fall back to PATH
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
