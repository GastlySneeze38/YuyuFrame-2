# Plan d'Action : Du Stub au POC Fonctionnel

## 📍 État Actuel

✅ **Infrastructure de base**
- Signaling WebSocket (découverte peers)
- Java Agent (injection bytecode)
- MojangRemapper (JAR deobfusqué)
- Hooks installés : `LevelChunk.tick()`, `ServerLevel.tick()`, `Entity.tick()`
- Stubs : `DistributedChunkManager`, `EntitySyncSystem`
- `rust_core.dll` (pont JNI)

❌ **Manque pour le POC**
- Logique de distribution de chunks
- Communication P2P réelle (messages)
- Synchronisation état monde
- Tests multi-instances

---

## 🎯 Objectif POC (2-3 semaines)

**Démo cible :**
```
Instance A                    Instance B
   ↓                             ↓
Spawn (0, 0)                 Spawn (100, 0)
   ↓                             ↓
Calcule chunks -50→50        Calcule chunks 50→150
   ↓                             ↓
Place bloc à (10, 64, 10)    Voit le bloc apparaître !
   ↓                             ↓
Voit joueur B se déplacer    Voit joueur A se déplacer
```

**Critères de succès :**
- [ ] 2 instances se connectent via signaling
- [ ] Chunks distribués (A calcule moitié ouest, B moitié est)
- [ ] Changements de blocs synchronisés
- [ ] Joueurs se voient mutuellement
- [ ] Pas de crash pendant 5 minutes

---

## 📋 Plan d'Action Étape par Étape

### **PHASE 1 : Communication P2P Basique (3-4 jours)**

#### Étape 1.1 : Activer le pont Rust
**Objectif :** Utiliser `rust_core.dll` pour la communication P2P

**Code Java :**

```java
// src/main/java/com/p2p/network/RustBridge.java
package com.p2p.network;

public class RustBridge {
    
    static {
        // Charger la DLL Rust
        System.loadLibrary("rust_core");
    }
    
    // Natives déclarées (implémentées en Rust)
    public static native long initP2P(String peerId, int port);
    public static native void connectToPeer(long handle, String peerAddress);
    public static native void sendMessage(long handle, String peerId, byte[] data);
    public static native byte[] receiveMessage(long handle);
    public static native void shutdown(long handle);
    
    // Handle du réseau P2P
    private static long networkHandle = 0;
    
    /**
     * Initialiser le réseau P2P
     */
    public static void initialize(String peerId, int port) {
        networkHandle = initP2P(peerId, port);
        
        if (networkHandle == 0) {
            throw new RuntimeException("Failed to initialize P2P network");
        }
        
        System.out.println("[P2P] Network initialized on port " + port);
    }
    
    /**
     * Envoyer un message à un peer
     */
    public static void send(String targetPeerId, byte[] message) {
        if (networkHandle == 0) {
            throw new IllegalStateException("P2P not initialized");
        }
        
        sendMessage(networkHandle, targetPeerId, message);
    }
    
    /**
     * Recevoir un message (non-bloquant)
     */
    public static byte[] receive() {
        if (networkHandle == 0) return null;
        return receiveMessage(networkHandle);
    }
}
```

**Code Rust (rust_core/src/lib.rs) :**

```rust
use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jint, jbyteArray};
use std::sync::Arc;
use tokio::sync::Mutex;
use webrtc::peer_connection::RTCPeerConnection;

// Structure du réseau P2P
struct P2PNetwork {
    peer_id: String,
    port: u16,
    peers: Vec<RTCPeerConnection>,
    // TODO: Ajouter canaux de messages
}

#[no_mangle]
pub extern "system" fn Java_com_p2p_network_RustBridge_initP2P(
    env: JNIEnv,
    _class: JClass,
    peer_id: JString,
    port: jint,
) -> jlong {
    
    let peer_id_str: String = env.get_string(peer_id)
        .expect("Invalid peer ID")
        .into();
    
    // Créer le réseau P2P
    let network = P2PNetwork {
        peer_id: peer_id_str,
        port: port as u16,
        peers: Vec::new(),
    };
    
    // Boxé et retourner le pointeur
    let boxed = Box::new(network);
    Box::into_raw(boxed) as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_p2p_network_RustBridge_sendMessage(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    peer_id: JString,
    data: jbyteArray,
) {
    
    // Récupérer le réseau
    let network = unsafe { &mut *(handle as *mut P2PNetwork) };
    
    // Convertir les données Java → Rust
    let data_bytes = env.convert_byte_array(data)
        .expect("Failed to convert byte array");
    
    let peer_id_str: String = env.get_string(peer_id)
        .expect("Invalid peer ID")
        .into();
    
    // TODO: Envoyer via WebRTC
    println!("[Rust] Send {} bytes to {}", data_bytes.len(), peer_id_str);
}

#[no_mangle]
pub extern "system" fn Java_com_p2p_network_RustBridge_receiveMessage(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jbyteArray {
    
    // TODO: Recevoir depuis la queue
    // Pour l'instant, retourner null
    JObject::null().into_inner()
}
```

