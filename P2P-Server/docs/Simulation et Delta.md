# Couche 3 — Simulation locale et synchronisation des deltas

> Troisième couche de l'architecture YuyuFrame P2P Minecraft.  
> Objectif : chaque pair simule uniquement les quadrants qui lui sont attribués, capture les changements produits, les diffuse aux autres pairs, et intègre les changements reçus directement dans sa représentation locale du monde — sans intermédiaire.

---

## Le choix architectural fondamental

La couche 3 repose sur un principe directeur qui conditionne toute son architecture : **les deltas des pairs sont injectés directement dans `ClientWorld`, la représentation locale du monde côté client Minecraft, sans passer par le serveur intégré comme intermédiaire.**

Ce choix mérite d'être expliqué en profondeur car il détermine la structure de chaque composant.

Dans Minecraft, `ClientWorld` est l'objet qui contient l'état du monde tel que le client le connaît et l'affiche. C'est dans `ClientWorld` que les paquets réseau venant d'un serveur classique sont appliqués — changements de blocs, mouvements d'entités, spawns, morts. Le renderer Minecraft lit `ClientWorld` et affiche ce qu'il contient. Du point de vue du renderer, la source des informations contenues dans `ClientWorld` est complètement transparente.

Dans un serveur classique, un seul flux alimente `ClientWorld` : les paquets TCP venant du serveur distant. Dans YuyuFrame, deux flux alimentent `ClientWorld` en parallèle : les paquets du serveur intégré local pour les quadrants que ce pair simule, et les deltas des pairs reçus via libp2p pour les quadrants que les autres pairs simulent. Ces deux flux convergent vers le même point et sont traités de façon identique par `ClientWorld`. Le renderer ne voit qu'un monde cohérent, sans savoir que certaines parties viennent de sources différentes.

L'alternative aurait été d'utiliser le serveur intégré comme intermédiaire : recevoir les deltas des pairs, les appliquer dans l'état interne du serveur intégré, laisser le serveur intégré générer les paquets correspondants, et laisser le client les recevoir via le protocole normal. Cette approche introduit deux états potentiellement désynchronisés — l'état du serveur intégré et l'état de `ClientWorld` — avec tout ce que cela implique en termes de complexité de gestion et de surface de bugs. L'approche retenue supprime cet intermédiaire et traite le problème à son niveau naturel.

---

## Vue d'ensemble des composants

La couche 3 est constituée de cinq composants distincts qui collaborent à chaque tick. Chacun a un rôle précis et délimité, et aucun n'empiète sur le domaine des autres.

Le **Java Agent** est le point d'entrée dans la JVM. Il s'exécute avant Minecraft et installe tous les autres composants dans l'environnement d'exécution. Sans lui, rien ne peut s'insérer dans la JVM de façon propre et non intrusive.

**Mixin standalone** est le mécanisme d'instrumentation du bytecode Minecraft. Il installe les hooks sur les méthodes critiques du serveur intégré et du client, sans modifier les fichiers Minecraft sur le disque. C'est lui qui permet aux autres composants de s'exécuter au bon moment dans le cycle de vie de Minecraft.

Le **cœur Rust** est le cerveau du système P2P. Il gère la communication réseau entre pairs, la sérialisation et désérialisation des deltas, la file d'attente des deltas entrants, et le pont vers Java via JNI. Il est chargé comme bibliothèque native par le Java Agent et tourne dans le même processus que Minecraft.

Le **serveur intégré** est le moteur de simulation de Minecraft, celui qui tourne nativement quand on ouvre un monde solo. Son rôle dans YuyuFrame est strictement limité : simuler uniquement les quadrants attribués à ce pair par l'ownership engine. Il ne sait rien du P2P, ne connaît pas l'existence des autres pairs, et n'est jamais utilisé comme intermédiaire pour les deltas reçus.

**ClientWorld** est le point de convergence de tout le système. C'est l'objet Java qui représente l'état du monde côté client, alimenté à la fois par le serveur intégré local et par les deltas des pairs. Le renderer Minecraft lit exclusivement depuis `ClientWorld` — c'est son unique source de vérité pour tout ce qu'il affiche.

---

## Le Java Agent — bootstrap de l'environnement

### Rôle précis

Le Java Agent est le premier code de YuyuFrame à s'exécuter dans le processus Minecraft. Sa méthode `premain` est appelée par la JVM avant le démarrage de Minecraft, ce qui lui confère un accès privilégié à la machine virtuelle dans son état vierge — avant que les classes Minecraft ne soient chargées, avant que le launcher n'initialise ses composants, avant tout.

