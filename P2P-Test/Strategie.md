# Stratégie d'Attaque : Minecraft P2P Distribué
## Modification Directe des Classes Serveur & Client

---

## 🎯 Vision Globale

Au lieu d'utiliser un proxy (BungeeCord), on crée un **système hybride client-serveur** où :

- Chaque joueur lance un **serveur Minecraft intégré** localement
- Le client Minecraft se connecte à son propre serveur local
- Les serveurs locaux communiquent en P2P entre eux
- Le client affiche les entités calculées par d'autres peers

```
Architecture classique:
Client → Serveur distant → Calcule tout

Notre architecture:
Client ←→ Serveur Local (ce peer) ←→ P2P ←→ Autres Serveurs Locaux
  ↓           ↓                              ↓
Affichage  Calcul partiel               Calcul partiel
```

---

## 📦 Phases de Développement

### **Phase 0 : Préparation (1 semaine)**
- Décompiler Minecraft avec des mappings propres
- Setup environnement de développement
- Créer le système de build qui repackage les classes modifiées

### **Phase 1 : POC Minimal (2-3 semaines)**
- Lancer serveur intégré depuis le client
- Communication P2P basique entre 2 instances
- Preuve de concept : 1 chunk calculé localement, 1 chunk reçu d'un peer

### **Phase 2 : Distribution des Chunks (3-4 semaines)**
- Implémentation complète du gestionnaire de chunks distribués
- Synchronisation des blocs entre peers
- Gestion des frontières entre zones

### **Phase 3 : Entités Distribuées (3-4 semaines)**
- Système d'entités "ghost"
- Synchronisation position/mouvement/état
- Interpolation pour fluidité

### **Phase 4 : Optimisation (2-3 semaines)**
- Load balancing automatique
- Compression des messages
- Réduction de la latence

### **Phase 5 : Production (2-3 semaines)**
- Interface launcher
- Gestion des erreurs robuste
- Documentation

**Total estimé : 3-4 mois de développement**

---

## 🔧 Phase 1 : POC Minimal - Plan d'Action Détaillé

### Objectif
Avoir 2 clients Minecraft qui partagent le calcul d'un monde simple.

### Étape 1.1 : Setup Environnement (2 jours)

```bash
# Structure du projet
minecraft-p2p/
├── decompiled/              # Minecraft décompilé
│   ├── client/
│   └── server/
├── patches/                 # Nos modifications
│   ├── server/
│   │   ├── ServerLevel.patch
│   │   ├── ServerChunkCache.patch
│   │   └── MinecraftServer.patch
│   └── client/
│       ├── Minecraft.patch
│       └── ClientLevel.patch
├── p2p-core/               # Notre code P2P
│   ├── network/
│   ├── distributed/
│   └── sync/
└── build/                  # Système de build
```

**Outils nécessaires :**
- MCP (Mod Coder Pack) ou Fabric Loom pour mappings
- Gradle avec plugin de patching
- Git pour versioning des patches

### Étape 1.2 : Lancer Serveur Intégré (3 jours)

**Classe à modifier : `net.minecraft.client.Minecraft`**

```java
// Patch à appliquer dans Minecraft.java

public class Minecraft implements Runnable {
    
    // AJOUT : Notre serveur intégré
    private IntegratedServerP2P integratedServerP2P;
    
    // MODIFICATION : Dans le constructeur ou au lancement
    public void startGame() {
        // ... code existant ...
        
        // NOUVEAU : Lancer notre serveur intégré P2P
        if (P2PConfig.isEnabled()) {
            this.integratedServerP2P = new IntegratedServerP2P(this);
            this.integratedServerP2P.start();
            
            // Se connecter à notre propre serveur
            this.connect("localhost", integratedServerP2P.getPort());
        }
    }
}
```

**Nouvelle classe : `IntegratedServerP2P`**

```java
package net.minecraft.p2p;

public class IntegratedServerP2P extends MinecraftServer {
    
    private final Minecraft minecraft;
    private P2PNetwork network;
    
    public IntegratedServerP2P(Minecraft minecraft) {
        super(/* args */);
        this.minecraft = minecraft;
    }
    
    @Override
    public boolean initServer() {
        // Init serveur normal
        boolean success = super.initServer();
        
        if (success) {
            // Init réseau P2P
            this.network = new P2PNetwork(this);
            this.network.initialize();
        }
        
        return success;
    }
    
    // Exposer le serveur en mode "headless"
    @Override
    public boolean isDedicatedServer() {
        return false; // Mode intégré
    }
}
```

