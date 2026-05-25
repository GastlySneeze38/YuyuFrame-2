# Minecraft P2P Distributed Server Launcher

## 🎮 Vue d'ensemble

Un launcher Minecraft révolutionnaire qui permet de créer des serveurs où **la charge de calcul est distribuée entre tous les joueurs connectés** via P2P (peer-to-peer). Au lieu d'avoir un serveur central qui calcule tout, chaque joueur calcule uniquement les zones du monde qu'il explore.

### Concept clé

```
Serveur classique:
┌──────────────┐
│   Serveur    │ ← 100% CPU pour calculer tout le monde
│   Central    │
└──────────────┘
       ↓
   4 joueurs

Système P2P distribué:
┌────┐  ┌────┐  ┌────┐  ┌────┐
│ P1 │  │ P2 │  │ P3 │  │ P4 │ ← Chacun 25% CPU
└────┘  └────┘  └────┘  └────┘
  ↓       ↓       ↓       ↓
Zone A  Zone B  Zone C  Zone D
```

### Caractéristiques

✅ **Distribution dynamique de charge** : Plus il y a de joueurs, moins chacun consomme de ressources  
✅ **Calcul à la demande** : Seules les zones actives sont calculées  
✅ **Stockage centralisé** : Les fichiers du monde restent sur un serveur central léger  
✅ **Scalabilité automatique** : Le système s'adapte au nombre de joueurs  
✅ **Économie de ressources** : Zones vides = 0% CPU  

---

## 🏗️ Architecture Technique

### Composants principaux

```
┌─────────────────────────────────────────────────────────┐
│                   LAUNCHER (Client)                      │
│  - Découverte des peers                                  │
│  - Injection de l'agent Java                             │
│  - Configuration P2P                                     │
│  - Lancement de Minecraft                                │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│              AGENT JAVA (Modification Runtime)           │
│  - Injection ASM dans les classes Minecraft              │
│  - Interception des ticks de chunks                      │
│  - Gestion de la distribution de charge                  │
│  - Synchronisation des entités                           │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                  RÉSEAU P2P (WebRTC/Socket)              │
│  - Communication entre peers                             │
│  - Synchronisation d'état                                │
│  - Transfert de chunks                                   │
│  - Broadcast des entités                                 │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│            SERVEUR CENTRAL (Stockage uniquement)         │
│  - Fichiers du monde (.mca, level.dat)                  │
│  - Base de données joueurs                               │
│  - Authentification                                      │
│  - Pas de calcul de jeu                                  │
└─────────────────────────────────────────────────────────┘
```

---

## 🧩 Composants détaillés

### 1. Launcher (Electron/Node.js ou Java)

**Responsabilités :**
- Interface utilisateur pour configuration
- Découverte des peers disponibles (DHT, serveur de signaling)
- Téléchargement de l'agent Java et des dépendances
- Configuration des arguments JVM
- Lancement du client Minecraft avec l'agent injecté

**Technologies suggérées :**
- Electron + React pour l'UI
- Node.js pour la logique launcher
- WebRTC pour la découverte P2P initiale

```javascript
// Exemple de structure
class P2PMinecraftLauncher {
  async launch(config) {
    // 1. Découvrir les peers
    const peers = await this.discoverPeers(config.serverCode);
    
    // 2. Télécharger/vérifier l'agent
    await this.ensureAgent();
    
    // 3. Configurer le réseau P2P
    const p2pConfig = await this.setupP2P(peers);
    
    // 4. Lancer Minecraft
    await this.launchMinecraft({
      javaAgent: './agents/distributed-agent.jar',
      p2pBootstrap: p2pConfig,
      worldServer: config.worldServerUrl
    });
  }
}
```

---

### 2. Agent Java (Core du système)

**Rôle :** Modifier le comportement de Minecraft en temps réel via injection de bytecode

**Classes Minecraft à modifier :**

```java
// Classes critiques à intercepter
net.minecraft.server.level.ServerLevel           // Tick du monde
net.minecraft.server.level.ServerChunkCache      // Gestion des chunks
net.minecraft.world.level.chunk.LevelChunk       // Tick individuel de chunk
net.minecraft.server.level.ChunkMap              // Chargement/déchargement
net.minecraft.world.entity.Entity                // Tick des entités
net.minecraft.world.level.block.entity.BlockEntity // Tick des tile entities
```

**Architecture de l'agent :**