Son rôle est exclusivement celui d'un bootstrapper. Il ne contient aucune logique métier, aucune logique de simulation, aucune logique réseau. Il prépare l'environnement pour les autres composants et disparaît fonctionnellement une fois cette préparation terminée.

### Les quatre opérations du bootstrap

La première opération est la détection de la version Minecraft. Le Java Agent lit le manifeste du JAR Minecraft pour identifier la version exacte en cours d'exécution. Cette information est nécessaire pour toutes les opérations suivantes, car les mappings d'obfuscation, les noms de classes et de méthodes internes, et potentiellement le comportement de certaines APIs varient entre versions.

La deuxième opération est le chargement des mappings MojMap. Minecraft est distribué avec un bytecode obfusqué : les noms lisibles comme `ServerWorld` ou `ClientWorld` sont remplacés par des identifiants courts dans la version compilée distribuée. Pour que Mixin puisse cibler les bonnes méthodes, il a besoin d'une table de correspondance entre les noms lisibles et les noms obfusqués réels. Ces tables — les MojMap, publiées par Mojang pour chaque version — sont embarquées dans le JAR de YuyuFrame pour les versions supportées et chargées dynamiquement au démarrage selon la version détectée.

La troisième opération est l'initialisation de Mixin standalone. Le Java Agent initialise la bibliothèque Mixin, lui fournit l'objet `Instrumentation` reçu de la JVM (nécessaire pour que Mixin puisse installer ses transformateurs de bytecode), les mappings chargés à l'étape précédente, et la liste des configurations Mixin de YuyuFrame. À partir de ce moment, Mixin est prêt à intercepter le chargement de n'importe quelle classe Minecraft.

La quatrième opération est le chargement de la bibliothèque native Rust. Le Java Agent extrait le fichier binaire approprié à la plateforme (`.so` sur Linux, `.dll` sur Windows, `.dylib` sur macOS) depuis les ressources embarquées dans le JAR, l'écrit dans un répertoire temporaire, et le charge via `System.load`. À partir de ce moment, le cœur Rust est opérationnel en mémoire et peut être appelé depuis Java via JNI.

### Ce que le Java Agent ne fait pas

Le Java Agent ne modifie aucun fichier Minecraft sur le disque. Toutes ses opérations sont en mémoire, dans la JVM courante. Une fois la JVM arrêtée, aucune trace de son passage n'est visible dans l'installation Minecraft.

Il ne communique pas avec les autres pairs. Il n'a aucune connaissance de la topologie P2P, des quadrants, des deltas ou de la simulation. Ces responsabilités appartiennent aux autres composants.

Il ne s'exécute qu'une seule fois par session de jeu, au démarrage. Pendant les vingt heures de jeu qui suivent, le Java Agent ne fait rien — ses transformateurs Mixin sont installés, sa bibliothèque native est chargée, et son travail est terminé.

---

## Mixin standalone — l'instrumentation du bytecode

### Rôle précis

Mixin standalone est la bibliothèque qui permet d'insérer du code dans le bytecode de Minecraft au moment du chargement des classes. Contrairement à ASM qui opère au niveau des instructions individuelles, Mixin opère à un niveau sémantique : on décrit où insérer du code en termes de méthodes et de points logiques dans leur exécution, pas en termes d'offsets d'instructions dans le bytecode brut. Cette abstraction rend les hooks robustes aux changements de structure interne entre versions de Minecraft.

Mixin standalone fonctionne de façon identique à Mixin via Fabric — la syntaxe des annotations, les types de points d'injection, le comportement des hooks sont exactement les mêmes. La seule différence est dans le bootstrap, géré par le Java Agent plutôt que par Fabric Loader.

### Les hooks installés côté serveur intégré

Le premier hook côté serveur est installé sur la méthode principale de tick du serveur intégré. Ce hook s'exécute au tout début de chaque tick, avant que la simulation ne commence. Il communique avec le cœur Rust via JNI pour récupérer la liste des quadrants owné par ce pair depuis l'ownership engine, et configure le gestionnaire de chunks du serveur intégré pour ne ticker que ces quadrants. Ce hook est également le moment où les deltas accumulés depuis le tick précédent sont récupérés du cœur Rust et préparés pour injection.

Le deuxième hook côté serveur est installé sur le gestionnaire de ticking des chunks. Pour chaque chunk candidat à la simulation, ce hook vérifie si ce chunk contient au moins un quadrant dans la liste des quadrants owné. Si aucun quadrant n'est owné, la simulation de ce chunk est empêchée. Si au moins un quadrant est owné, la simulation est autorisée.