### Étape 1.3 : Communication P2P Basique (4 jours)

**Nouvelle classe : `P2PNetwork`**

```java
package net.minecraft.p2p.network;

public class P2PNetwork {
    
    private MinecraftServer server;
    private Map<UUID, PeerConnection> peers;
    private ServerSocket listenerSocket;
    
    public void initialize() {
        // 1. Démarrer listener sur port aléatoire
        startListener();
        
        // 2. Se connecter au serveur de découverte
        discoverPeers();
        
        // 3. Établir connexions P2P
        connectToPeers();
        
        // 4. Lancer thread de sync
        startSyncThread();
    }
    
    private void startListener() {
        try {
            listenerSocket = new ServerSocket(0); // Port auto
            int port = listenerSocket.getLocalPort();
            
            // Thread pour accepter les connexions
            new Thread(() -> {
                while (!listenerSocket.isClosed()) {
                    try {
                        Socket socket = listenerSocket.accept();
                        handleNewPeer(socket);
                    } catch (IOException e) {
                        // Handle
                    }
                }
            }).start();
            
        } catch (IOException e) {
            throw new RuntimeException("Cannot start P2P listener", e);
        }
    }
    
    private void discoverPeers() {
        // Contacter le serveur central pour obtenir la liste des peers
        try {
            URL url = new URL("http://central-server.com/api/peers?world=myworld");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            // Parse JSON response
            // [ { "playerId": "uuid", "address": "ip:port" }, ... ]
            
            List<PeerInfo> peerList = parsePeerList(conn.getInputStream());
            
            for (PeerInfo peer : peerList) {
                connectToPeer(peer);
            }
            
        } catch (IOException e) {
            // Handle
        }
    }
    
    private void connectToPeer(PeerInfo peer) {
        try {
            Socket socket = new Socket(peer.address, peer.port);
            PeerConnection connection = new PeerConnection(socket, peer.playerId);
            peers.put(peer.playerId, connection);
            
            // Handshake
            connection.sendHandshake(server.getServerUUID());
            
        } catch (IOException e) {
            // Handle
        }
    }
    
    // Envoyer un message à tous les peers
    public void broadcast(P2PMessage message) {
        byte[] data = message.serialize();
        
        for (PeerConnection peer : peers.values()) {
            peer.send(data);
        }
    }
    
    // Thread de synchronisation (20 Hz)
    private void startSyncThread() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        
        executor.scheduleAtFixedRate(() -> {
            // Sync des données
            syncChunkOwnership();
            syncEntities();
            
        }, 0, 50, TimeUnit.MILLISECONDS);
    }
}
```

### Étape 1.4 : Distribution Basique de Chunks (5 jours)

**Classe à modifier : `net.minecraft.server.level.ServerLevel`**

```java
// Patch pour ServerLevel.java

public class ServerLevel extends Level implements WorldGenLevel {
    
    // AJOUT : Notre gestionnaire distribué
    private static DistributedChunkManager distributedManager;
    
    // MODIFICATION : Dans la méthode tick()
    public void tick(BooleanSupplier hasTimeLeft) {
        
        // NOUVEAU : Check si on doit ticker ce monde
        if (distributedManager != null && 
            !distributedManager.shouldTickWorld(this)) {
            // Un autre peer s'occupe de ce monde
            return;
        }
        
        // ... code existant pour le tick ...
    }
}
```

**Classe à modifier : `net.minecraft.world.level.chunk.LevelChunk`**

```java
// Patch pour LevelChunk.java

public class LevelChunk implements ChunkAccess {
    
    // MODIFICATION : Dans la méthode tick()
    public void tick(BooleanSupplier hasTimeLeft) {
        
        // NOUVEAU : Vérifier si on doit calculer ce chunk
        if (!DistributedChunkManager.shouldTickChunk(this)) {
            // Skip - calculé par un autre peer
            return;
        }
        
        // ... code existant pour le tick ...
        
        // NOUVEAU : Après le tick, broadcaster les changements
        DistributedChunkManager.afterChunkTick(this);
    }
}
```

**Nouvelle classe : `DistributedChunkManager`**