```java
public class DistributedMinecraftAgent {
    
    // Point d'entrée de l'agent
    public static void premain(String args, Instrumentation inst) {
        // Enregistrer les transformateurs de classes
        inst.addTransformer(new ServerLevelTransformer());
        inst.addTransformer(new ChunkTickTransformer());
        inst.addTransformer(new EntityTickTransformer());
        
        // Initialiser le système P2P
        P2PNetwork.initialize(parseArgs(args));
    }
}

// Transformateur pour ServerLevel.tick()
public class ServerLevelTransformer implements ClassFileTransformer {
    
    @Override
    public byte[] transform(ClassLoader loader, String className, 
                          Class<?> classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          byte[] classfileBuffer) {
        
        if (className.equals("net/minecraft/server/level/ServerLevel")) {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            
            // Trouver et modifier la méthode tick()
            for (MethodNode method : classNode.methods) {
                if (method.name.equals("tick")) {
                    injectDistributedLogic(method);
                }
            }
            
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        }
        
        return null; // Pas de modification
    }
    
    private void injectDistributedLogic(MethodNode method) {
        // Injecter au début de la méthode :
        // if (!DistributedChunkManager.shouldTickWorld(this)) return;
        
        InsnList injection = new InsnList();
        injection.add(new VarInsnNode(ALOAD, 0)); // this
        injection.add(new MethodInsnNode(
            INVOKESTATIC,
            "com/p2pminecraft/DistributedChunkManager",
            "shouldTickWorld",
            "(Lnet/minecraft/server/level/ServerLevel;)Z",
            false
        ));
        
        LabelNode continueLabel = new LabelNode();
        injection.add(new JumpInsnNode(IFNE, continueLabel));
        injection.add(new InsnNode(RETURN)); // Skip si false
        injection.add(continueLabel);
        
        method.instructions.insert(injection);
    }
}
```

---

### 3. Gestionnaire de chunks distribués

**Algorithme de distribution intelligente :**

```java
public class DistributedChunkManager {
    
    // Qui calcule quel chunk ?
    private static ConcurrentHashMap<ChunkPos, UUID> chunkOwnership = new ConcurrentHashMap<>();
    
    // Chunks que JE calcule
    private static Set<ChunkPos> myChunks = ConcurrentHashMap.newKeySet();
    
    // Mon UUID de joueur
    private static UUID myPlayerId;
    
    /**
     * Détermine si ce client doit calculer le tick de ce chunk
     * 
     * Règles :
     * - Aucun joueur dans les 10 chunks ? → personne ne calcule
     * - 1 joueur seul ? → il calcule tout autour de lui
     * - Plusieurs joueurs ? → distribution par hash consistant
     */
    public static boolean shouldTickChunk(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        
        // Trouver tous les joueurs dans la zone de rendu
        List<ServerPlayer> nearbyPlayers = findPlayersInRange(pos, VIEW_DISTANCE);
        
        if (nearbyPlayers.isEmpty()) {
            // Personne ici = pas de calcul (économie CPU)
            return false;
        }
        
        if (nearbyPlayers.size() == 1) {
            // Un seul joueur = il calcule tout
            UUID playerId = nearbyPlayers.get(0).getUUID();
            chunkOwnership.put(pos, playerId);
            return playerId.equals(myPlayerId);
        }
        
        // Plusieurs joueurs = distribuer équitablement
        return assignChunkOwner(pos, nearbyPlayers);
    }
    
    /**
     * Algorithme de distribution par hash consistant
     * Garantit que le même chunk est toujours assigné au même joueur
     * (tant que les joueurs ne bougent pas)
     */
    private static boolean assignChunkOwner(ChunkPos pos, List<ServerPlayer> players) {
        // Hash du chunk pour déterminer l'ownership
        long hash = hashChunkPos(pos);
        
        // Sélectionner un joueur basé sur le hash
        int playerIndex = (int) (Math.abs(hash) % players.size());
        UUID assignedPlayer = players.get(playerIndex).getUUID();
        
        // Mettre à jour la table d'ownership
        chunkOwnership.put(pos, assignedPlayer);
        
        // Est-ce que c'est moi qui doit calculer ?
        boolean isMine = assignedPlayer.equals(myPlayerId);
        
        if (isMine) {
            myChunks.add(pos);
        } else {
            myChunks.remove(pos);
        }
        
        return isMine;
    }
    
    /**
     * Hash consistant pour stabilité
     */
    private static long hashChunkPos(ChunkPos pos) {
        // Utiliser MurmurHash ou similaire pour bonne distribution
        long x = pos.x;
        long z = pos.z;
        return x * 31 + z * 17;
    }
    
    /**
     * Trouver les joueurs dans un rayon autour d'un chunk
     */
    private static List<ServerPlayer> findPlayersInRange(ChunkPos center, int radius) {
        List<ServerPlayer> result = new ArrayList<>();
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ChunkPos playerChunk = new ChunkPos(player.blockPosition());
            
            int dx = Math.abs(playerChunk.x - center.x);
            int dz = Math.abs(playerChunk.z - center.z);
            
            if (dx <= radius && dz <= radius) {
                result.add(player);
            }
        }
        
        return result;
    }
}
```