Le troisième hook côté serveur est installé à la fin du tick de simulation. Il collecte tous les changements produits par la simulation — les changements de blocs que le serveur intégré a enregistrés pour les envoyer aux clients, les mouvements d'entités, les spawns et les morts — et les transmet au cœur Rust via JNI pour construction du delta et diffusion.

### Les hooks installés côté client

Le hook côté client est installé sur les méthodes d'application de paquets dans `ClientWorld`. Minecraft possède déjà des méthodes bien définies pour appliquer des changements à `ClientWorld` depuis le réseau — `handleBlockUpdate`, `addEntity`, `removeEntity`, et leurs équivalents pour les différentes catégories de changements. Ces méthodes sont conçues pour être appelées depuis le thread réseau quand des paquets arrivent d'un serveur distant.

Le hook de YuyuFrame crée un second point d'appel pour ces mêmes méthodes. Au lieu d'un paquet réseau, c'est un delta désérialisé provenant du cœur Rust qui déclenche l'appel. Les méthodes `ClientWorld` reçoivent exactement les mêmes arguments dans les deux cas — elles ne distinguent pas la source. Le résultat est identique : `ClientWorld` est mis à jour, le renderer voit les changements au prochain frame.

### Ce que Mixin ne fait pas

Mixin ne contient pas de logique métier. Il ne sait pas ce qu'est un delta, ne connaît pas l'ownership engine, ne communique pas avec les autres pairs. Il est un outil de plomberie pure : au bon moment, il appelle le bon code. Le code qu'il appelle appartient aux autres composants.

---

## Le cœur Rust — le cerveau P2P

### Rôle précis

Le cœur Rust est la bibliothèque native chargée via JNI par le Java Agent. Il est le composant central de YuyuFrame, celui autour duquel tous les autres gravitent. Il ne touche pas directement à Minecraft — il ne lit pas le monde Minecraft, ne modifie pas de blocs, ne déplace pas d'entités. Toutes ses interactions avec Minecraft passent par le pont JNI vers les composants Java.

Son domaine est exclusivement la logique P2P et la gestion des deltas : qui simule quoi, comment le résultat de la simulation voyage d'un pair à l'autre, et dans quel ordre les changements reçus doivent être appliqués.

### L'ownership engine

L'ownership engine est décrit en détail dans la documentation de la couche 2. Dans le contexte de la couche 3, son rôle est de fournir à chaque début de tick la liste des quadrants que ce pair doit simuler. Cette liste est transmise au hook Mixin côté serveur intégré via JNI, qui l'utilise pour configurer le ticking sélectif des chunks.

L'ownership engine est également consulté lors de la capture de delta, pour filtrer les changements produits par la simulation et ne conserver que ceux qui appartiennent aux quadrants owné. Il est enfin consulté lors de la détection des transferts d'entités — quand une entité franchit une frontière entre deux quadrants owné par des pairs différents.

### La gestion des deltas sortants

Quand le hook Mixin de fin de tick transmet les changements produits par la simulation, le cœur Rust les reçoit sous forme de structures Java sérialisées dans un buffer JNI. Il les désérialise, les filtre via l'ownership engine pour ne conserver que les changements dans les quadrants owné, et les condense dans une structure de delta.

La sérialisation du delta en format binaire compact est réalisée intégralement en Rust. Le format est propriétaire et optimisé pour les besoins de YuyuFrame : coordonnées relatives, états de blocs sur 16 bits, positions d'entités en virgule fixe. Si le delta est vide, aucune sérialisation n'a lieu et aucun paquet n'est produit.

Le delta sérialisé est envoyé à chaque pair connecté via les streams libp2p établis par la couche 1. L'envoi est asynchrone et non-bloquant — le cœur Rust place le paquet dans le buffer d'envoi et reprend immédiatement sans attendre de confirmation.

### La gestion des deltas entrants

La réception des deltas des pairs est gérée par des tâches Tokio qui tournent en parallèle de tout le reste. Chaque stream libp2p entrant a sa propre tâche de lecture. Quand un delta arrive, il est désérialisé et placé dans une file d'attente thread-safe indexée par tick_id et peer_id.

Au début de chaque tick, le hook Mixin interroge le cœur Rust via JNI pour récupérer les deltas dont le tick_id est dans la fenêtre temporelle acceptable. Le cœur Rust extrait ces deltas de la file, les sérialise dans un buffer JNI, et les retourne au hook Mixin qui les injecte dans `ClientWorld`.

