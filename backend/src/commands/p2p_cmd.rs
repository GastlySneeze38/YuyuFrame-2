use crate::minecraft::p2p;

/// Démarre le nœud libp2p et retourne le code de session (PeerID ~52 chars).
/// Idempotent : retourne le même code si déjà démarré.
#[tauri::command]
pub async fn p2p_start() -> Result<String, String> {
    p2p::start_libp2p().await.map_err(|e| e.to_string())
}

/// Rejoint une session P2P en entrant le code de l'hôte.
/// Lance la connexion via les relay nodes publics + tente le hole punch.
#[tauri::command]
pub async fn p2p_join(peer_id: String) -> Result<(), String> {
    p2p::join_libp2p(peer_id).await.map_err(|e| e.to_string())
}
