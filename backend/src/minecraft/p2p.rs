use std::collections::HashMap;
use std::io;
use std::net::{TcpListener, TcpStream};
use std::path::{Path, PathBuf};
use std::sync::{Arc, Mutex, OnceLock};
use std::thread;
use std::time::Duration;

use anyhow::{anyhow, Result};
use serde::{Deserialize, Serialize};
use tauri::Emitter;
use tungstenite::{accept, Message};

pub const SIGNALING_PORT: u16 = 8765;

// ── Paths ─────────────────────────────────────────────────────────────────────

/// Dossier P2P dans AppData/YuyuFrame/p2p/
/// Doit contenir : p2p-agent.jar, remapper.jar, rust_core.dll
pub fn p2p_dir() -> PathBuf {
    dirs::data_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("YuyuFrame")
        .join("p2p")
}

// ── Signaling server ──────────────────────────────────────────────────────────

#[derive(Serialize, Deserialize, Debug)]
#[serde(tag = "type", rename_all = "snake_case")]
enum Msg {
    Join      { id: String, name: String },
    Position  { id: String, x: i32, z: i32 },
    PeerList  { peers: Vec<PeerEntry> },
    PeerJoined { id: String, name: String, x: i32, z: i32 },
    PeerLeft  { id: String },
    /// Relay de données entre pairs.
    /// `to` absent = broadcast à tous sauf `from`.
    /// `to` présent = unicast vers le pair ciblé.
    Data {
        from: String,
        #[serde(skip_serializing_if = "Option::is_none")]
        to: Option<String>,
        payload: String,
    },
}

#[derive(Serialize, Deserialize, Debug, Clone)]
struct PeerEntry { id: String, name: String, x: i32, z: i32 }

struct PeerHandle {
    name: String,
    x: i32,
    z: i32,
    tx: std::sync::mpsc::Sender<String>,
}

type PeerMap = Arc<Mutex<HashMap<String, PeerHandle>>>;

static SIGNALING_STARTED: OnceLock<()> = OnceLock::new();

/// Démarre le signaling WebSocket en arrière-plan (idempotent).
pub fn start_signaling(app: tauri::AppHandle) {
    SIGNALING_STARTED.get_or_init(|| {
        let peers: PeerMap = Arc::new(Mutex::new(HashMap::new()));
        let pm = peers.clone();
        thread::spawn(move || {
            let listener = match TcpListener::bind(("127.0.0.1", SIGNALING_PORT)) {
                Ok(l) => l,
                Err(e) => {
                    tracing::error!("[P2P Signaling] Bind impossible sur le port {}: {}", SIGNALING_PORT, e);
                    return;
                }
            };
            let _ = app.emit("game_log", serde_json::json!({
                "line": format!("[P2P Signaling] En écoute sur ws://127.0.0.1:{}", SIGNALING_PORT),
                "level": "out"
            }));
            for stream in listener.incoming().flatten() {
                let pm = pm.clone();
                thread::spawn(move || handle_peer(stream, pm));
            }
        });
    });
}

fn send_all(peers: &PeerMap, exclude: &str, json: &str) {
    let map = peers.lock().unwrap();
    for (id, p) in map.iter() {
        if id != exclude {
            p.tx.send(json.to_owned()).ok();
        }
    }
}

fn handle_peer(stream: TcpStream, peers: PeerMap) {
    stream.set_read_timeout(Some(Duration::from_millis(10))).ok();
    stream.set_nodelay(true).ok();

    let mut ws = match accept(stream) {
        Ok(w)  => w,
        Err(_) => return,
    };

    let (tx, rx) = std::sync::mpsc::channel::<String>();
    let mut my_id: Option<String> = None;

    loop {
        while let Ok(msg) = rx.try_recv() {
            if ws.send(Message::Text(msg.into())).is_err() { return; }
        }

        match ws.read() {
            Ok(Message::Text(raw)) => {
                let raw = raw.to_string();
                match serde_json::from_str::<Msg>(&raw) {
                    Ok(Msg::Join { id, name }) => {
                        let list = {
                            let map = peers.lock().unwrap();
                            let entries: Vec<PeerEntry> = map.iter()
                                .map(|(eid, p)| PeerEntry { id: eid.clone(), name: p.name.clone(), x: p.x, z: p.z })
                                .collect();
                            serde_json::to_string(&Msg::PeerList { peers: entries }).unwrap()
                        };
                        tx.send(list).ok();

                        peers.lock().unwrap().insert(id.clone(), PeerHandle {
                            name: name.clone(), x: 0, z: 0, tx: tx.clone(),
                        });
                        my_id = Some(id.clone());

                        let joined = serde_json::to_string(&Msg::PeerJoined {
                            id: id.clone(), name: name.clone(), x: 0, z: 0,
                        }).unwrap();
                        send_all(&peers, &id, &joined);

                        tracing::info!("[P2P Signaling] + {} ({}...)", name, &id[..8.min(id.len())]);
                    }
                    Ok(Msg::Position { id, x, z }) => {
                        if let Some(p) = peers.lock().unwrap().get_mut(&id) {
                            p.x = x; p.z = z;
                        }
                        send_all(&peers, &id, &raw);
                    }
                    Ok(Msg::Data { ref from, ref to, ref payload }) => {
                        let out = serde_json::to_string(&Msg::Data {
                            from: from.clone(),
                            to: to.clone(),
                            payload: payload.clone(),
                        }).unwrap();
                        match to {
                            Some(target) => {
                                // Unicast vers le pair ciblé
                                if let Some(peer) = peers.lock().unwrap().get(target.as_str()) {
                                    peer.tx.send(out).ok();
                                }
                            }
                            None => {
                                // Broadcast à tous sauf l'émetteur
                                send_all(&peers, from, &out);
                            }
                        }
                    }
                    _ => {}
                }
            }
            Ok(Message::Ping(d)) => { ws.send(Message::Pong(d)).ok(); }
            Ok(Message::Close(_)) => break,
            Err(tungstenite::Error::Io(e))
                if e.kind() == io::ErrorKind::WouldBlock
                || e.kind() == io::ErrorKind::TimedOut => {}
            Err(_) => break,
            _ => {}
        }
    }

    if let Some(id) = my_id {
        let name = peers.lock().unwrap()
            .remove(&id).map(|p| p.name).unwrap_or_default();
        let left = serde_json::to_string(&Msg::PeerLeft { id: id.clone() }).unwrap();
        send_all(&peers, &id, &left);
        tracing::info!("[P2P Signaling] - {} déconnecté", name);
    }
}