### Le pont JNI

Le pont JNI est la membrane entre la JVM et le cœur Rust. Il expose un ensemble minimal de fonctions appelables depuis Java : une fonction pour transmettre le delta capturé en fin de tick, une fonction pour récupérer les deltas accumulés en début de tick, une fonction pour notifier l'ownership engine d'un mouvement de joueur, et une fonction pour initier ou terminer une session P2P.

Le transfert de données à travers ce pont utilise des buffers directs (direct ByteBuffer) qui partagent la mémoire entre la JVM et Rust sans copie. Cette technique élimine l'overhead habituellement associé aux appels JNI pour les gros transferts de données.

### Ce que le cœur Rust ne fait pas

Le cœur Rust ne lit pas le monde Minecraft directement. Il ne sait pas quels blocs se trouvent à quelles coordonnées. Il ne connaît pas les entités présentes dans le monde. Toutes ces informations lui arrivent via le pont JNI depuis les hooks Mixin, sous forme de données déjà condensées et prêtes à être transmises.

Il ne prend pas de décisions d'affichage. Il ne sait pas ce qui est rendu à l'écran, ni même ce qui est contenu dans `ClientWorld`. Son rôle s'arrête à la livraison des deltas aux hooks Mixin — ce que ces hooks en font avec `ClientWorld` ne le concerne pas.

---

## Le serveur intégré — un moteur de simulation délimité

### Rôle précis et limité

Le serveur intégré de Minecraft est le moteur de simulation natif qui tourne normalement en mode solo. Dans YuyuFrame, son rôle est strictement délimité : **simuler uniquement les quadrants attribués à ce pair, et envoyer les résultats à son propre client local via le protocole Minecraft normal.**

C'est tout. Il ne sait pas qu'il existe d'autres pairs. Il ne connaît pas le concept de delta P2P. Il ne reçoit jamais les deltas des autres pairs et ne les traite jamais. Il est utilisé comme un moteur de simulation puissant et éprouvé pour la portion du monde qui appartient à ce pair — et pour rien d'autre.

### Pourquoi il n'est pas utilisé comme intermédiaire

La tentation naturelle serait d'utiliser le serveur intégré comme point de passage pour les deltas reçus des pairs : les appliquer dans son état interne, le laisser générer les paquets correspondants, et laisser le client les recevoir normalement. Cette approche a été explicitement rejetée.

Le serveur intégré maintient son propre état interne du monde, séparé de `ClientWorld`. Si on lui applique des changements qui n'ont pas été produits par sa propre simulation, son état interne diverge de ce que `ClientWorld` contient réellement. Cette divergence crée deux sources de vérité conflictuelles : le serveur intégré pense que le monde est dans l'état A, `ClientWorld` contient l'état B. La physique simulée par le serveur intégré au tick suivant part de l'état A et produit des résultats incorrects. Les bugs qui émergent de cette désynchronisation sont subtils, difficiles à reproduire et difficiles à déboguer.

En cantonnant le serveur intégré strictement aux quadrants owné et en injectant les deltas des pairs directement dans `ClientWorld`, on élimine cette dualité. L'état interne du serveur intégré est toujours cohérent avec ce qu'il a simulé. `ClientWorld` contient l'état complet du monde visible, alimenté de deux sources claires et non conflictuelles.

### La simulation sélective

Le hook Mixin installé sur le gestionnaire de ticking des chunks empêche le serveur intégré de ticker les chunks qui ne sont pas dans la liste des quadrants owné. Du point de vue du serveur intégré, ces chunks sont traités comme des chunks hors du rayon de vue — ils existent en mémoire (leurs données sont chargées pour que la géographie soit accessible), mais aucun calcul de simulation n'est effectué sur eux.

Cette distinction — chunk chargé mais non-tické — est native dans Minecraft. Le serveur la gère correctement sans modification du moteur. Les entités qui se trouvent dans des chunks non-tickés sont figées, leur pathfinding est suspendu, leur état de santé ne régresse pas. Exactement ce qu'on veut : les entités des quadrants des autres pairs sont gelées localement, leur état étant mis à jour exclusivement par les deltas reçus.

### Ce que le serveur intégré ne fait pas

Le serveur intégré n'envoie ses paquets qu'à son propre client local. Il n'envoie rien aux autres pairs — cette responsabilité appartient au cœur Rust. Il ne reçoit rien des autres pairs — cette responsabilité appartient aux hooks Mixin côté client.

---