```java
package net.minecraft.p2p.distributed;

public class DistributedChunkManager {
    
    // Table d'ownership : quel peer possède quel chunk
    private static ConcurrentHashMap<ChunkPos, UUID> ownership = new ConcurrentHashMap<>();
    
    // Mon ID
    private static UUID myPeerId;
    
    // Réseau P2P
    private static P2PNetwork network;
    
    public static void initialize(UUID peerId, P2PNetwork net) {
        myPeerId = peerId;
        network = net;
    }
    
    /**
     * POC Simple : Division alternée
     * Chunk (0,0) = Peer 0
     * Chunk (1,0) = Peer 1
     * Chunk (0,1) = Peer 0
     * etc.
     */
    public static boolean shouldTickChunk(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        
        // Trouver tous les peers actifs
        List<UUID> activePeers = network.getActivePeers();
        
        if (activePeers.isEmpty()) {
            return true; // Je suis seul
        }
        
        // Hash du chunk pour assignment
        int hash = Math.abs((pos.x * 31 + pos.z * 17));
        int peerIndex = hash % activePeers.size();
        
        UUID assignedPeer = activePeers.get(peerIndex);
        
        // Mettre à jour ownership
        ownership.put(pos, assignedPeer);
        
        // Est-ce moi ?
        return assignedPeer.equals(myPeerId);
    }
    
    /**
     * Après le tick, broadcaster les changements
     */
    public static void afterChunkTick(LevelChunk chunk) {
        // Créer message avec les changements
        ChunkUpdateMessage msg = new ChunkUpdateMessage();
        msg.chunkPos = chunk.getPos();
        msg.changes = collectChanges(chunk);
        
        // Envoyer aux peers qui ont des joueurs proches
        network.sendToNearbyPeers(msg, chunk.getPos());
    }
    
    private static List<BlockChange> collectChanges(LevelChunk chunk) {
        // TODO : Tracker les blocs qui ont changé depuis le dernier tick
        // Pour POC, on peut envoyer tous les blockstates
        return Collections.emptyList();
    }
}
```

### Étape 1.5 : Recevoir et Appliquer les Chunks Distants (4 jours)

**Classe à modifier (CLIENT) : `net.minecraft.client.multiplayer.ClientLevel`**

```java
// Patch pour ClientLevel.java (côté client)

public class ClientLevel extends Level {
    
    // AJOUT : Recevoir les updates de chunks distants
    public void handleRemoteChunkUpdate(ChunkUpdateMessage msg) {
        LevelChunk chunk = this.getChunk(msg.chunkPos.x, msg.chunkPos.z);
        
        if (chunk == null) {
            // Chunk pas chargé localement, ignorer
            return;
        }
        
        // Appliquer les changements de blocs
        for (BlockChange change : msg.changes) {
            chunk.setBlockState(
                change.pos, 
                change.newState, 
                false  // No update (déjà calculé ailleurs)
            );
        }
    }
}
```

**Handler de messages réseau :**

```java
package net.minecraft.p2p.network;

public class MessageHandler {
    
    private ClientLevel clientLevel;
    private ServerLevel serverLevel;
    
    public void handleMessage(P2PMessage message) {
        switch (message.getType()) {
            
            case CHUNK_UPDATE:
                ChunkUpdateMessage chunkMsg = (ChunkUpdateMessage) message;
                
                // Appliquer côté client pour affichage
                if (clientLevel != null) {
                    clientLevel.handleRemoteChunkUpdate(chunkMsg);
                }
                break;
                
            case ENTITY_UPDATE:
                EntityUpdateMessage entityMsg = (EntityUpdateMessage) message;
                handleEntityUpdate(entityMsg);
                break;
                
            // ... autres types ...
        }
    }
}
```

---

## 🧪 Test du POC Phase 1

### Setup de test

```bash
# Instance 1
java -jar minecraft-p2p.jar \
  --peer-id=peer-1 \
  --world=test-world \
  --central-server=http://localhost:3000

# Instance 2
java -jar minecraft-p2p.jar \
  --peer-id=peer-2 \
  --world=test-world \
  --central-server=http://localhost:3000
```

### Critères de succès POC Phase 1

✅ Les 2 instances se trouvent et se connectent en P2P  
✅ Chaque instance calcule ~50% des chunks  
✅ Les blocs placés par peer-1 apparaissent chez peer-2  
✅ Les joueurs se voient mutuellement  
✅ FPS stable (pas de dégradation)  

---

## 📋 Phase 2 : Distribution Complète des Chunks

### Étape 2.1 : Algorithme de Distribution Intelligent

Au lieu de la division alternée simple, implémenter :

