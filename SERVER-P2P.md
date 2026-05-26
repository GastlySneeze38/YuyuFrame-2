# YuyuFrame — Serveur Minecraft P2P Distribué

## Vue d'ensemble

YuyuFrame est un système qui permet à plusieurs joueurs Minecraft de partager la charge de simulation du monde sans serveur central. Chaque joueur simule la portion du monde qui lui est attribuée, diffuse les changements aux autres, et reçoit en retour les changements des portions qu'il ne simule pas. Du point de vue de chaque joueur, le monde est complet et cohérent.

```
Serveur classique :
┌──────────────────┐
│  Serveur central │  ← simule tout le monde, seul
└──────────────────┘
         ↓
   tous les joueurs reçoivent

YuyuFrame :
┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐
│ Pair A │  │ Pair B │  │ Pair C │  │ Pair D │
│ zone A │  │ zone B │  │ zone C │  │ zone D │
└────────┘  └────────┘  └────────┘  └────────┘
     ↕           ↕           ↕           ↕
              deltas P2P entre pairs
```

La charge est distribuée proportionnellement au nombre de joueurs. Plus il y a de joueurs, moins chacun simule. Un joueur seul simule tout. Dix joueurs répartis simulent chacun un dixième.

---

## Principe de fonctionnement

### L'unité de simulation : le quadrant

Le monde n'est pas distribué au niveau du chunk entier mais au niveau du quadrant. Chaque chunk Minecraft de seize blocs sur seize est subdivisé en quatre quadrants de huit blocs sur huit. Cette granularité plus fine permet une distribution équitable même quand plusieurs joueurs se trouvent dans la même zone.

### L'attribution : déterministe et sans consensus

Chaque pair calcule localement qui doit simuler chaque quadrant en appliquant la même règle : le quadrant appartient au pair dont la position est la plus proche du centre géométrique du quadrant. En cas d'égalité exacte, l'identifiant du pair sert de départage. Cette règle est identique sur toutes les machines — le résultat est le même partout sans qu'aucune communication ne soit nécessaire pour s'accorder.

### Les deltas : ce qui circule sur le réseau

À chaque tick (vingt fois par seconde), chaque pair capture les changements produits par sa simulation — blocs modifiés, entités déplacées, spawns, morts — et les diffuse aux autres pairs sous forme de delta. Les pairs reçoivent ces deltas et les appliquent directement dans leur représentation locale du monde. Quand rien ne change, aucun delta n'est émis. Le trafic réseau est proportionnel à l'activité réelle du monde, pas à son existence.

### La confiance mutuelle

YuyuFrame est conçu pour des sessions entre joueurs qui se connaissent et se font confiance. Il n'y a pas d'anticheat, pas de validation des deltas reçus. Un pair est autorité absolue sur ses quadrants. Ce choix délibéré rend l'architecture radicalement plus simple que tout système avec validation distribuée.

---

## Architecture technique

YuyuFrame est constitué de quatre couches superposées, chacune documentée séparément.

```
┌────────────────────────────────────────────────────┐
│  Couche 4 — Border Sync                            │
│  Ownership physic-aware pour fluides et redstone   │
│  Ghost layer en lecture seule pour les entités     │
└────────────────────────────────────────────────────┘
                        ↕
┌────────────────────────────────────────────────────┐
│  Couche 3 — Simulation locale et deltas            │
│  Java Agent + Mixin standalone                     │
│  Serveur intégré Minecraft (simulation sélective)  │
│  Cœur Rust (deltas, JNI, libp2p)                  │
│  Injection directe dans ClientWorld               │
└────────────────────────────────────────────────────┘
                        ↕
┌────────────────────────────────────────────────────┐
│  Couche 2 — Ownership Engine                       │
│  Attribution des quadrants par distance minimale   │
│  Gestion des transferts d'entités                  │
│  Détection des phénomènes physiques aux frontières │
└────────────────────────────────────────────────────┘
                        ↕
┌────────────────────────────────────────────────────┐
│  Couche 1 — Connectivité P2P                       │
│  libp2p (NAT traversal, hole punching)             │
│  Relay server léger (signaling uniquement)         │
│  Tunnel chiffré Noise Protocol                     │
└────────────────────────────────────────────────────┘
```

---

## Les composants

### Le Java Agent

YuyuFrame ne s'installe pas comme un mod. Il s'injecte dans la JVM Minecraft via un argument de démarrage standard :

```
java -javaagent:yuyuframe.jar -jar minecraft.jar
```

Le Java Agent s'exécute avant Minecraft, initialise Mixin standalone avec les mappings d'obfuscation de la version Minecraft détectée, installe les hooks sur les méthodes critiques du jeu, et charge le cœur Rust. Minecraft démarre ensuite normalement, déjà instrumenté, sans qu'aucun fichier n'ait été modifié.