## ClientWorld — le point de convergence

### Rôle précis

`ClientWorld` est l'objet Java qui représente l'état du monde du point de vue du client Minecraft. C'est la structure de données que le renderer lit pour savoir quels blocs afficher à quelles coordonnées, quelles entités positionner où, quel état de santé afficher dans la barre de vie. Tout ce que le joueur voit à l'écran provient de `ClientWorld`.

Dans YuyuFrame, `ClientWorld` reçoit des mises à jour de deux sources distinctes.

La première source est le serveur intégré local. Quand le serveur intégré tick les quadrants owné et que des changements se produisent, il génère des paquets Minecraft et les envoie au client via la connexion locale (loopback). Le client reçoit ces paquets et les applique à `ClientWorld` via son pipeline de traitement de paquets normal — le même pipeline qui fonctionnerait avec un serveur distant.

La seconde source est le hook Mixin côté client, qui reçoit les deltas des pairs depuis le cœur Rust et appelle directement les méthodes d'application de changements de `ClientWorld`. Ces méthodes — `handleBlockUpdate`, `addEntity`, `removeEntity` et leurs équivalents — sont exactement les mêmes que celles appelées lors du traitement des paquets du serveur intégré. `ClientWorld` ne distingue pas les deux sources.

### Pourquoi ClientWorld est le bon niveau d'abstraction

`ClientWorld` a été conçu pour recevoir des ordres d'un serveur et les appliquer sans validation. Il fait confiance à ce qu'on lui injecte — c'est son comportement normal vis-à-vis d'un serveur. Cette confiance aveugle, qui serait un problème dans un contexte compétitif, est exactement ce dont YuyuFrame a besoin dans un contexte de jeu entre amis basé sur la confiance mutuelle.

En injectant directement dans `ClientWorld`, YuyuFrame utilise le bon niveau d'abstraction : ni trop bas (manipuler les buffers de rendu OpenGL), ni trop haut (passer par le serveur intégré comme intermédiaire inutile). `ClientWorld` est la représentation unifiée du monde telle que le client Minecraft la comprend, et c'est précisément ce qu'on veut mettre à jour.

---

## Le tick loop complet — déroulement séquentiel

### Architecture du tick dans YuyuFrame

Un tick dans YuyuFrame se déroule en plusieurs phases séquentielles. Il est important de comprendre que certaines de ces phases concernent le thread du serveur intégré, d'autres concernent le thread du client, et d'autres encore concernent les tâches Tokio du cœur Rust. Ces threads coexistent dans le même processus mais s'exécutent en parallèle.

### Phase 1 — Injection des deltas entrants (début de tick, thread client)

Au tout début de chaque tick, avant que la simulation locale ne commence, le hook Mixin côté client interroge le cœur Rust via JNI pour récupérer les deltas accumulés depuis le dernier tick. Le cœur Rust extrait de sa file d'attente tous les deltas dont le tick_id est dans la fenêtre temporelle acceptable et les retourne au hook.

Le hook applique ces deltas à `ClientWorld` dans l'ordre de leur tick_id. Pour chaque changement de bloc dans un delta, il appelle `ClientWorld.handleBlockUpdate` avec la position et le nouvel état. Pour chaque entité déplacée, il appelle les setters de position et d'état sur l'entité correspondante dans `ClientWorld`. Pour chaque spawn, il crée l'entité et l'ajoute à `ClientWorld`. Pour chaque mort, il retire l'entité de `ClientWorld`.

Cette phase se termine avant que la simulation locale ne commence, garantissant que le serveur intégré voit un état du monde incluant les dernières informations reçues des pairs quand il commence à calculer ses propres quadrants.

### Phase 2 — Configuration de la simulation locale (début de tick, thread serveur)

Le hook Mixin côté serveur interroge le cœur Rust via JNI pour obtenir la liste des quadrants owné ce tick depuis l'ownership engine. Cette liste est transmise au gestionnaire de chunks du serveur intégré, qui configure quels chunks seront tickés et lesquels resteront gelés.

### Phase 3 — Simulation Minecraft (pendant le tick, thread serveur)

Le serveur intégré exécute son tick normal sur les chunks configurés comme tickables. Tout le moteur de simulation de Minecraft s'applique sans modification. Le serveur intégré génère les paquets correspondant aux changements produits et les envoie au client local. Le client les reçoit via son pipeline normal et met à jour `ClientWorld` — ce sont les changements des quadrants owné par ce pair.

### Phase 4 — Capture du delta (fin de tick, thread serveur)