```java
public class SmartChunkDistribution {
    
    /**
     * Distribution basée sur la proximité des joueurs
     */
    public static UUID assignChunkOwner(ChunkPos pos, List<ServerPlayer> allPlayers) {
        
        // Trouver les joueurs dans un rayon de 10 chunks
        List<ServerPlayer> nearbyPlayers = new ArrayList<>();
        
        for (ServerPlayer player : allPlayers) {
            ChunkPos playerChunk = new ChunkPos(player.blockPosition());
            double distance = pos.distance(playerChunk);
            
            if (distance <= 10) {
                nearbyPlayers.add(player);
            }
        }
        
        if (nearbyPlayers.isEmpty()) {
            // Personne proche = pas de calcul
            return null;
        }
        
        if (nearbyPlayers.size() == 1) {
            // Un seul joueur = il calcule
            return nearbyPlayers.get(0).getUUID();
        }
        
        // Plusieurs joueurs = hash consistant
        int hash = Math.abs(pos.hashCode());
        int index = hash % nearbyPlayers.size();
        return nearbyPlayers.get(index).getUUID();
    }
}
```

### Étape 2.2 : Gestion des Frontières

Problème : Un chunk à la frontière peut avoir des interactions avec les chunks voisins.

**Solution : Zone tampon de synchronisation**

```java
public class ChunkBorderSync {
    
    /**
     * Pour chaque chunk que je calcule, synchroniser les 8 voisins
     */
    public static void syncBorders(LevelChunk myChunk) {
        ChunkPos pos = myChunk.getPos();
        
        // Les 8 chunks voisins
        ChunkPos[] neighbors = {
            new ChunkPos(pos.x - 1, pos.z - 1),
            new ChunkPos(pos.x - 1, pos.z),
            new ChunkPos(pos.x - 1, pos.z + 1),
            new ChunkPos(pos.x, pos.z - 1),
            new ChunkPos(pos.x, pos.z + 1),
            new ChunkPos(pos.x + 1, pos.z - 1),
            new ChunkPos(pos.x + 1, pos.z),
            new ChunkPos(pos.x + 1, pos.z + 1)
        };
        
        for (ChunkPos neighbor : neighbors) {
            UUID owner = DistributedChunkManager.getOwner(neighbor);
            
            if (owner != null && !owner.equals(myPeerId)) {
                // Envoyer l'état de ma bordure à ce peer
                sendBorderState(myChunk, neighbor, owner);
            }
        }
    }
    
    private static void sendBorderState(LevelChunk myChunk, 
                                       ChunkPos neighborPos, 
                                       UUID neighborOwner) {
        
        // Extraire les blocs de bordure (16x16x256 mais seulement les bords)
        List<BlockState> borderBlocks = extractBorderBlocks(myChunk, neighborPos);
        
        BorderSyncMessage msg = new BorderSyncMessage();
        msg.sourceChunk = myChunk.getPos();
        msg.targetChunk = neighborPos;
        msg.borderBlocks = borderBlocks;
        
        network.sendTo(msg, neighborOwner);
    }
}
```

### Étape 2.3 : Optimisation - Dirty Tracking

Ne synchroniser que ce qui a changé :

```java
public class ChunkDirtyTracker {
    
    // Tracker les sections modifiées (16x16x16)
    private Map<ChunkPos, BitSet> dirtySections = new ConcurrentHashMap<>();
    
    /**
     * Marquer une section comme modifiée
     */
    public void markDirty(ChunkPos chunk, int sectionY) {
        BitSet sections = dirtySections.computeIfAbsent(
            chunk, 
            k -> new BitSet(16)
        );
        sections.set(sectionY);
    }
    
    /**
     * Obtenir les sections à synchroniser
     */
    public List<Integer> getDirtySections(ChunkPos chunk) {
        BitSet sections = dirtySections.get(chunk);
        if (sections == null) return Collections.emptyList();
        
        List<Integer> result = new ArrayList<>();
        for (int i = sections.nextSetBit(0); i >= 0; i = sections.nextSetBit(i + 1)) {
            result.add(i);
        }
        
        return result;
    }
    
    /**
     * Effacer après sync
     */
    public void clearDirty(ChunkPos chunk) {
        dirtySections.remove(chunk);
    }
}
```

**Intégration dans le tick :**

