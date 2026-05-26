use std::collections::HashMap;
use std::sync::{Mutex, OnceLock};

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

pub const VIEW_DISTANCE: i32 = 3;

// --- Données partagées ---

#[derive(Clone)]
pub struct PeerInfo {
    pub id: String,
    pub x: i32,
    pub z: i32,
}

pub struct ChunkDistributor {
    pub my_id: String,
    pub my_x: i32,
    pub my_z: i32,
    pub peers: HashMap<String, PeerInfo>,
}

impl ChunkDistributor {
    pub fn new(my_id: &str, x: i32, z: i32) -> Self {
        Self {
            my_id: my_id.to_string(),
            my_x: x,
            my_z: z,
            peers: HashMap::new(),
        }
    }

    pub fn should_tick_chunk(&self, cx: i32, cz: i32) -> bool {
        let mut nearby: Vec<&str> = self
            .peers
            .values()
            .filter(|p| {
                (p.x - cx).abs() <= VIEW_DISTANCE && (p.z - cz).abs() <= VIEW_DISTANCE
            })
            .map(|p| p.id.as_str())
            .collect();
        nearby.push(&self.my_id);
        nearby.sort();

        let hash = (cx as i64)
            .wrapping_mul(73856093)
            .wrapping_add((cz as i64).wrapping_mul(19349663))
            .unsigned_abs() as usize;

        nearby[hash % nearby.len()] == self.my_id
    }

    pub fn my_chunks(&self) -> Vec<(i32, i32)> {
        let mut result = Vec::new();
        for cx in (self.my_x - VIEW_DISTANCE)..=(self.my_x + VIEW_DISTANCE) {
            for cz in (self.my_z - VIEW_DISTANCE)..=(self.my_z + VIEW_DISTANCE) {
                if self.should_tick_chunk(cx, cz) {
                    result.push((cx, cz));
                }
            }
        }
        result
    }
}

// --- État global (thread-safe) ---

static STATE: OnceLock<Mutex<ChunkDistributor>> = OnceLock::new();

fn get_state() -> &'static Mutex<ChunkDistributor> {
    STATE.get_or_init(|| Mutex::new(ChunkDistributor::new("default", 0, 0)))
}

// --- Exports JNI pour com.p2pminecraft.runtime.RustBridge ---

/// Initialiser avec l'ID du peer et sa position de départ
#[no_mangle]
pub extern "system" fn Java_com_p2pminecraft_runtime_RustBridge_init(
    mut env: JNIEnv,
    _class: JClass,
    peer_id: JString,
    x: jint,
    z: jint,
) {
    let id: String = env.get_string(&peer_id).unwrap().into();
    let mut dist = get_state().lock().unwrap();
    dist.my_id = id;
    dist.my_x = x;
    dist.my_z = z;
    dist.peers.clear();
}

/// Est-ce que ce peer doit calculer le tick de ce chunk ?
#[no_mangle]
pub extern "system" fn Java_com_p2pminecraft_runtime_RustBridge_shouldTickChunk(
    _env: JNIEnv,
    _class: JClass,
    cx: jint,
    cz: jint,
) -> jboolean {
    let dist = get_state().lock().unwrap();
    if dist.should_tick_chunk(cx, cz) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// Mettre à jour la position de ce peer
#[no_mangle]
pub extern "system" fn Java_com_p2pminecraft_runtime_RustBridge_setMyPosition(
    _env: JNIEnv,
    _class: JClass,
    cx: jint,
    cz: jint,
) {
    let mut dist = get_state().lock().unwrap();
    dist.my_x = cx;
    dist.my_z = cz;
}

/// Ajouter ou mettre à jour un peer distant
#[no_mangle]
pub extern "system" fn Java_com_p2pminecraft_runtime_RustBridge_upsertPeer(
    mut env: JNIEnv,
    _class: JClass,
    peer_id: JString,
    x: jint,
    z: jint,
) {
    let id: String = env.get_string(&peer_id).unwrap().into();
    let mut dist = get_state().lock().unwrap();
    dist.peers.insert(id.clone(), PeerInfo { id, x, z });
}

/// Supprimer un peer (déconnexion)
#[no_mangle]
pub extern "system" fn Java_com_p2pminecraft_runtime_RustBridge_removePeer(
    mut env: JNIEnv,
    _class: JClass,
    peer_id: JString,
) {
    let id: String = env.get_string(&peer_id).unwrap().into();
    get_state().lock().unwrap().peers.remove(&id);
}

/// Nombre de chunks que ce peer calcule (utile pour debug/monitoring)
#[no_mangle]
pub extern "system" fn Java_com_p2pminecraft_runtime_RustBridge_myChunkCount(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    get_state().lock().unwrap().my_chunks().len() as jint
}