#### Étape 1.2 : Protocole de Messages

**Définir les types de messages :**

```java
// src/main/java/com/p2p/protocol/MessageType.java
package com.p2p.protocol;

public enum MessageType {
    HANDSHAKE(0x01),           // Connexion initiale
    CHUNK_OWNERSHIP(0x02),     // Déclaration d'ownership
    BLOCK_CHANGE(0x03),        // Changement de bloc
    ENTITY_UPDATE(0x04),       // Position entité
    PLAYER_POSITION(0x05);     // Position joueur
    
    public final byte id;
    
    MessageType(int id) {
        this.id = (byte) id;
    }
    
    public static MessageType fromByte(byte b) {
        for (MessageType type : values()) {
            if (type.id == b) return type;
        }
        throw new IllegalArgumentException("Unknown message type: " + b);
    }
}
```

**Classe de message :**

```java
// src/main/java/com/p2p/protocol/P2PMessage.java
package com.p2p.protocol;

import java.io.*;
import java.nio.ByteBuffer;

public abstract class P2PMessage {
    
    protected MessageType type;
    
    public P2PMessage(MessageType type) {
        this.type = type;
    }
    
    /**
     * Sérialiser en bytes
     */
    public byte[] serialize() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        try {
            // Header : type (1 byte)
            dos.writeByte(type.id);
            
            // Payload
            writePayload(dos);
            
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }
    
    /**
     * Désérialiser depuis bytes
     */
    public static P2PMessage deserialize(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        
        try {
            // Lire le type
            byte typeId = dis.readByte();
            MessageType type = MessageType.fromByte(typeId);
            
            // Créer le message approprié
            switch (type) {
                case HANDSHAKE:
                    return HandshakeMessage.read(dis);
                case CHUNK_OWNERSHIP:
                    return ChunkOwnershipMessage.read(dis);
                case BLOCK_CHANGE:
                    return BlockChangeMessage.read(dis);
                case ENTITY_UPDATE:
                    return EntityUpdateMessage.read(dis);
                case PLAYER_POSITION:
                    return PlayerPositionMessage.read(dis);
                default:
                    throw new IllegalArgumentException("Unknown type: " + type);
            }
            
        } catch (IOException e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }
    
    protected abstract void writePayload(DataOutputStream out) throws IOException;
}
```

**Message de handshake :**

```java
// src/main/java/com/p2p/protocol/HandshakeMessage.java
package com.p2p.protocol;

import java.io.*;
import java.util.UUID;

public class HandshakeMessage extends P2PMessage {
    
    public UUID peerId;
    public String minecraftVersion;
    public String worldId;
    
    public HandshakeMessage() {
        super(MessageType.HANDSHAKE);
    }
    
    @Override
    protected void writePayload(DataOutputStream out) throws IOException {
        // UUID (16 bytes)
        out.writeLong(peerId.getMostSignificantBits());
        out.writeLong(peerId.getLeastSignificantBits());
        
        // Version (string)
        out.writeUTF(minecraftVersion);
        
        // World ID (string)
        out.writeUTF(worldId);
    }
    
    public static HandshakeMessage read(DataInputStream in) throws IOException {
        HandshakeMessage msg = new HandshakeMessage();
        
        long msb = in.readLong();
        long lsb = in.readLong();
        msg.peerId = new UUID(msb, lsb);
        
        msg.minecraftVersion = in.readUTF();
        msg.worldId = in.readUTF();
        
        return msg;
    }
}
```

#### Étape 1.3 : Gestionnaire de Messages