```java
public void tickChunk(LevelChunk chunk) {
    ChunkPos pos = chunk.getPos();
    
    // Avant le tick, noter l'état
    ChunkSnapshot before = createSnapshot(chunk);
    
    // Tick normal
    chunk.tick(hasTimeLeft);
    
    // Après le tick, comparer et marquer dirty
    ChunkSnapshot after = createSnapshot(chunk);
    
    List<Integer> dirtySections = findDifferences(before, after);
    
    for (int sectionY : dirtySections) {
        dirtyTracker.markDirty(pos, sectionY);
    }
    
    // Synchroniser seulement si des changements
    if (!dirtySections.isEmpty()) {
        syncDirtySections(chunk, dirtySections);
    }
}
```

---

## 🎭 Phase 3 : Entités Distribuées

### Étape 3.1 : Système d'Entités "Ghost"

**Concept :**
- **Entité locale** : Calculée par ce peer (AI, physique, etc.)
- **Entité ghost** : Calculée ailleurs, juste affichée ici

**Classe à modifier : `net.minecraft.world.entity.Entity`**

```java
// Patch pour Entity.java

public abstract class Entity {
    
    // AJOUT : Flag pour entités ghost
    private boolean isGhost = false;
    private UUID ghostOwner = null;
    
    // MODIFICATION : Dans tick()
    public void tick() {
        // NOUVEAU : Skip le tick si ghost
        if (this.isGhost) {
            // Les ghosts ne font que de l'interpolation visuelle
            this.tickGhost();
            return;
        }
        
        // ... code existant pour entités locales ...
        
        // NOUVEAU : Après le tick, broadcaster si c'est mon entité
        if (this.shouldBroadcast()) {
            EntitySyncSystem.broadcastEntity(this);
        }
    }
    
    private void tickGhost() {
        // Interpolation de position pour smoothness
        this.lerpTo(
            this.targetX, 
            this.targetY, 
            this.targetZ, 
            this.targetYRot, 
            this.targetXRot, 
            3, 
            false
        );
    }
    
    public void setGhost(boolean ghost, UUID owner) {
        this.isGhost = ghost;
        this.ghostOwner = owner;
        
        if (ghost) {
            // Désactiver AI et physique
            if (this instanceof Mob) {
                ((Mob) this).setNoAi(true);
            }
            this.noPhysics = true;
        }
    }
    
    private boolean shouldBroadcast() {
        // Broadcaster si :
        // - C'est un mob (pas un item)
        // - Il a bougé
        // - Ça fait >50ms depuis le dernier broadcast
        
        return this instanceof LivingEntity 
            && !this.isGhost
            && (System.currentTimeMillis() - lastBroadcast > 50);
    }
}
```

### Étape 3.2 : Synchronisation des Entités