// ── Mapped JAR ────────────────────────────────────────────────────────────────

/// Retourne le chemin vers `client-<ver>-mapped.jar`, le créant si besoin.
/// `java` : exécutable Java déjà résolu par ensure_java().
pub async fn ensure_mapped_jar(
    version: &str,
    client_jar: &Path,
    client: &reqwest::Client,
    app: &tauri::AppHandle,
    java: &str,
) -> Result<PathBuf> {
    let cache_dir = p2p_dir().join("cache");
    tokio::fs::create_dir_all(&cache_dir).await?;

    let mapped_jar = cache_dir.join(format!("client-{}-mapped.jar", version));
    if mapped_jar.exists() {
        let _ = app.emit("game_log", serde_json::json!({
            "line": format!("[P2P] JAR mappé en cache : {}", mapped_jar.display()),
            "level": "out"
        }));
        return Ok(mapped_jar);
    }

    // Mappings
    let mappings = cache_dir.join(format!("client-mappings-{}.txt", version));
    if !mappings.exists() {
        download_mappings(version, &mappings, client, app).await?;
    }

    // Vérifier remapper.jar
    let remapper_jar = p2p_dir().join("remapper.jar");
    if !remapper_jar.exists() {
        return Err(anyhow!(
            "remapper.jar manquant dans {}\n  Copier P2P-Test/remapper/build/remapper.jar vers ce dossier",
            p2p_dir().display()
        ));
    }

    // Lancer le remapping
    tracing::info!("[P2P] Remapping {} en cours...", version);
    let _ = app.emit("download_progress", serde_json::json!({
        "current": 0, "total": 100,
        "message": format!("P2P : remapping Minecraft {} (1-3 min)...", version)
    }));

    let status = tokio::process::Command::new(java)
        .args(["-Xmx512m", "-jar"])
        .arg(&remapper_jar)
        .arg(client_jar)
        .arg(&mappings)
        .arg(&mapped_jar)
        .status()
        .await
        .map_err(|e| anyhow!("java introuvable pour le remapping : {}", e))?;

    if !status.success() {
        return Err(anyhow!("Remapping du JAR Minecraft échoué — voir les logs"));
    }

    let _ = app.emit("game_log", serde_json::json!({
        "line": format!("[P2P] JAR mappé → {}", mapped_jar.display()),
        "level": "out"
    }));
    Ok(mapped_jar)
}

async fn download_mappings(
    version: &str,
    dest: &Path,
    client: &reqwest::Client,
    app: &tauri::AppHandle,
) -> Result<()> {
    tracing::info!("[P2P] Téléchargement des mappings Mojang pour {}...", version);
    let _ = app.emit("download_progress", serde_json::json!({
        "current": 0, "total": 100,
        "message": format!("P2P : mappings Mojang {}...", version)
    }));

    let manifest: serde_json::Value = client
        .get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
        .send().await?.json().await?;

    let ver_url = manifest["versions"]
        .as_array()
        .and_then(|a| a.iter().find(|v| v["id"].as_str() == Some(version)))
        .and_then(|v| v["url"].as_str())
        .ok_or_else(|| anyhow!("Version {} introuvable dans le manifest Mojang", version))?
        .to_owned();

    let ver_data: serde_json::Value = client.get(&ver_url).send().await?.json().await?;

    let url = ver_data["downloads"]["client_mappings"]["url"]
        .as_str()
        .ok_or_else(|| anyhow!(
            "Pas de client_mappings pour {} — version < 1.14.4 non supportée", version
        ))?
        .to_owned();

    tracing::info!("[P2P] Mappings URL : {}", url);
    let bytes = client.get(&url).send().await?.bytes().await?;
    tokio::fs::write(dest, &bytes).await?;
    tracing::info!("[P2P] Mappings → {}", dest.display());
    Ok(())
}