```java
// src/main/java/com/p2p/network/MessageHandler.java
package com.p2p.network;

import com.p2p.protocol.*;
import java.util.*;
import java.util.concurrent.*;

public class MessageHandler {
    
    private static final Map<UUID, PeerInfo> connectedPeers = new ConcurrentHashMap<>();
    private static UUID myPeerId;
    
    /**
     * Démarrer la boucle de réception
     */
    public static void startReceiveLoop() {
        Thread receiveThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    // Recevoir depuis Rust
                    byte[] data = RustBridge.receive();
                    
                    if (data != null && data.length > 0) {
                        // Désérialiser
                        P2PMessage message = P2PMessage.deserialize(data);
                        
                        // Dispatcher
                        handleMessage(message);
                    }
                    
                    // Petite pause pour éviter busy-wait
                    Thread.sleep(10);
                    
                } catch (Exception e) {
                    System.err.println("[P2P] Receive error: " + e.getMessage());
                }
            }
        });
        
        receiveThread.setName("P2P-Receive");
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * Dispatcher de messages
     */
    private static void handleMessage(P2PMessage message) {
        switch (message.type) {
            case HANDSHAKE:
                handleHandshake((HandshakeMessage) message);
                break;
                
            case CHUNK_OWNERSHIP:
                handleChunkOwnership((ChunkOwnershipMessage) message);
                break;
                
            case BLOCK_CHANGE:
                handleBlockChange((BlockChangeMessage) message);
                break;
                
            case ENTITY_UPDATE:
                handleEntityUpdate((EntityUpdateMessage) message);
                break;
                
            case PLAYER_POSITION:
                handlePlayerPosition((PlayerPositionMessage) message);
                break;
        }
    }
    
    private static void handleHandshake(HandshakeMessage msg) {
        System.out.println("[P2P] Peer connected: " + msg.peerId);
        
        PeerInfo info = new PeerInfo();
        info.peerId = msg.peerId;
        info.minecraftVersion = msg.minecraftVersion;
        info.worldId = msg.worldId;
        info.lastSeen = System.currentTimeMillis();
        
        connectedPeers.put(msg.peerId, info);
    }
    
    // TODO: Implémenter les autres handlers
    
    /**
     * Envoyer un message à tous les peers
     */
    public static void broadcast(P2PMessage message) {
        byte[] data = message.serialize();
        
        for (UUID peerId : connectedPeers.keySet()) {
            RustBridge.send(peerId.toString(), data);
        }
    }
    
    /**
     * Obtenir la liste des peers connectés
     */
    public static List<UUID> getConnectedPeers() {
        return new ArrayList<>(connectedPeers.keySet());
    }
}

class PeerInfo {
    UUID peerId;
    String minecraftVersion;
    String worldId;
    long lastSeen;
}
```

---

### **PHASE 2 : Distribution de Chunks (4-5 jours)**

#### Étape 2.1 : Implémenter DistributedChunkManager

**Code complet :**

