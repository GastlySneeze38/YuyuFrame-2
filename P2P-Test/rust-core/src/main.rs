// Peer standalone pour tester le réseau P2P sans Minecraft
use rust_core::{ChunkDistributor, VIEW_DISTANCE};
use serde::{Deserialize, Serialize};
use tungstenite::{connect, Message};
use uuid::Uuid;

const SIGNALING: &str = "ws://127.0.0.1:8765";

#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(tag = "type", rename_all = "snake_case")]
enum Msg {
    Join { id: String, name: String },
    Position { id: String, x: i32, z: i32 },
    PeerList { peers: Vec<PeerEntry> },
    PeerJoined { id: String, name: String, x: i32, z: i32 },
    PeerLeft { id: String },
}

#[derive(Serialize, Deserialize, Debug, Clone)]
struct PeerEntry {
    id: String,
    name: String,
    x: i32,
    z: i32,
}

fn main() {
    let name = std::env::args()
        .nth(1)
        .unwrap_or_else(|| format!("Joueur_{}", &Uuid::new_v4().to_string()[..4]));
    let my_id = Uuid::new_v4().to_string();

    let start_x = (Uuid::new_v4().as_u128() % 21) as i32 - 10;
    let start_z = (Uuid::new_v4().as_u128() % 21) as i32 - 10;

    println!("[{}] ID: {} | pos: ({},{})", name, &my_id[..8], start_x, start_z);

    let mut dist = ChunkDistributor::new(&my_id, start_x, start_z);

    let (mut socket, _) = connect(SIGNALING).expect("Connexion au signaling impossible");
    println!("[{}] Connecté", name);

    let join = serde_json::to_string(&Msg::Join { id: my_id.clone(), name: name.clone() }).unwrap();
    socket.send(Message::Text(join.into())).unwrap();

    let pos = serde_json::to_string(&Msg::Position { id: my_id.clone(), x: start_x, z: start_z }).unwrap();
    socket.send(Message::Text(pos.into())).unwrap();

    print_ownership(&name, &dist);

    loop {
        match socket.read() {
            Ok(Message::Text(raw)) => {
                match serde_json::from_str::<Msg>(&raw) {
                    Ok(Msg::PeerList { peers }) => {
                        for p in &peers {
                            dist.peers.insert(p.id.clone(), rust_core::PeerInfo { id: p.id.clone(), x: p.x, z: p.z });
                        }
                        println!("[{}] {} peer(s) existant(s)", name, peers.len());
                        print_ownership(&name, &dist);
                    }
                    Ok(Msg::PeerJoined { id, name: pname, x, z }) => {
                        dist.peers.insert(id.clone(), rust_core::PeerInfo { id, x, z });
                        println!("[{}] → {} a rejoint", name, pname);
                        print_ownership(&name, &dist);
                    }
                    Ok(Msg::PeerLeft { id }) => {
                        dist.peers.remove(&id);
                        println!("[{}] ← peer parti", name);
                        print_ownership(&name, &dist);
                    }
                    Ok(Msg::Position { id, x, z }) => {
                        if let Some(p) = dist.peers.get_mut(&id) {
                            p.x = x;
                            p.z = z;
                        }
                        print_ownership(&name, &dist);
                    }
                    _ => {}
                }
            }
            Ok(Message::Close(_)) | Err(_) => break,
            _ => {}
        }
    }

    println!("[{}] Déconnecté", name);
}

fn print_ownership(name: &str, d: &ChunkDistributor) {
    let chunks = d.my_chunks();
    let total = ((VIEW_DISTANCE * 2 + 1) * (VIEW_DISTANCE * 2 + 1)) as usize;
    println!(
        "[{}] {}/{} chunks ({}%) | pos ({},{}) | {} peer(s)",
        name, chunks.len(), total, chunks.len() * 100 / total,
        d.my_x, d.my_z, d.peers.len()
    );
}