Le hook Mixin de fin de tick collecte tous les changements produits par la simulation dans les quadrants owné. Il transmet ces changements au cœur Rust via JNI. Le cœur Rust filtre via l'ownership engine, construit le delta, le sérialise, et le place dans les buffers d'envoi des streams libp2p vers chaque pair connecté.

### Phase 5 — Envoi réseau (asynchrone, tâches Tokio)

Les tâches Tokio du cœur Rust envoient les deltas en attente dans les buffers d'envoi vers chaque pair. Cette opération est entièrement asynchrone et ne bloque pas les threads du serveur intégré ou du client.

### Phase 6 — Réception réseau (asynchrone, tâches Tokio)

En parallèle de tout le reste, les tâches Tokio de réception lisent les streams libp2p entrants. Chaque delta reçu est désérialisé et placé dans la file d'attente indexée par tick_id et peer_id. Cette file sera consultée lors de la phase 1 du tick suivant.

### État final après un tick

À l'issue de ce cycle, `ClientWorld` contient un état cohérent du monde complet : les quadrants owné ont été mis à jour par la simulation locale du serveur intégré (phases 3), les quadrants des pairs ont été mis à jour par les deltas reçus (phase 1 au début du cycle suivant). Le renderer affiche `ClientWorld` en continu — il voit un monde cohérent qui évolue à chaque tick.

---

## La capture du delta — ce qui est collecté

### Les changements de blocs

Minecraft maintient en interne, pour chaque chunk, une liste de changements de blocs produits dans le tick courant. Cette liste existe nativement dans le serveur intégré et est normalement consommée pour générer les paquets `Block Update` vers les clients. Le hook Mixin de YuyuFrame intercepte cette liste avant qu'elle ne soit consommée, la filtre pour ne garder que les blocs dans les quadrants owné, et transmet le résultat au cœur Rust.

Un changement de bloc dans le delta représente une position tridimensionnelle et un état de bloc final. Il ne représente pas l'action qui a produit ce changement — pas "un piston a poussé ce bloc", mais "ce bloc est maintenant dans cet état". Cette distinction est fondamentale : le pair qui reçoit le delta n'a pas à rejouer la physique qui a produit le changement, il applique directement le résultat.

### Les mouvements d'entités

Chaque entité dans les quadrants owné qui a changé de position, d'orientation, d'état de santé ou d'effets actifs depuis le tick précédent génère une entrée dans le delta. Les entités immobiles et dont l'état n'a pas changé ne génèrent aucune entrée — c'est la principale optimisation de taille du delta en dehors des situations d'action intense.

### Les spawns et les morts

Quand un mob apparaît dans un quadrant owné — spawn naturel de nuit, spawn depuis un spawner, apparition due à un sort — ses données complètes sont incluses dans le delta : type d'entité, position, orientation, état de santé initial, équipement, effets actifs. Quand un mob meurt dans un quadrant owné, seul son UUID est inclus dans le delta pour signaler sa disparition.

### Les tile entities

Les blocs avec un état interne complexe — fourneaux dont la cuisson progresse, coffres dont le contenu change, enchanteurs dont le niveau d'expérience varie — génèrent des entrées séparées dans le delta. Leur sérialisation est plus volumineuse car elle inclut des données NBT structurées, mais leur fréquence de modification est faible comparée aux blocs simples et aux entités mobiles.

### L'optimisation du delta vide

Si aucun des quatre types de changements n'a produit de données dans les quadrants owné au cours du tick, le delta est vide. Dans ce cas, le cœur Rust ne construit aucun paquet et n'envoie aucune donnée sur le réseau. Cette optimisation est d'une importance capitale : dans une session de jeu calme où les joueurs explorent sans interagir intensément avec le monde, la grande majorité des ticks produisent des deltas vides. La bande passante effective est alors nulle, indépendamment du nombre de ticks par seconde.

---

## L'injection des deltas reçus dans ClientWorld

### Le principe de l'application directe

Quand le hook Mixin côté client reçoit les deltas des pairs depuis le cœur Rust, il les applique à `ClientWorld` via les mêmes méthodes que celles utilisées par le pipeline de traitement de paquets normal. Pour `ClientWorld`, ces appels sont indiscernables de ceux provenant du serveur intégré.

La différence fondamentale avec l'approche serveur intégré intermédiaire est l'absence de recalcul de physique. Quand on appelle `ClientWorld.handleBlockUpdate`, on lui fournit l'état final du bloc. `ClientWorld` met à jour sa représentation et notifie le renderer. Il n'évalue pas la physique de ce changement — il n't pas un moteur de simulation, c'est une représentation. La physique a été évaluée par le pair propriétaire du quadrant ; son résultat est transmis via le delta et appliqué directement.