```java
// src/main/java/com/p2p/distributed/DistributedChunkManager.java
package com.p2p.distributed;

import com.p2p.network.MessageHandler;
import com.p2p.protocol.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DistributedChunkManager {
    
    // Mon UUID
    private static UUID myPeerId;
    
    // Table d'ownership : ChunkPos → Peer UUID
    private static final Map<ChunkPos, UUID> chunkOwnership = new ConcurrentHashMap<>();
    
    // Chunks que JE calcule
    private static final Set<ChunkPos> myChunks = ConcurrentHashMap.newKeySet();
    
    /**
     * Initialisation
     */
    public static void initialize(UUID peerId) {
        myPeerId = peerId;
        
        System.out.println("[DistributedChunkManager] Initialized with peer ID: " + peerId);
    }
    
    /**
     * POINT D'ENTRÉE : Injecté dans ServerLevel.tick()
     */
    public static boolean shouldTickWorld(ServerLevel level) {
        // Pour le POC, on tick toujours le monde
        // (La distribution se fait au niveau chunk)
        return true;
    }
    
    /**
     * POINT D'ENTRÉE : Injecté dans LevelChunk.tick()
     */
    public static boolean shouldTickChunk(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        
        // Trouver ou assigner l'owner de ce chunk
        UUID owner = getOrAssignOwner(pos);
        
        if (owner == null) {
            // Personne dans cette zone, skip
            return false;
        }
        
        // Est-ce que c'est moi qui dois calculer ?
        boolean isMine = owner.equals(myPeerId);
        
        if (isMine) {
            myChunks.add(pos);
        } else {
            myChunks.remove(pos);
        }
        
        return isMine;
    }
    
    /**
     * POINT D'ENTRÉE : Après le tick du chunk
     */
    public static void afterChunkTick(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        
        if (!myChunks.contains(pos)) {
            // Pas mon chunk, rien à faire
            return;
        }
        
        // TODO: Tracker les changements et broadcaster
        // Pour le POC, on broadcaster périodiquement
    }
    
    /**
     * Algorithme d'assignment d'ownership
     * POC Simple : Division verticale du monde
     */
    private static UUID getOrAssignOwner(ChunkPos pos) {
        // Chercher dans le cache
        UUID cached = chunkOwnership.get(pos);
        if (cached != null) {
            return cached;
        }
        
        // Obtenir les peers connectés
        List<UUID> peers = MessageHandler.getConnectedPeers();
        
        if (peers.isEmpty()) {
            // Je suis seul
            chunkOwnership.put(pos, myPeerId);
            return myPeerId;
        }
        
        // POC : Division simple
        // X < 0 : Peer 0
        // X >= 0 : Peer 1
        
        UUID owner;
        if (pos.x < 0) {
            owner = myPeerId; // Moi = peer 0
        } else {
            owner = peers.get(0); // Premier peer = peer 1
        }
        
        chunkOwnership.put(pos, owner);
        
        // Broadcaster l'ownership
        ChunkOwnershipMessage msg = new ChunkOwnershipMessage();
        msg.chunkX = pos.x;
        msg.chunkZ = pos.z;
        msg.ownerId = owner;
        
        MessageHandler.broadcast(msg);
        
        return owner;
    }
    
    /**
     * Recevoir un message d'ownership d'un autre peer
     */
    public static void handleChunkOwnership(ChunkOwnershipMessage msg) {
        ChunkPos pos = new ChunkPos(msg.chunkX, msg.chunkZ);
        chunkOwnership.put(pos, msg.ownerId);
    }
    
    /**
     * Debug : Afficher les stats
     */
    public static void printStats() {
        System.out.println("=== Chunk Distribution ===");
        System.out.println("My chunks: " + myChunks.size());
        System.out.println("Total known: " + chunkOwnership.size());
    }
}
```

#### Étape 2.2 : Message ChunkOwnership

```java
// src/main/java/com/p2p/protocol/ChunkOwnershipMessage.java
package com.p2p.protocol;

import java.io.*;
import java.util.UUID;

public class ChunkOwnershipMessage extends P2PMessage {
    
    public int chunkX;
    public int chunkZ;
    public UUID ownerId;
    
    public ChunkOwnershipMessage() {
        super(MessageType.CHUNK_OWNERSHIP);
    }
    
    @Override
    protected void writePayload(DataOutputStream out) throws IOException {
        out.writeInt(chunkX);
        out.writeInt(chunkZ);
        out.writeLong(ownerId.getMostSignificantBits());
        out.writeLong(ownerId.getLeastSignificantBits());
    }
    
    public static ChunkOwnershipMessage read(DataInputStream in) throws IOException {
        ChunkOwnershipMessage msg = new ChunkOwnershipMessage();
        
        msg.chunkX = in.readInt();
        msg.chunkZ = in.readInt();
        
        long msb = in.readLong();
        long lsb = in.readLong();
        msg.ownerId = new UUID(msb, lsb);
        
        return msg;
    }
}
```

---

### **PHASE 3 : Synchronisation Blocs (3-4 jours)**

#### Étape 3.1 : Tracker les Changements de Blocs

