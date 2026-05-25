use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use tungstenite::{connect, Message};
use uuid::Uuid;

const VIEW_DISTANCE: i32 = 3;
const SIGNALING: &str = "ws://127.0.0.1:8765";

// --- Messages réseau ---

#[derive(Serialize, Deserialize, Debug, Clone)]
#[serde(tag = "type", rename_all = "snake_case")]
enum Msg {
    Join { id: String, name: String },
    Position { id: String, x: i32, z: i32 },
    PeerList { peers: Vec<PeerInfo> },
    PeerJoined { id: String, name: String, x: i32, z: i32 },
    PeerLeft { id: String },
}

#[derive(Serialize, Deserialize, Debug, Clone)]
struct PeerInfo {
    id: String,
    name: String,
    x: i32,
    z: i32,
}

// --- Distribution de chunks ---

struct ChunkDistributor {
    my_id: String,
    my_x: i32,
    my_z: i32,
    peers: HashMap<String, PeerInfo>,
}

impl ChunkDistributor {
    fn new(my_id: &str, x: i32, z: i32) -> Self {
        Self {
            my_id: my_id.to_string(),
            my_x: x,
            my_z: z,
            peers: HashMap::new(),
        }
    }

    fn my_chunks(&self) -> Vec<(i32, i32)> {
        let mut result = Vec::new();
        let all_peers: Vec<&PeerInfo> = self.peers.values().collect();

        for cx in (self.my_x - VIEW_DISTANCE)..=(self.my_x + VIEW_DISTANCE) {
            for cz in (self.my_z - VIEW_DISTANCE)..=(self.my_z + VIEW_DISTANCE) {
                // Peers dans la zone du chunk
                let mut nearby: Vec<&str> = all_peers
                    .iter()
                    .filter(|p| {
                        (p.x - cx).abs() <= VIEW_DISTANCE
                            && (p.z - cz).abs() <= VIEW_DISTANCE
                    })
                    .map(|p| p.id.as_str())
                    .collect();
                nearby.push(&self.my_id);
                nearby.sort(); // tri stable pour consensus entre peers

                // Hash consistant
                let hash = (cx as i64)
                    .wrapping_mul(73856093)
                    .wrapping_add((cz as i64).wrapping_mul(19349663))
                    .unsigned_abs() as usize;
                let owner = nearby[hash % nearby.len()];

                if owner == self.my_id {
                    result.push((cx, cz));
                }
            }
        }
        result
    }
}

fn main() {
    let name = std::env::args()
        .nth(1)
        .unwrap_or_else(|| format!("Joueur_{}", &Uuid::new_v4().to_string()[..4]));
    let my_id = Uuid::new_v4().to_string();

    let start_x = (Uuid::new_v4().as_u128() % 21) as i32 - 10;
    let start_z = (Uuid::new_v4().as_u128() % 21) as i32 - 10;

    println!(
        "[{}] ID: {} | Position initiale: ({}, {})",
        name,
        &my_id[..8],
        start_x,
        start_z
    );

    let mut dist = ChunkDistributor::new(&my_id, start_x, start_z);

    let (mut socket, _) =
        connect(SIGNALING).expect("Impossible de se connecter au serveur de signaling");
    println!("[{}] Connecté au signaling", name);

    let join = serde_json::to_string(&Msg::Join {
        id: my_id.clone(),
        name: name.clone(),
    })
    .unwrap();
    socket.send(Message::Text(join.into())).unwrap();

    let pos = serde_json::to_string(&Msg::Position {
        id: my_id.clone(),
        x: start_x,
        z: start_z,
    })
    .unwrap();
    socket.send(Message::Text(pos.into())).unwrap();

    print_ownership(&name, &dist);

    loop {
        match socket.read() {
            Ok(Message::Text(raw)) => {
                let msg: Result<Msg, _> = serde_json::from_str(&raw);
                match msg {
                    Ok(Msg::PeerList { peers }) => {
                        for p in &peers {
                            dist.peers.insert(p.id.clone(), p.clone());
                        }
                        println!("[{}] {} peer(s) déjà connecté(s)", name, peers.len());
                        print_ownership(&name, &dist);
                    }
                    Ok(Msg::PeerJoined {
                        id,
                        name: pname,
                        x,
                        z,
                    }) => {
                        dist.peers
                            .insert(id.clone(), PeerInfo { id, name: pname.clone(), x, z });
                        println!("[{}] → {} a rejoint", name, pname);
                        print_ownership(&name, &dist);
                    }
                    Ok(Msg::PeerLeft { id }) => {
                        let gone = dist.peers.remove(&id);
                        println!(
                            "[{}] ← {} est parti",
                            name,
                            gone.map(|p| p.name).unwrap_or_default()
                        );
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
    let pct = chunks.len() * 100 / total;
    println!(
        "[{}] Ownership: {}/{} chunks ({}%) | pos ({},{}) | {} peer(s)",
        name,
        chunks.len(),
        total,
        pct,
        d.my_x,
        d.my_z,
        d.peers.len()
    );
}