### L'ordre d'application

Les deltas reçus d'un même tick de plusieurs pairs sont appliqués dans un ordre déterministe : d'abord par tick_id croissant, puis par peer_id en cas d'égalité de tick_id. Cet ordre garantit que deux exécutions de YuyuFrame avec les mêmes données d'entrée produisent le même état de `ClientWorld`.

Les entités sont traitées avant les blocs pour éviter des situations où un bloc change d'état dans une zone qu'une entité traverse dans le même delta, ce qui pourrait produire des artefacts visuels brefs si l'entité était rendue avant que le bloc ne soit mis à jour.

### La gestion des conflits locaux

Dans de rares cas, un delta reçu peut contenir un changement dans une zone que ce pair vient également de modifier dans son propre tick. Par exemple, si deux pairs construisent très proches l'un de l'autre et que leurs zones se chevauchent légèrement lors d'un changement d'ownership. Dans ce cas, le delta entrant prend la priorité sur l'état local pour la portion concernée — le pair propriétaire de ce quadrant a l'autorité sur son état. Ce comportement est cohérent avec le modèle de confiance de YuyuFrame : on fait confiance au pair propriétaire.

---

## Le transfer d'entité entre pairs

### Détection du croisement

Quand une entité mobile — mob, projectile, item au sol — se trouve dans les quadrants owné de ce pair et se déplace vers un quadrant owné par un autre pair, son croisement de frontière est détecté par le cœur Rust via l'ownership engine. Cette détection se produit lors de la phase de capture du delta, après la simulation du tick.

### Le protocole de transfer

Le pair qui perd l'entité l'inclut dans une catégorie spéciale du delta : les transfers. Cette catégorie contient l'état NBT complet de l'entité — position exacte, vecteur de vitesse, état de santé, effets actifs, inventaire pour les mobs porteurs. L'entité est retirée de la simulation locale immédiatement après la capture du delta.

Le pair qui reçoit le transfer extrait les données de l'entité et crée une nouvelle instance dans son `ClientWorld` et dans son propre serveur intégré, à la position exacte transmise. Dès le tick suivant, ce pair simule cette entité comme si elle avait toujours été dans ses quadrants.

Pendant le tick de transfer, l'entité n'est simulée par personne. Elle est visible dans `ClientWorld` dans son dernier état connu pendant exactement la durée de propagation réseau du delta — typiquement un à deux ticks, imperceptible dans des conditions réseau normales.

---

## La synchronisation de l'état initial

### Le problème du join

Quand un pair rejoint une session en cours, il n'a pas l'état actuel du monde que les autres pairs simulent depuis potentiellement plusieurs heures. Les deltas ne peuvent pas reconstruire cet état — ils représentent des changements incrémentaux, pas un état absolu.

### Le snapshot Anvil