```java
// src/main/java/com/p2p/distributed/BlockChangeTracker.java
package com.p2p.distributed;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlockChangeTracker {
    
    // Changes par chunk : ChunkPos → Liste de changements
    private static final Map<ChunkPos, List<BlockChange>> pendingChanges = 
        new ConcurrentHashMap<>();
    
    /**
     * Enregistrer un changement de bloc
     * APPELÉ depuis un hook dans setBlockState()
     */
    public static void trackChange(BlockPos pos, BlockState oldState, BlockState newState) {
        ChunkPos chunkPos = new ChunkPos(pos);
        
        // Vérifier si c'est mon chunk
        if (!DistributedChunkManager.isMyChunk(chunkPos)) {
            return; // Pas mon chunk, ignorer
        }
        
        BlockChange change = new BlockChange();
        change.x = pos.getX();
        change.y = pos.getY();
        change.z = pos.getZ();
        change.oldState = oldState;
        change.newState = newState;
        change.timestamp = System.currentTimeMillis();
        
        // Ajouter à la liste
        pendingChanges
            .computeIfAbsent(chunkPos, k -> new ArrayList<>())
            .add(change);
    }
    
    /**
     * Flusher les changements (appelé périodiquement)
     */
    public static void flushChanges() {
        if (pendingChanges.isEmpty()) return;
        
        // Créer des messages
        for (Map.Entry<ChunkPos, List<BlockChange>> entry : pendingChanges.entrySet()) {
            ChunkPos chunk = entry.getKey();
            List<BlockChange> changes = entry.getValue();
            
            if (changes.isEmpty()) continue;
            
            // Créer message
            BlockChangeMessage msg = new BlockChangeMessage();
            msg.chunkX = chunk.x;
            msg.chunkZ = chunk.z;
            msg.changes = new ArrayList<>(changes);
            
            // Broadcaster
            MessageHandler.broadcast(msg);
            
            // Clear
            changes.clear();
        }
    }
}

class BlockChange {
    int x, y, z;
    BlockState oldState;
    BlockState newState;
    long timestamp;
}
```

#### Étape 3.2 : Hook dans setBlockState

**Modification du transformer :**

```java
// Dans ChunkTransformer.java ou similaire
public class BlockStateTransformer {
    
    public static byte[] transform(byte[] bytecode) {
        // Trouver la méthode setBlockState dans LevelChunk
        // Injecter APRÈS le changement :
        // BlockChangeTracker.trackChange(pos, oldState, newState);
        
        ClassReader reader = new ClassReader(bytecode);
        ClassWriter writer = new ClassWriter(COMPUTE_MAXS);
        
        ClassVisitor visitor = new ClassVisitor(ASM9, writer) {
            public MethodVisitor visitMethod(int access, String name, ...) {
                if (name.equals("setBlockState")) {
                    return new MethodVisitor(ASM9, mv) {
                        @Override
                        public void visitInsn(int opcode) {
                            // AVANT chaque RETURN
                            if (opcode >= IRETURN && opcode <= RETURN) {
                                // Injecter trackChange
                                mv.visitVarInsn(ALOAD, 1); // pos
                                mv.visitVarInsn(ALOAD, 2); // oldState
                                mv.visitVarInsn(ALOAD, 3); // newState
                                
                                mv.visitMethodInsn(
                                    INVOKESTATIC,
                                    "com/p2p/distributed/BlockChangeTracker",
                                    "trackChange",
                                    "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
                                    false
                                );
                            }
                            
                            super.visitInsn(opcode);
                        }
                    };
                }
                return mv;
            }
        };
        
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }
}
```

#### Étape 3.3 : Appliquer les Changements Reçus

```java
// Dans MessageHandler.java
private static void handleBlockChange(BlockChangeMessage msg) {
    ChunkPos chunkPos = new ChunkPos(msg.chunkX, msg.chunkZ);
    
    // Obtenir le chunk côté client
    // Note: Nécessite accès au ClientLevel
    LevelChunk chunk = getClientChunk(chunkPos);
    
    if (chunk == null) {
        // Chunk pas chargé, ignorer
        return;
    }
    
    // Appliquer tous les changements
    for (BlockChange change : msg.changes) {
        BlockPos pos = new BlockPos(change.x, change.y, change.z);
        
        chunk.setBlockState(
            pos, 
            change.newState, 
            false  // false = pas de notification (déjà calculé ailleurs)
        );
    }
}
```

---

### **PHASE 4 : Tests Multi-Instances (2-3 jours)**

#### Étape 4.1 : Script de Test

```bash
#!/bin/bash
# test-p2p.sh

# Lancer 2 instances de Minecraft

# Instance 1
java -javaagent:p2p-agent.jar=peerId=peer-1,worldId=test,port=25001 \
     -Xmx2G \
     -cp client-mapped.jar \
     net.minecraft.client.main.Main \
     --username Player1 &

PID1=$!

sleep 10

# Instance 2
java -javaagent:p2p-agent.jar=peerId=peer-2,worldId=test,port=25002 \
     -Xmx2G \
     -cp client-mapped.jar \
     net.minecraft.client.main.Main \
     --username Player2 &

PID2=$!

echo "Instance 1 PID: $PID1"
echo "Instance 2 PID: $PID2"

# Attendre Ctrl+C
trap "kill $PID1 $PID2" EXIT
wait
```