### Mixin standalone

Mixin est la bibliothèque qui installe les hooks dans le bytecode Minecraft. Elle est utilisée ici sans Fabric Loader — comme bibliothèque autonome initialisée par le Java Agent. Elle gère l'obfuscation automatiquement via les MojMap et garantit que les hooks sont installés aux bons endroits même entre versions de Minecraft. Les hooks sont installés sur deux composants distincts : le serveur intégré pour contrôler ce qu'il simule et capturer les changements, et `ClientWorld` pour recevoir les deltas des pairs.

### Le cœur Rust

Le cœur Rust est une bibliothèque native chargée via JNI. C'est lui qui gère tout ce qui touche au réseau P2P et aux deltas. Il contient l'ownership engine, le pipeline libp2p pour la communication entre pairs, la sérialisation et désérialisation binaire des deltas, et la file d'attente des deltas entrants. Il communique avec le code Java via un pont JNI minimal — deux appels par tick, un en début et un en fin.

### Le serveur intégré Minecraft

Quand un joueur ouvre un monde solo dans Minecraft, un serveur intégré s'exécute dans la même JVM. YuyuFrame utilise ce serveur intégré comme moteur de simulation pour les quadrants owné par ce pair. Il simule uniquement ces quadrants — les quadrants des autres pairs sont gelés pour lui. Il ne sait pas que YuyuFrame existe. Il envoie ses résultats au client local via le protocole Minecraft normal.

### ClientWorld

`ClientWorld` est la représentation du monde côté client Minecraft — c'est l'objet que le renderer lit pour afficher ce que le joueur voit. YuyuFrame alimente `ClientWorld` depuis deux sources : le serveur intégré local pour les quadrants simulés par ce pair, et les deltas des pairs reçus via libp2p pour tous les autres quadrants. Le renderer ne voit qu'un monde cohérent et complet, sans savoir que certaines parties viennent de machines distantes.

### Le relay server

Le relay server est le seul composant d'infrastructure externe. Son rôle est strictement limité au signaling initial : permettre à deux pairs de se trouver sur internet et d'échanger leurs adresses pour établir une connexion directe. Une fois la connexion établie entre les pairs, le relay n'est plus impliqué dans le flux de données de jeu. Il est conçu pour être extrêmement léger — quelques kilooctets échangés au démarrage d'une session, puis silence.

---

## Ce qui circule sur le réseau

### Les deltas de simulation

Les deltas constituent la quasi-totalité du trafic de YuyuFrame. Un delta contient les changements de blocs, les mouvements d'entités, les spawns, les morts, et les mises à jour de tile entities produits par la simulation locale d'un pair pendant un tick. Si rien n'a changé, le delta est vide et rien n'est envoyé.

En conditions normales de jeu, le trafic est de l'ordre de quelques kilooctets par seconde par pair. Les pics surviennent lors d'explosions ou de farm à mobs denses, mais restent dans des limites confortables pour une connexion résidentielle moderne.

### Le ghost layer

Le ghost layer est un échange léger entre pairs adjacents — ceux qui partagent une frontière entre leurs quadrants. Chaque pair envoie l'état de ses deux blocs de bordure à ses voisins une fois par tick. Les voisins utilisent ces données comme contexte pour le pathfinding et la collision de leurs entités, sans simuler ces blocs. Le ghost layer ne modifie pas l'état du monde — c'est une lecture seule.

### Le signaling initial

Au démarrage d'une session, les pairs échangent quelques dizaines d'octets via le relay server pour se trouver, puis s'envoient un snapshot des chunks autour de leur position respective pour synchroniser leur état initial. Ce snapshot, en format Anvil natif Minecraft, est le seul transfert de données volumineuses — il n'a lieu qu'une fois par pair par session.

---

## Gestion des cas limites

### Joueurs dans la même zone

Quand plusieurs joueurs sont proches, leurs zones de simulation se chevauchent. L'ownership engine attribue chaque quadrant au joueur le plus proche, produisant une distribution équitable et continue. Les quadrants aux frontières sont attribués de façon déterministe sans communication entre pairs.

### Physique aux frontières de quadrants

Les phénomènes physiques qui traversent une frontière — eau qui coule, redstone qui propage, piston qui pousse — sont traités par l'ownership engine physic-aware. Avant chaque tick, il détecte ces phénomènes et étend temporairement l'ownership du pair concerné pour englober toute la zone affectée. Un seul pair simule la chaîne physique complète. Les autres reçoivent le résultat via delta.

### Déconnexion d'un pair