---

### 4. Synchronisation des entités

Les entités (mobs, items, projectiles) doivent être visibles même si calculées par un autre peer.

```java
public class EntitySyncSystem {
    
    // Entités que je calcule (ownership)
    private Set<Integer> myEntities = ConcurrentHashMap.newKeySet();
    
    // Entités "ghost" (calculées ailleurs, juste affichées)
    private Set<Integer> ghostEntities = ConcurrentHashMap.newKeySet();
    
    /**
     * Intercepte le tick d'une entité
     */
    public static boolean shouldTickEntity(Entity entity) {
        int entityId = entity.getId();
        ChunkPos chunkPos = entity.chunkPosition();
        
        // Vérifier qui possède le chunk de l'entité
        UUID chunkOwner = DistributedChunkManager.getChunkOwner(chunkPos);
        
        if (chunkOwner == null) {
            // Chunk non chargé, skip
            return false;
        }
        
        boolean isMine = chunkOwner.equals(myPlayerId);
        
        if (isMine) {
            myEntities.add(entityId);
            ghostEntities.remove(entityId);
            return true; // Je calcule
        } else {
            myEntities.remove(entityId);
            ghostEntities.add(entityId);
            return false; // Un autre calcule
        }
    }
    
    /**
     * Broadcast mes entités aux autres peers
     * Appelé ~20 fois par seconde
     */
    public void broadcastMyEntities() {
        List<EntityUpdate> updates = new ArrayList<>();
        
        for (int entityId : myEntities) {
            Entity entity = getEntityById(entityId);
            if (entity == null) continue;
            
            EntityUpdate update = new EntityUpdate();
            update.entityId = entityId;
            update.type = entity.getType().toString();
            update.x = entity.getX();
            update.y = entity.getY();
            update.z = entity.getZ();
            update.vx = entity.getDeltaMovement().x;
            update.vy = entity.getDeltaMovement().y;
            update.vz = entity.getDeltaMovement().z;
            update.yaw = entity.getYRot();
            update.pitch = entity.getXRot();
            
            updates.add(update);
        }
        
        // Envoyer via P2P aux joueurs proches
        P2PNetwork.broadcast(updates, getNearbyPeers());
    }
    
    /**
     * Recevoir les updates d'entités des autres peers
     */
    public void receiveEntityUpdate(EntityUpdate update) {
        if (ghostEntities.contains(update.entityId)) {
            // Mettre à jour l'entité ghost
            Entity entity = getEntityById(update.entityId);
            
            if (entity == null) {
                // Créer l'entité en mode ghost
                entity = createGhostEntity(update);
            }
            
            // Appliquer la position
            entity.setPos(update.x, update.y, update.z);
            entity.setDeltaMovement(update.vx, update.vy, update.vz);
            entity.setYRot(update.yaw);
            entity.setXRot(update.pitch);
            
            // Important : désactiver l'AI et la physique
            entity.setNoAi(true);
            entity.noPhysics = true;
        }
    }
}

// Structure de message
class EntityUpdate {
    int entityId;
    String type;
    double x, y, z;
    double vx, vy, vz;
    float yaw, pitch;
    long timestamp;
}
```

---

### 5. Réseau P2P

**Protocole de communication :**