#### Étape 4.2 : Tests à Faire

**Test 1 : Connexion P2P**
```
1. Lancer instance 1
2. Vérifier logs : "[P2P] Network initialized"
3. Lancer instance 2
4. Vérifier logs : "[P2P] Peer connected: peer-2"
```

**Test 2 : Distribution Chunks**
```
1. Instance 1 spawn à (0, 64, 0)
2. Instance 2 spawn à (100, 64, 0)
3. Vérifier logs instance 1 : "My chunks: ~50" (X < 0)
4. Vérifier logs instance 2 : "My chunks: ~50" (X >= 0)
```

**Test 3 : Synchronisation Blocs**
```
1. Dans instance 1, placer bloc à (10, 64, 10)
2. Vérifier instance 2 : bloc doit apparaître
3. Dans instance 2, placer bloc à (110, 64, 10)
4. Vérifier instance 1 : bloc doit apparaître
```

---

## 📊 Checklist Complète POC

### Infrastructure
- [x] Java Agent fonctionnel
- [x] MojangRemapper fonctionnel
- [x] Signaling WebSocket
- [ ] Rust bridge activé
- [ ] Messages P2P implémentés

### Distribution
- [x] Hooks installés (tick)
- [ ] DistributedChunkManager complet
- [ ] Algorithme d'assignment
- [ ] Broadcast ownership

### Synchronisation
- [ ] BlockChangeTracker
- [ ] Hook setBlockState
- [ ] Messages BlockChange
- [ ] Application changements

### Tests
- [ ] 2 instances se connectent
- [ ] Chunks distribués
- [ ] Blocs synchronisés
- [ ] Stable 5+ minutes

---

## 🚀 Timeline Estimée

```
Semaine 1 :
- Jour 1-2 : Communication P2P basique
- Jour 3-4 : Protocole messages
- Jour 5   : Tests connexion

Semaine 2 :
- Jour 1-2 : DistributedChunkManager
- Jour 3-4 : BlockChangeTracker
- Jour 5   : Tests distribution

Semaine 3 :
- Jour 1-2 : Synchronisation complète
- Jour 3-4 : Tests multi-instances
- Jour 5   : Debug et stabilisation
```

---

## 💡 Conseils Pratiques

### Debug
```java
// Ajouter des logs partout
System.out.println("[P2P] " + message);

// Activer logging ASM
System.setProperty("jdk.instrument.traceLevel", "2");
```

### Performance
```java
// Flusher changements toutes les 50ms (20 Hz)
ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
executor.scheduleAtFixedRate(
    BlockChangeTracker::flushChanges,
    0, 50, TimeUnit.MILLISECONDS
);
```

### Tests Unitaires
```java
@Test
public void testChunkAssignment() {
    UUID peer1 = UUID.randomUUID();
    UUID peer2 = UUID.randomUUID();
    
    DistributedChunkManager.initialize(peer1);
    
    ChunkPos negChunk = new ChunkPos(-5, 0);
    ChunkPos posChunk = new ChunkPos(5, 0);
    
    // Vérifier assignment
    assertEquals(peer1, getOwner(negChunk));
    assertEquals(peer2, getOwner(posChunk));
}
```

---

## 📦 Fichiers à Créer/Modifier

**Nouveau :**
```
src/main/java/com/p2p/
├── network/
│   ├── RustBridge.java          ← CRÉER
│   └── MessageHandler.java      ← CRÉER
├── protocol/
│   ├── MessageType.java         ← CRÉER
│   ├── P2PMessage.java          ← CRÉER
│   ├── HandshakeMessage.java    ← CRÉER
│   ├── ChunkOwnershipMessage.java ← CRÉER
│   └── BlockChangeMessage.java  ← CRÉER
├── distributed/
│   ├── DistributedChunkManager.java ← COMPLÉTER
│   └── BlockChangeTracker.java  ← CRÉER
└── transformer/
    └── BlockStateTransformer.java ← CRÉER
```

**Rust :**
```
rust_core/src/
├── lib.rs                       ← COMPLÉTER
├── network.rs                   ← CRÉER
└── webrtc.rs                    ← CRÉER
```

---

**Prochaine action immédiate :** Implémenter `RustBridge.java` et tester la communication P2P basique !

Voulez-vous que je génère le code complet de l'un de ces fichiers ?