Quand un pair se déconnecte, ses quadrants sont redistribués aux pairs les plus proches par l'ownership engine. Le nouveau propriétaire charge le dernier état connu depuis le disque local. Les modifications effectuées par le pair déconnecté depuis la dernière sauvegarde sont perdues, exactement comme lors d'un crash de serveur classique. Une fenêtre de grâce de trente secondes permet au pair de se reconnecter sans redistribution.

### Pair seul dans une zone

Un pair seul simule l'intégralité des chunks dans son rayon de vue — comportement identique à un monde solo. YuyuFrame n'ajoute aucun overhead de simulation dans ce cas.

---

## Performance

### Distribution de la charge

La charge de simulation est distribuée proportionnellement entre les pairs actifs. Avec des joueurs répartis dans des zones distinctes, chacun simule approximativement un N-ième du monde actif, N étant le nombre de joueurs. L'efficacité de la distribution dépend de la dispersion géographique des joueurs — des joueurs groupés bénéficient moins de la distribution que des joueurs dispersés.

### Latence perçue

La latence entre deux pairs détermine le délai avec lequel les actions de l'un sont visibles chez l'autre. Avec une latence de cinquante millisecondes, un changement est visible en un à deux ticks — imperceptible. YuyuFrame interpole les positions des entités entre les deltas reçus pour lisser les mouvements même en cas de latence variable.

### Bande passante

La bande passante consommée par YuyuFrame est très inférieure à celle d'un serveur Minecraft classique. La compression différentielle par delta, combinée à l'absence totale de trafic quand rien ne change, produit un flux réseau minimal. Une connexion fibre résidentielle standard supporte confortablement une session YuyuFrame avec dix pairs actifs.

---

## Structure du projet

```
yuyuframe/
├── agent/                     ← Java Agent + Mixin standalone
│   ├── bootstrap/             ← premain, détection version, init Mixin
│   ├── mixins/                ← hooks sur serveur intégré et ClientWorld
│   └── jni/                   ← pont vers le cœur Rust
│
├── rust-core/                 ← Cœur Rust (bibliothèque native)
│   ├── ownership/             ← Ownership engine + physic-aware
│   ├── delta/                 ← Capture, sérialisation, diffusion
│   ├── network/               ← libp2p, streams, file d'attente
│   └── jni/                   ← Interface JNI exposée à Java
│
├── relay/                     ← Relay server (signaling uniquement)
│   └── src/                   ← Serveur léger, quelques centaines de lignes
│
├── docs/
│   ├── README_couche1_connectivite_p2p.md
│   ├── README_couche2_ownership_engine.md
│   ├── README_couche3_v2_simulation_et_delta.md
│   ├── README_couche4_border_sync.md
│   └── README_aot_precompilation.md
│
└── README.md                  ← Ce fichier
```

---

## Roadmap

### Phase 1 — Connectivité P2P (couche 1)
Établir des connexions libp2p fiables entre deux pairs sur internet, avec NAT traversal et relay de secours. Vérification : deux instances Minecraft se connectent et maintiennent un tunnel stable.

### Phase 2 — Ownership engine (couche 2)
Implémenter l'attribution déterministe des quadrants par distance minimale. Vérification : deux instances calculent la même table d'ownership à partir des mêmes positions, sans communication.

### Phase 3 — Simulation et deltas (couche 3)
Instrumenter le serveur intégré pour la simulation sélective, capturer les deltas en fin de tick, les diffuser, les recevoir et les injecter dans `ClientWorld`. Vérification : un bloc posé sur une instance apparaît sur l'autre.

### Phase 4 — Border sync (couche 4)
Implémenter l'ownership physic-aware pour les fluides et la redstone, et le ghost layer pour les entités aux frontières. Vérification : l'eau coule naturellement à travers les frontières de quadrants.

### Phase 5 — Stabilisation et optimisation
Tests multi-instances prolongés, détection et gestion des cas limites, implémentation optionnelle de la précompilation AOT pour les releases.

---

## Documentation détaillée

Chaque couche est documentée en détail dans un fichier dédié :

- **Couche 1** — Connectivité P2P, libp2p, signaling, NAT traversal
- **Couche 2** — Ownership engine, quadrants, règle déterministe, handoff
- **Couche 3** — Java Agent, Mixin standalone, cœur Rust, tick loop, deltas, ClientWorld
- **Couche 4** — Border sync, ownership physic-aware, ghost layer, transfer d'entités
- **AOT** — Précompilation optionnelle pour optimiser le démarrage

---

*YuyuFrame — Architecture P2P Minecraft basée sur la confiance mutuelle.*  
*Version : 0.1.0-alpha*