```java
package net.minecraft.p2p.entity;

public class EntitySyncSystem {
    
    // Mes entités (que je calcule)
    private static Set<Integer> myEntities = ConcurrentHashMap.newKeySet();
    
    // Entités ghost (calculées ailleurs)
    private static Set<Integer> ghostEntities = ConcurrentHashMap.newKeySet();
    
    // Dernière position connue pour interpolation
    private static Map<Integer, EntityState> lastKnownState = new ConcurrentHashMap<>();
    
    /**
     * Broadcaster une entité locale
     */
    public static void broadcastEntity(Entity entity) {
        int id = entity.getId();
        
        if (!myEntities.contains(id)) {
            myEntities.add(id);
        }
        
        // Créer le message
        EntityUpdateMessage msg = new EntityUpdateMessage();
        msg.entityId = id;
        msg.entityType = entity.getType().toString();
        msg.x = entity.getX();
        msg.y = entity.getY();
        msg.z = entity.getZ();
        msg.vx = entity.getDeltaMovement().x;
        msg.vy = entity.getDeltaMovement().y;
        msg.vz = entity.getDeltaMovement().z;
        msg.yaw = entity.getYRot();
        msg.pitch = entity.getXRot();
        msg.timestamp = System.currentTimeMillis();
        
        // Envoyer aux peers proches
        List<UUID> nearbyPeers = findPeersNear(entity.chunkPosition());
        P2PNetwork.sendToMultiple(msg, nearbyPeers);
    }
    
    /**
     * Recevoir une update d'entité
     */
    public static void receiveEntityUpdate(EntityUpdateMessage msg, Level level) {
        Entity entity = level.getEntity(msg.entityId);
        
        if (entity == null) {
            // Créer l'entité en mode ghost
            entity = createGhostEntity(msg, level);
            if (entity == null) return; // Type inconnu
        }
        
        // Marquer comme ghost
        entity.setGhost(true, msg.senderPeerId);
        ghostEntities.add(msg.entityId);
        
        // Mise à jour de position avec interpolation
        entity.lerpTo(
            msg.x, msg.y, msg.z,
            msg.yaw, msg.pitch,
            3, // Interpoler sur 3 ticks
            false
        );
        
        // Sauvegarder l'état pour prédiction
        EntityState state = new EntityState();
        state.x = msg.x;
        state.y = msg.y;
        state.z = msg.z;
        state.vx = msg.vx;
        state.vy = msg.vy;
        state.vz = msg.vz;
        state.timestamp = msg.timestamp;
        
        lastKnownState.put(msg.entityId, state);
    }
    
    /**
     * Créer une entité ghost
     */
    private static Entity createGhostEntity(EntityUpdateMessage msg, Level level) {
        EntityType<?> type = Registry.ENTITY_TYPE.get(
            new ResourceLocation(msg.entityType)
        );
        
        if (type == null) return null;
        
        Entity entity = type.create(level);
        entity.setId(msg.entityId);
        entity.setPos(msg.x, msg.y, msg.z);
        entity.setYRot(msg.yaw);
        entity.setXRot(msg.pitch);
        
        // Ajouter au monde
        level.addFreshEntity(entity);
        
        return entity;
    }
    
    /**
     * Prédiction client-side pour smoothness
     */
    public static void predictEntity(Entity ghost) {
        if (!ghost.isGhost()) return;
        
        EntityState lastState = lastKnownState.get(ghost.getId());
        if (lastState == null) return;
        
        // Temps depuis dernière update
        long elapsed = System.currentTimeMillis() - lastState.timestamp;
        
        if (elapsed > 1000) {
            // Pas d'update depuis 1 sec, entité probablement déchargée
            ghost.remove(RemovalReason.DISCARDED);
            return;
        }
        
        // Prédiction linéaire
        double predX = lastState.x + lastState.vx * (elapsed / 1000.0);
        double predY = lastState.y + lastState.vy * (elapsed / 1000.0);
        double predZ = lastState.z + lastState.vz * (elapsed / 1000.0);
        
        ghost.lerpTo(predX, predY, predZ, ghost.getYRot(), ghost.getXRot(), 1, false);
    }
}

class EntityState {
    double x, y, z;
    double vx, vy, vz;
    long timestamp;
}
```

### Étape 3.3 : Gestion des Joueurs

Les joueurs sont un cas spécial car ils sont TOUJOURS contrôlés par leur client.

```java
public class PlayerSyncSystem {
    
    /**
     * Les joueurs ne sont JAMAIS des ghosts
     * Chaque client contrôle son propre joueur
     */
    public static void syncPlayers() {
        // Chaque peer envoie la position de SON joueur
        ServerPlayer localPlayer = getLocalPlayer();
        
        if (localPlayer != null) {
            PlayerPositionMessage msg = new PlayerPositionMessage();
            msg.playerId = localPlayer.getUUID();
            msg.x = localPlayer.getX();
            msg.y = localPlayer.getY();
            msg.z = localPlayer.getZ();
            msg.yaw = localPlayer.getYRot();
            msg.pitch = localPlayer.getXRot();
            
            // Broadcast à tous
            P2PNetwork.broadcast(msg);
        }
    }
    
    /**
     * Recevoir la position d'un joueur distant
     */
    public static void receivePlayerPosition(PlayerPositionMessage msg, ServerLevel level) {
        ServerPlayer player = level.getPlayerByUUID(msg.playerId);
        
        if (player == null) {
            // Créer le joueur
            // Note : Nécessite de gérer l'authentification P2P
            player = createRemotePlayer(msg, level);
        }
        
        // Mettre à jour position
        player.setPos(msg.x, msg.y, msg.z);
        player.setYRot(msg.yaw);
        player.setXRot(msg.pitch);
    }
}
```

---

## 🔄 Phase 4 : Load Balancing Dynamique

### Algorithme de Rééquilibrage