```java
public class P2PNetwork {
    
    // Connexions aux autres peers
    private Map<UUID, PeerConnection> peers = new ConcurrentHashMap<>();
    
    // Types de messages
    enum MessageType {
        CHUNK_OWNERSHIP,      // Claim d'ownership de chunks
        ENTITY_UPDATE,        // État d'entités
        BLOCK_CHANGE,         // Changement de bloc
        PLAYER_POSITION,      // Position du joueur
        CHUNK_REQUEST,        // Demande de données chunk
        CHUNK_DATA,           // Envoi de données chunk
        HANDSHAKE,            // Connexion initiale
        HEARTBEAT             // Keep-alive
    }
    
    /**
     * Initialisation du réseau P2P
     */
    public static void initialize(P2PConfig config) {
        // Se connecter au serveur de signaling
        SignalingServer signaling = new SignalingServer(config.signalingUrl);
        
        // Obtenir la liste des peers
        List<PeerInfo> peers = signaling.discoverPeers(config.serverId);
        
        // Établir connexions WebRTC ou TCP
        for (PeerInfo peer : peers) {
            connectToPeer(peer);
        }
        
        // Lancer le thread de synchronisation
        startSyncThread();
    }
    
    /**
     * Thread de synchronisation (20 ticks/sec)
     */
    private static void startSyncThread() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        
        executor.scheduleAtFixedRate(() -> {
            // Broadcast ownership de chunks
            broadcastChunkOwnership();
            
            // Broadcast entités
            EntitySyncSystem.broadcastMyEntities();
            
            // Heartbeat
            sendHeartbeat();
            
        }, 0, 50, TimeUnit.MILLISECONDS); // 20 Hz
    }
    
    /**
     * Envoyer un message à tous les peers proches
     */
    public static void broadcast(Object message, List<UUID> targetPeers) {
        byte[] serialized = serialize(message);
        
        for (UUID peerId : targetPeers) {
            PeerConnection peer = peers.get(peerId);
            if (peer != null && peer.isConnected()) {
                peer.send(serialized);
            }
        }
    }
    
    /**
     * Sérialisation binaire efficace
     */
    private static byte[] serialize(Object obj) {
        // Utiliser Protocol Buffers ou MessagePack pour efficacité
        // Exemple avec MessagePack :
        return MessagePack.pack(obj);
    }
}

// Configuration P2P
class P2PConfig {
    String signalingUrl;     // Serveur de découverte
    String serverId;         // ID du serveur à rejoindre
    int maxPeers;            // Limite de connexions P2P
    boolean useWebRTC;       // WebRTC vs TCP/UDP
}

// Info sur un peer
class PeerInfo {
    UUID playerId;
    String address;
    int port;
    String webrtcOffer;  // Si WebRTC
}
```

---

### 6. Load Balancer (Rééquilibrage dynamique)

```java
public class LoadBalancer {
    
    /**
     * Rééquilibrer la charge toutes les 5 secondes
     */
    public void rebalance() {
        // Calculer la charge de chaque peer
        Map<UUID, Integer> peerLoads = new HashMap<>();
        
        for (UUID peerId : connectedPeers) {
            int chunkCount = countChunksForPeer(peerId);
            peerLoads.put(peerId, chunkCount);
        }
        
        // Trouver le plus chargé et le moins chargé
        UUID overloaded = Collections.max(peerLoads.entrySet(), 
            Map.Entry.comparingByValue()).getKey();
        UUID underloaded = Collections.min(peerLoads.entrySet(), 
            Map.Entry.comparingByValue()).getKey();
        
        int maxLoad = peerLoads.get(overloaded);
        int minLoad = peerLoads.get(underloaded);
        
        // Si déséquilibre > 30%, redistribuer
        float imbalance = (float)(maxLoad - minLoad) / maxLoad;
        
        if (imbalance > 0.3f) {
            // Transférer ~25% des chunks du plus chargé vers le moins chargé
            int toTransfer = (maxLoad - minLoad) / 4;
            transferChunks(overloaded, underloaded, toTransfer);
        }
    }
    
    private void transferChunks(UUID from, UUID to, int count) {
        // Trouver les chunks transférables (pas de joueurs à proximité)
        List<ChunkPos> transferable = findTransferableChunks(from, count);
        
        // Envoyer message de transfert
        ChunkTransferMessage msg = new ChunkTransferMessage();
        msg.fromPeer = from;
        msg.toPeer = to;
        msg.chunks = transferable;
        
        P2PNetwork.send(msg, to);
        P2PNetwork.send(msg, from);
    }
}
```

---

## 📡 Serveur central (Stockage uniquement)

Le serveur central ne calcule RIEN, il sert uniquement de :

**1. Dépôt de fichiers**
```
/worlds/
  /survival-world/
    /region/        ← Fichiers .mca (terrain)
    /playerdata/    ← Inventaires, stats
    /data/          ← Villages, structures
    level.dat       ← Seed, règles
```

