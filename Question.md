Aucun blocage pour finir la couche 1. Voici l'état exact :

Ce qui existe déjà :

Signaling WebSocket fonctionnel dans le backend Tauri (p2p.rs)
Java agent qui se connecte à ws://127.0.0.1:8765 — cette partie ne change pas
rust-core avec la logique ownership en Rust déjà prête
Ce qui manque pour compléter la couche 1 :

Le signaling actuel tourne uniquement sur 127.0.0.1 — les deux instances Minecraft doivent être sur la même machine. Pour jouer entre deux PC différents, il faut :

Dans le backend Tauri — remplacer le relay TCP simple par rust-libp2p avec :

Transport Noise + Yamux (chiffrement + multiplexage)
Protocole identify (découverte de ton IP publique)
Circuit relay (fallback si hole punch échoue)
dcutr (hole punching direct)
Un relay server déployé quelque part sur Internet (fly.io gratuit suffit, ou utiliser les relay publics libp2p de Protocol Labs)

Le Java agent ne change pas — il continue de parler WebSocket à localhost:8765. Le backend Tauri fait le pont WebSocket local ↔ libp2p internet.


[Java Agent] ←ws→ [Tauri Backend] ←libp2p→ [Tauri Backend distant] ←ws→ [Java Agent distant]

Donc : pas de blocage, effort modéré. La crate rust-libp2p couvre exactement ce cas d'usage et a des exemples complets pour hole punching + relay. On commence ?

Les relay nodes publics de Protocol Labs (utilisés par IPFS) fonctionnent comme relay libp2p. Tu peux les utiliser sans rien déployer le temps de développer. Une fois ton serveur prêt, tu bascules sur ton propre relay avec un simple changement de config (une adresse).