```java
public class DynamicLoadBalancer {
    
    // Métriques de charge
    private Map<UUID, LoadMetrics> peerLoads = new ConcurrentHashMap<>();
    
    /**
     * Rééquilibrer toutes les 5 secondes
     */
    public void rebalance() {
        // 1. Collecter les métriques de tous les peers
        collectMetrics();
        
        // 2. Détecter déséquilibre
        if (!isBalanced()) {
            // 3. Calculer nouvelle distribution
            Map<ChunkPos, UUID> newDistribution = calculateOptimalDistribution();
            
            // 4. Appliquer migrations
            applyMigrations(newDistribution);
        }
    }
    
    private void collectMetrics() {
        for (PeerConnection peer : P2PNetwork.getPeers()) {
            LoadMetricsRequest req = new LoadMetricsRequest();
            LoadMetricsResponse resp = peer.sendRequest(req);
            
            peerLoads.put(peer.getPeerId(), resp.metrics);
        }
    }
    
    private boolean isBalanced() {
        if (peerLoads.isEmpty()) return true;
        
        int maxLoad = Collections.max(peerLoads.values(), 
            Comparator.comparing(m -> m.chunkCount)).chunkCount;
        int minLoad = Collections.min(peerLoads.values(), 
            Comparator.comparing(m -> m.chunkCount)).chunkCount;
        
        // Déséquilibré si écart > 30%
        float imbalance = (float)(maxLoad - minLoad) / maxLoad;
        return imbalance <= 0.3f;
    }
    
    private Map<ChunkPos, UUID> calculateOptimalDistribution() {
        // Algorithme glouton :
        // 1. Trier chunks par charge (entités, redstone, etc.)
        // 2. Assigner au peer le moins chargé
        
        Map<ChunkPos, UUID> distribution = new HashMap<>();
        List<ChunkPos> allChunks = getAllActiveChunks();
        
        // Trier par charge (descendant)
        allChunks.sort((a, b) -> Integer.compare(
            getChunkWeight(b), 
            getChunkWeight(a)
        ));
        
        for (ChunkPos chunk : allChunks) {
            // Trouver le peer le moins chargé
            UUID leastLoaded = findLeastLoadedPeer();
            distribution.put(chunk, leastLoaded);
            
            // Mettre à jour la charge virtuelle
            incrementVirtualLoad(leastLoaded, getChunkWeight(chunk));
        }
        
        return distribution;
    }
    
    private int getChunkWeight(ChunkPos pos) {
        // Poids basé sur :
        // - Nombre d'entités
        // - Complexité redstone
        // - Block entities (furnaces, etc.)
        
        LevelChunk chunk = getChunk(pos);
        if (chunk == null) return 1;
        
        int weight = 1;
        weight += chunk.getEntities().size() * 2;
        weight += chunk.getBlockEntities().size();
        // TODO : Détecter redstone actif
        
        return weight;
    }
}

class LoadMetrics {
    int chunkCount;
    int entityCount;
    float cpuUsage;
    long memoryUsed;
    int tickTime; // Microseconds
}
```

---

## 🛡️ Phase 5 : Robustesse et Edge Cases

### Gestion des Déconnexions

```java
public class DisconnectionHandler {
    
    /**
     * Quand un peer se déconnecte
     */
    public void onPeerDisconnect(UUID peerId) {
        // 1. Récupérer tous les chunks qu'il possédait
        List<ChunkPos> orphanedChunks = findChunksOwnedBy(peerId);
        
        // 2. Les redistribuer aux peers restants
        redistributeChunks(orphanedChunks);
        
        // 3. Récupérer ses entités
        List<Entity> orphanedEntities = findEntitiesOwnedBy(peerId);
        
        for (Entity entity : orphanedEntities) {
            // Trouver le peer le plus proche
            UUID newOwner = findNearestPeer(entity.chunkPosition());
            
            // Transférer ownership
            transferEntity(entity, newOwner);
        }
        
        // 4. Nettoyer la connexion
        P2PNetwork.removePeer(peerId);
    }
    
    /**
     * Détecter les peers qui ne répondent plus
     */
    public void detectDeadPeers() {
        long now = System.currentTimeMillis();
        
        for (PeerConnection peer : P2PNetwork.getPeers()) {
            long lastHeartbeat = peer.getLastHeartbeat();
            
            if (now - lastHeartbeat > 5000) {
                // Pas de heartbeat depuis 5 sec = mort
                onPeerDisconnect(peer.getPeerId());
            }
        }
    }
}
```

### Synchronisation de Sauvegarde