**2. Base de données**
```sql
-- Authentification et métadonnées
CREATE TABLE players (
    uuid VARCHAR(36) PRIMARY KEY,
    username VARCHAR(16),
    last_position_x DOUBLE,
    last_position_y DOUBLE,
    last_position_z DOUBLE,
    last_seen TIMESTAMP
);

CREATE TABLE servers (
    server_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100),
    created_at TIMESTAMP,
    max_players INT
);
```

**3. API REST simple**
```javascript
// Endpoints
GET  /worlds/:worldId/region/:x/:z     // Télécharger un fichier région
POST /worlds/:worldId/region/:x/:z     // Upload une région modifiée
GET  /players/:uuid                     // Données joueur
POST /players/:uuid                     // Sauvegarder données joueur
GET  /servers/:serverId/peers          // Liste des peers connectés
POST /servers/:serverId/join           // Rejoindre un serveur
```

**Implémentation légère (Node.js/Express) :**

```javascript
const express = require('express');
const app = express();

// Télécharger une région
app.get('/worlds/:worldId/region/:x/:z', (req, res) => {
    const { worldId, x, z } = req.params;
    const regionFile = `./worlds/${worldId}/region/r.${x}.${z}.mca`;
    
    if (fs.existsSync(regionFile)) {
        res.sendFile(regionFile);
    } else {
        res.status(404).send('Region not found');
    }
});

// Upload d'une région (quand un peer sauvegarde)
app.post('/worlds/:worldId/region/:x/:z', upload.single('region'), (req, res) => {
    const { worldId, x, z } = req.params;
    const regionFile = `./worlds/${worldId}/region/r.${x}.${z}.mca`;
    
    fs.writeFileSync(regionFile, req.file.buffer);
    res.json({ success: true });
});

app.listen(3000);
```

---

## 🚀 Stack technologique

### Launcher
- **UI** : Electron + React + TypeScript
- **Backend** : Node.js
- **P2P Discovery** : WebRTC + Socket.io pour signaling

### Agent Java
- **Language** : Java 17+
- **Bytecode Manipulation** : ASM 9.x
- **Build** : Gradle

### Réseau P2P
- **WebRTC** : PeerJS ou simple-peer
- **Fallback TCP** : Socket Java natif
- **Sérialisation** : MessagePack ou Protocol Buffers

### Serveur central
- **Backend** : Node.js + Express ou Go
- **Base de données** : PostgreSQL ou SQLite
- **Stockage** : Système de fichiers local ou S3

---

## 📊 Performance attendue

### Scénarios

**1 joueur seul :**
```
- Calcule tous les chunks autour de lui (radius 10)
- ~100 chunks actifs
- CPU : 60-80% d'un cœur
```

**2 joueurs éloignés :**
```
- Chacun calcule sa zone
- ~100 chunks chacun
- CPU : 50% chacun (total 100%, mais distribué)
```

**4 joueurs dispersés :**
```
- Chaque joueur : ~100 chunks
- CPU : 25-30% par joueur
- Total : 100-120% distribué sur 4 machines
```

**10 joueurs dans différentes zones :**
```
- Chaque joueur : ~40 chunks en moyenne
- CPU : 10-15% par joueur
- Scalabilité quasi-linéaire
```

### Limitations

⚠️ **Zones partagées** : Si tous les joueurs sont au même endroit, le bénéfice P2P diminue  
⚠️ **Latence réseau** : Sync d'entités nécessite ~50ms max entre peers  
⚠️ **Bande passante** : ~1-2 Mbps par joueur pour la synchronisation  

---

## 🗺️ Roadmap de développement

### Phase 1 : Prototype (2-3 mois)
- [ ] Launcher basique avec injection d'agent
- [ ] Agent Java avec interception de ServerLevel.tick()
- [ ] Système de chunks distribués (algorithme de base)
- [ ] Communication P2P simple (TCP/IP)
- [ ] Test avec 2 clients

### Phase 2 : Synchronisation (2-3 mois)
- [ ] Système d'entités "ghost"
- [ ] Broadcast des entités entre peers
- [ ] Gestion des changements de blocs
- [ ] Protocole de messages optimisé
- [ ] Test avec 4-5 clients

### Phase 3 : Optimisation (2 mois)
- [ ] Load balancer automatique
- [ ] WebRTC pour réduire latence
- [ ] Compression des messages
- [ ] Détection de déconnexion et migration
- [ ] Test avec 10+ clients