Le protocole d'initialisation est un transfert de snapshot plutôt qu'un replay de deltas. Le pair hôte (celui qui simule les quadrants autour de la zone d'arrivée du nouveau pair) envoie les données de chunks dans le format de fichier régional natif de Minecraft — le format Anvil, des fichiers `.mca` contenant les sections de terrain encodées en NBT compressé.

Le nouveau pair reçoit ces données, les charge dans son serveur intégré comme si c'était des chunks sauvegardés localement, et initialise son `ClientWorld` à partir de cet état. Du point de vue de Minecraft, rien ne distingue ces chunks d'une sauvegarde locale.

### La génération procédurale partagée

Pour les chunks jamais visités par aucun pair — zones non explorées du monde — le nouveau pair les génère procéduralement lui-même. La graine du monde est partagée entre tous les pairs au moment de la création de la session. La génération procédurale de Minecraft étant déterministe à partir de la graine, chaque pair génère un terrain identique pour les mêmes coordonnées, sans qu'aucune donnée ne doive être échangée.

---

## Analyse de la bande passante

### Dans les conditions normales

La bande passante consommée par YuyuFrame en conditions normales de jeu est très faible. Un tick sans activité ne génère aucun trafic. Un tick avec un joueur qui marche et quelques mobs actifs génère un delta de l'ordre de cinquante à deux cents octets. À vingt ticks par seconde, cela représente un à quatre kilooctets par seconde — bien en dessous de la capacité de n'importe quelle connexion résidentielle moderne.

### Dans les conditions d'activité intense

Les explosions de TNT représentent le pic de bande passante le plus élevé : un grand nombre de blocs changent en un seul tick. Un delta d'explosion peut atteindre plusieurs dizaines de kilooctets selon la taille de l'explosion. Ce pic est court et ne se répète pas — la seconde suivant l'explosion, les deltas retombent à leur niveau normal.

Les farms à mobs denses génèrent un flux de deltas d'entités élevé mais prévisible : quelques kilooctets par seconde en continu pour des centaines d'entités actives. C'est le scénario de bande passante soutenue le plus élevé dans une utilisation normale de Minecraft.

### La comparaison avec un serveur classique

Un serveur Minecraft classique génère davantage de trafic qu'YuyuFrame pour un même scénario de jeu. La raison est que le serveur classique envoie ses paquets à chaque client connecté, y compris des paquets de confirmation et de synchronisation d'état qui n'existent pas dans le protocole de delta de YuyuFrame. La compression différentielle par delta, combinée à l'élimination des deltas vides, produit systématiquement moins de trafic que le protocole Minecraft standard.

---

## Ce que cette couche ne fait pas

La couche 3 ne gère pas la synchronisation des blocs aux frontières entre quadrants owné par des pairs différents. Cette responsabilité appartient à la couche 4. La couche 3 fournit à la couche 4 les données brutes de l'état des blocs frontière via l'ownership engine ; la couche 4 gère le protocole d'échange et l'injection en retour.

La couche 3 ne valide pas les deltas reçus des pairs. Elle les applique aveuglément à `ClientWorld`. La validation du contenu des deltas est un choix architectural explicitement absent dans le modèle de confiance de YuyuFrame.

La couche 3 ne gère pas la persistance du monde. La sauvegarde sur disque des chunks owné par ce pair est déléguée au mécanisme de sauvegarde natif du serveur intégré, conservé intact. Ce mécanisme sauvegarde périodiquement les chunks que le serveur intégré a en mémoire — exactement les chunks owné par ce pair.

---

## Interfaces entre composants internes

### Java Agent → Mixin standalone

Le Java Agent initialise Mixin standalone lors du bootstrap et lui fournit l'objet `Instrumentation`, les mappings MojMap, et la liste des configurations Mixin. Après l'initialisation, le Java Agent n'interagit plus avec Mixin. Mixin fonctionne de façon autonome, interceptant les chargements de classes et appliquant ses transformations.

### Java Agent → cœur Rust

Le Java Agent charge la bibliothèque native Rust via `System.load`. Après le chargement, le Java Agent n'interagit plus avec le cœur Rust. C'est Mixin qui appelle le cœur Rust via JNI.

### Mixin ↔ cœur Rust

Mixin appelle le cœur Rust via JNI à deux moments par tick : en début de tick pour récupérer les deltas entrants et la liste des quadrants owné, en fin de tick pour transmettre le delta capturé. Ces deux appels regroupent toutes les communications JNI du tick, minimisant l'overhead du passage de contexte.

### Mixin → ClientWorld

Mixin appelle les méthodes de `ClientWorld` pour appliquer les deltas reçus. Ces appels sont identiques à ceux effectués par le pipeline de traitement de paquets normal de Minecraft.

### Serveur intégré → ClientWorld

Le serveur intégré envoie ses paquets au client local via la connexion loopback. Le client les reçoit via son pipeline normal et les applique à `ClientWorld`. Cette interaction n'implique pas YuyuFrame — c'est le comportement natif de Minecraft en mode solo.

### Cœur Rust ↔ couche 1 (libp2p)

Le cœur Rust utilise le runtime libp2p établi par la couche 1 pour envoyer les deltas sérialisés aux pairs et recevoir les deltas entrants. La couche 1 est transparente pour la couche 3 — elle fournit des streams fiables et ordonnés, et la couche 3 y écrit et y lit des octets sans se préoccuper du transport sous-jacent.

### Cœur Rust ↔ couche 2 (ownership engine)

L'ownership engine est une partie intégrante du cœur Rust. Il n'y a pas de communication inter-composant ici — c'est un sous-module interne du cœur Rust, appelé directement par les autres parties du cœur lors de la configuration du tick et de la capture du delta.

---

*Documentation rédigée pour le projet YuyuFrame — Architecture P2P Minecraft basée sur la confiance.*  
*Couche précédente : [Couche 2 — Ownership Engine](./README_couche2_ownership_engine.md)*  
*Couche suivante : [Couche 4 — Border Sync](./README_couche4_border_sync.md)*