```java
public class WorldSaveSync {
    
    /**
     * Quand on sauvegarde, synchroniser avec le serveur central
     */
    public void saveWorld() {
        // 1. Sauvegarder localement tous MES chunks
        for (ChunkPos pos : DistributedChunkManager.getMyChunks()) {
            LevelChunk chunk = getChunk(pos);
            saveChunkToFile(chunk);
        }
        
        // 2. Uploader au serveur central
        uploadModifiedRegions();
        
        // 3. Notifier les autres peers
        notifyWorldSaved();
    }
    
    private void uploadModifiedRegions() {
        // Trouver les fichiers régions modifiés
        List<File> modifiedRegions = findModifiedRegionFiles();
        
        for (File regionFile : modifiedRegions) {
            // Upload via HTTP
            uploadRegionFile(regionFile);
        }
    }
    
    private void uploadRegionFile(File file) {
        try {
            URL url = new URL("http://central-server.com/worlds/myworld/regions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            
            // Envoyer le fichier
            OutputStream out = conn.getOutputStream();
            Files.copy(file.toPath(), out);
            out.close();
            
            int response = conn.getResponseCode();
            // Handle response
            
        } catch (IOException e) {
            // Retry logic
        }
    }
}
```

---

## 📊 Métriques et Monitoring

### Dashboard de Performance

```java
public class PerformanceMonitor {
    
    // Métriques à tracker
    private long chunksCalculatedPerSecond;
    private long entitiesSyncedPerSecond;
    private long bytesReceivedPerSecond;
    private long bytesSentPerSecond;
    private int activePeers;
    private float averageTickTime;
    
    /**
     * Exposer via API pour dashboard web
     */
    public MetricsSnapshot getMetrics() {
        MetricsSnapshot snapshot = new MetricsSnapshot();
        
        snapshot.chunksPerSec = chunksCalculatedPerSecond;
        snapshot.entitiesPerSec = entitiesSyncedPerSecond;
        snapshot.networkIn = bytesReceivedPerSecond;
        snapshot.networkOut = bytesSentPerSecond;
        snapshot.peers = activePeers;
        snapshot.tickTime = averageTickTime;
        snapshot.timestamp = System.currentTimeMillis();
        
        return snapshot;
    }
    
    /**
     * Logger les métriques toutes les 10 secondes
     */
    public void logMetrics() {
        LOGGER.info("=== P2P Metrics ===");
        LOGGER.info("Chunks/sec: " + chunksCalculatedPerSecond);
        LOGGER.info("Entities/sec: " + entitiesSyncedPerSecond);
        LOGGER.info("Network: ↓ " + formatBytes(bytesReceivedPerSecond) + 
                    "/s ↑ " + formatBytes(bytesSentPerSecond) + "/s");
        LOGGER.info("Active peers: " + activePeers);
        LOGGER.info("Avg tick time: " + averageTickTime + "ms");
    }
}
```

---

## 🎯 Critères de Succès Finaux

### Performance
- [ ] 10+ joueurs simultanés avec distribution équitable
- [ ] <50ms de latence pour sync d'entités
- [ ] <5% overhead CPU vs serveur normal
- [ ] <2 Mbps bande passante par joueur

### Stabilité
- [ ] Gestion propre des déconnexions
- [ ] Pas de duplication d'entités
- [ ] Pas de perte de données
- [ ] Récupération automatique après crash

### Expérience Joueur
- [ ] Mouvement fluide des entités distantes
- [ ] Pas de "ghosting" visible
- [ ] Latence imperceptible (<100ms)
- [ ] Pas de différence avec serveur classique

---

## 🚀 Prochaines Étapes

1. **Semaine 1-2** : Setup environnement + décompilation
2. **Semaine 3-5** : POC Phase 1 (serveur intégré + P2P basique)
3. **Semaine 6-9** : Distribution chunks complète
4. **Semaine 10-13** : Entités distribuées
5. **Semaine 14-15** : Optimisation et robustesse
6. **Semaine 16** : Tests et polish

**Durée totale estimée : ~4 mois**

---

## 📚 Ressources et Outils

### Décompilation
- **MCP** (Mod Coder Pack) : Mappings officiels
- **Fabric** : Toolchain moderne pour modding
- **ForgeGradle** : Build system

### Développement
- **ASM** : Manipulation bytecode
- **Mixin** : Framework d'injection de code
- **JProfiler** : Profiling performance

### Réseau
- **Netty** : Framework réseau (déjà dans Minecraft)
- **Protocol Buffers** : Sérialisation efficace
- **WebRTC** : P2P avec NAT traversal

### Testing
- **JUnit** : Tests unitaires
- **Mockito** : Mocking
- **Gatling** : Tests de charge

---

**Version** : 1.0  
**Dernière mise à jour** : Mai 2026