### Phase 4 : Production (2 mois)
- [ ] Interface utilisateur complète
- [ ] Serveur central robuste
- [ ] Système de mods/plugins compatible
- [ ] Documentation complète
- [ ] Tests de stress

### Phase 5 : Fonctionnalités avancées
- [ ] Support multi-mondes (Nether, End)
- [ ] Sauvegarde distribuée
- [ ] Anti-cheat basique
- [ ] Système de backup automatique
- [ ] Métriques de performance en temps réel

---

## 🔧 Structure du projet

```
P2P-Server/
├── server-agent/                      # Agent Java
│   ├── src/main/java/
│   │   ├── agent/
│   │   │   ├── DistributedMinecraftAgent.java
│   │   │   ├── transformers/
│   │   │   │   ├── ServerLevelTransformer.java
│   │   │   │   ├── ChunkTickTransformer.java
│   │   │   │   └── EntityTickTransformer.java
│   │   │   └── injection/
│   │   │       └── ASMUtils.java
│   │   ├── distributed/
│   │   │   ├── DistributedChunkManager.java
│   │   │   ├── EntitySyncSystem.java
│   │   │   ├── LoadBalancer.java
│   │   │   └── ChunkOwnership.java
│   │   ├── network/
│   │   │   ├── P2PNetwork.java
│   │   │   ├── PeerConnection.java
│   │   │   ├── MessageProtocol.java
│   │   │   └── SignalingClient.java
│   │   └── storage/
│   │       ├── WorldStorageClient.java
│   │       └── RegionFileSync.java
│   └── build.gradle
│
├── server/                     # Serveur central (stockage)
│   ├── src/
│   │   ├── api/               # API REST
│   │   ├── storage/           # Gestion fichiers
│   │   ├── database/          # PostgreSQL
│   │   └── signaling/         # Serveur de signaling P2P
│   └── package.json
│
├── protocol/                   # Définitions protocole
│   ├── messages.proto         # Protocol Buffers
│   └── README.md
│
├── docs/                       # Documentation
│   ├── architecture.md
│   ├── protocol.md
│   ├── deployment.md
│   └── contributing.md
│
└── README.md                   # Ce fichier
```

---

## 🛠️ Installation et développement

### Prérequis
- Java 17+
- Node.js 18+
- Gradle 7+
- Minecraft 1.20.x (compatible)

### Build de l'agent

```bash
cd agent
gradle shadowJar
# Génère : agent/build/libs/distributed-agent.jar
```

### Build du launcher

```bash
cd launcher
npm install
npm run build
# Génère : launcher/dist/
```

### Lancer en développement

```bash
# Terminal 1 : Serveur central
cd server
npm install
npm run dev

# Terminal 2 : Launcher (client 1)
cd launcher
npm run dev

# Terminal 3 : Launcher (client 2)
cd launcher
npm run dev
```

---

## 📝 Configuration

### Fichier de configuration launcher

```json
{
  "launcher": {
    "minecraftVersion": "1.20.4",
    "javaPath": "/usr/bin/java",
    "jvmArgs": ["-Xmx4G", "-Xms2G"]
  },
  "p2p": {
    "signalingServer": "wss://signal.myserver.com",
    "useWebRTC": true,
    "maxPeers": 20,
    "port": 25565
  },
  "world": {
    "storageServer": "https://storage.myserver.com",
    "autoSync": true,
    "syncInterval": 300
  }
}
```

---

## 🤝 Contribution

Ce projet est en développement actif. Les contributions sont bienvenues !

### Domaines qui nécessitent de l'aide :
- Optimisation réseau (réduction de latence)
- Gestion de la déconnexion/reconnexion
- Interface utilisateur du launcher
- Tests de charge et benchmarks
- Documentation

---

## ⚖️ Licence

À définir (MIT suggérée)

---

## 📞 Contact

- GitHub Issues pour les bugs
- Discussions pour les questions générales
- Discord : [Lien à créer]

---

## 🎯 Objectifs à long terme

1. **Scalabilité** : Supporter 50+ joueurs simultanés
2. **Stabilité** : 99%+ uptime avec récupération automatique
3. **Performance** : <50ms de latence pour sync entités
4. **Compatibilité** : Support des mods populaires (Forge, Fabric)
5. **Économie** : Réduire les coûts d'hébergement de 70-90%

---

**Version** : 0.1.0-alpha  
**Dernière mise à jour** : Mai 2026