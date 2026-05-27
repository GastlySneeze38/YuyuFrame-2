# Précompilation AOT des classes Minecraft

> Document d'optimisation pour le projet YuyuFrame.  
> Objectif : éliminer le coût de transformation bytecode au démarrage en précalculant et mettant en cache les classes Minecraft instrumentées.

---

## Contexte et problème résolu

### Ce que fait le Java Agent au démarrage

Chaque fois que Minecraft démarre avec YuyuFrame actif, le Java Agent intercepte le chargement des classes Minecraft ciblées et les transforme via Mixin standalone avant qu'elles ne soient utilisées. Cette transformation — appliquer les hooks de la couche 3 sur les méthodes du serveur intégré et de `ClientWorld` — est effectuée intégralement en mémoire, pendant la phase de démarrage de la JVM, sur chaque lancement.

Le coût de cette transformation est mesuré en fractions de secondes sur du matériel moderne, et il est entièrement invisible une fois Minecraft lancé — le bytecode transformé est compilé nativement par le JIT de la JVM et s'exécute sans overhead pendant la session de jeu. Mais il existe, et il se répète à chaque lancement, même si ni Minecraft ni YuyuFrame n'ont changé depuis la dernière session.

### La prémisse de l'AOT

Si les deux entrées de la transformation — le bytecode original de Minecraft et le code des hooks YuyuFrame — sont identiques d'un lancement à l'autre, le résultat de la transformation est également identique. Il n'y a aucune raison de recalculer ce résultat à chaque démarrage. On peut le calculer une seule fois, le persister sur disque, et le réutiliser directement lors des lancements suivants.

C'est le principe de la précompilation AOT (Ahead-of-Time) dans le contexte de YuyuFrame : transformer les classes Minecraft ciblées en dehors du cycle de lancement, stocker le résultat dans un cache local, et court-circuiter la phase de transformation au démarrage en chargeant directement les classes déjà instrumentées depuis ce cache.

---

## Fonctionnement général

### Les deux modes de démarrage

YuyuFrame peut démarrer dans deux modes selon l'état du cache AOT.

Le premier mode est le mode de transformation à chaud, identique au comportement décrit dans la documentation du Java Agent. Le Java Agent intercepte le chargement des classes ciblées, Mixin standalone applique les transformations, et le bytecode transformé est utilisé pour la session en cours. Ce mode est utilisé lors du premier lancement après installation, lors d'une mise à jour de Minecraft ou de YuyuFrame, ou en l'absence de cache valide.

Le second mode est le mode AOT. Le Java Agent détecte la présence d'un cache valide au démarrage, charge les classes pré-transformées directement depuis le cache, et les injecte dans la JVM sans passer par Mixin. Le temps de démarrage est réduit à celui de la lecture de fichiers disque — une opération de quelques dizaines de millisecondes — plutôt qu'à celui d'une transformation de bytecode.

### Ce qui est mis en cache

Le cache ne contient pas Minecraft dans son intégralité. Seules les classes que YuyuFrame transforme sont mises en cache — typiquement quelques dizaines de classes sur les milliers que contient Minecraft. Toutes les classes non transformées continuent d'être chargées normalement depuis le JAR Minecraft original par le classloader habituel.

Chaque entrée du cache correspond à une classe transformée et contient le bytecode résultant de l'application des hooks Mixin sur le bytecode original. Ce bytecode est fonctionnellement identique à ce que le Java Agent produirait à chaud — c'est rigoureusement le même résultat, simplement persisté.

### L'identification du cache

Le cache est identifié par un ensemble de clés qui déterminent son état de validité. Ces clés sont calculées à partir des éléments qui, s'ils changent, nécessitent une retransformation.

La première clé est un hash du bytecode original de chaque classe transformée, extrait directement du JAR Minecraft. Si Mojang modifie une classe entre deux versions — même une modification mineure — son hash change et la classe correspondante dans le cache est invalidée.

La deuxième clé est un hash du JAR YuyuFrame lui-même, ou plus précisément des configurations Mixin et du code des hooks. Si YuyuFrame est mis à jour et que ses hooks changent, toutes les entrées du cache sont invalidées, car le résultat de la transformation serait différent même si le bytecode Minecraft n'a pas changé.

La troisième clé est la version de Mixin standalone utilisée par YuyuFrame. Une mise à jour de la bibliothèque Mixin peut modifier la façon dont elle génère le bytecode transformé, rendant le cache précédent invalide.

Ces trois clés sont combinées en un identifiant unique du cache. Si cet identifiant correspond à ce qui est stocké sur disque, le cache est valide et peut être utilisé. Dans le cas contraire, une nouvelle transformation à chaud est effectuée et le cache est reconstruit.

---

## Le processus de construction du cache

### Déclenchement de la construction

La construction du cache peut être déclenchée de deux façons.

La première est automatique : lors d'un lancement en mode de transformation à chaud, après que les classes ont été transformées et que Minecraft a démarré, YuyuFrame peut persister les classes transformées en arrière-plan. Cette approche ne ralentit pas le premier lancement puisque la persistance se fait après le démarrage de Minecraft, mais elle signifie que le premier lancement après installation ou mise à jour sera toujours en mode transformation à chaud.

La seconde est explicite : un outil de ligne de commande fourni avec YuyuFrame permet de déclencher la construction du cache avant le premier lancement. Cet outil est invoqué par le launcher au moment de l'installation de YuyuFrame ou de la mise à jour de Minecraft, dans une étape distincte visible par l'utilisateur. L'utilisateur voit une barre de progression pendant la transformation, et les lancements suivants bénéficient immédiatement du cache.

### Le déroulement de la transformation offline

Lors de la construction explicite du cache, l'outil de précompilation charge le JAR Minecraft depuis son emplacement habituel, instancie Mixin standalone dans le même état que lors d'un lancement normal — avec les mêmes mappings, les mêmes configurations, les mêmes hooks — et applique les transformations sur chaque classe ciblée dans un environnement isolé, sans lancer Minecraft.

Le résultat de chaque transformation est écrit dans le répertoire de cache avec ses métadonnées d'identification. L'ensemble du processus est idempotent : relancer la construction du cache produit exactement le même résultat si les entrées n'ont pas changé.

### L'environnement d'isolation

La transformation offline présente un défi particulier par rapport à la transformation à chaud : certains transformateurs Mixin peuvent avoir des dépendances sur des classes qui se chargent dans un ordre précis, ou sur des états initialisés par d'autres parties de Minecraft. Ces dépendances, si elles existent, rendraient la transformation offline impossible ou produiraient des résultats différents de la transformation à chaud.

La conception des hooks Mixin de YuyuFrame doit tenir compte de cette contrainte. Les hooks doivent être stateless du point de vue de la transformation — leur application ne doit dépendre que du bytecode de la classe cible et de la configuration Mixin, pas d'un état runtime externe. Cette contrainte est en réalité une bonne pratique de conception qui rend les hooks plus robustes et plus prévisibles dans tous les contextes.

---

## Intégration avec le Java Agent

### Le Java Agent comme orchestrateur

Le Java Agent reste le point d'entrée de YuyuFrame dans tous les cas. Son comportement change selon l'état du cache, mais il est toujours présent dans la ligne de lancement. Cette uniformité est importante pour la maintenabilité : il n'y a pas deux configurations différentes selon qu'on utilise le cache ou non.

Au démarrage, le Java Agent effectue la vérification du cache avant d'enregistrer ses transformateurs Mixin. Si le cache est valide, il enregistre un transformateur alternatif qui charge les classes depuis le cache au lieu d'appliquer Mixin. Si le cache est absent ou invalide, il enregistre les transformateurs Mixin habituels.

### Le transformateur de cache

Le transformateur de cache est un `ClassFileTransformer` dont la logique est simple : pour chaque classe ciblée par YuyuFrame, chercher dans le cache une version pré-transformée correspondant au hash du bytecode reçu. Si une entrée valide est trouvée, retourner le bytecode mis en cache. Sinon, retourner null pour laisser Mixin gérer la transformation à chaud.

Ce mécanisme de fallback garantit la robustesse du système : une entrée de cache corrompue ou incohérente ne bloque pas le démarrage. Le Java Agent retombe silencieusement sur la transformation à chaud pour la classe concernée et peut optionnellement reconstruire l'entrée de cache en arrière-plan.

### La cohabitation avec Mixin

Quand le cache est actif, Mixin standalone est tout de même initialisé par le Java Agent — il gère les mappings et la configuration nécessaires à la vérification de cohérence du cache. Mais ses transformateurs ne sont pas enregistrés dans la JVM, ou sont enregistrés avec une priorité inférieure au transformateur de cache. Les classes pré-transformées sont donc chargées depuis le cache sans passer par le pipeline Mixin.

Cette architecture préserve Mixin comme source de vérité pour la transformation : c'est toujours Mixin qui définit ce que le bytecode transformé doit contenir, même si ce bytecode a été calculé lors d'une session précédente.

---

## Structure du cache sur le disque

### Organisation des fichiers

Le cache est stocké dans un répertoire dédié de l'installation YuyuFrame, distinct du répertoire Minecraft et du répertoire de saves. Cette séparation est intentionnelle : le cache peut être supprimé sans affecter Minecraft ni les données de jeu, et il n'est pas synchronisé avec les sauvegardes.

Le répertoire de cache contient un fichier d'index et un ensemble de fichiers de bytecode. Le fichier d'index est un document structuré qui associe chaque nom de classe transformée à son hash d'entrée (bytecode original), son hash de sortie (bytecode transformé), et la clé d'identification globale du cache sous lequel cette entrée a été produite. Les fichiers de bytecode contiennent le bytecode transformé brut, un fichier par classe.

### La taille du cache

Le cache ne contient que les classes transformées, qui représentent une fraction minuscule du bytecode total de Minecraft. Un cache complet pour une version de Minecraft donnée occupe quelques centaines de kilooctets — négligeable par rapport à la taille du JAR Minecraft lui-même, et invisible pour l'utilisateur en termes d'espace disque.

### La gestion des versions multiples

Un utilisateur peut avoir plusieurs profils Minecraft avec des versions différentes dans son launcher. YuyuFrame maintient un cache distinct par version de Minecraft, identifié par le numéro de version et la clé d'identification globale de YuyuFrame. Ces caches coexistent dans le répertoire de cache sans interférence.

Quand une ancienne version de Minecraft n'est plus utilisée depuis longtemps, son entrée de cache peut être nettoyée automatiquement selon une politique de rétention configurable — par défaut, les caches des versions non utilisées depuis plus de trente jours sont supprimés.

---

## Pertinence par phase de développement

### Pendant le développement de YuyuFrame

L'AOT est contre-productive en phase de développement actif. Chaque modification des hooks YuyuFrame invalide le cache et nécessite une reconstruction, ajoutant de la friction au cycle de développement. En développement, le mode de transformation à chaud est préférable : il reflète toujours l'état le plus récent des hooks sans étape intermédiaire.

Il est recommandé de désactiver l'AOT via une option de configuration pendant le développement, et de ne l'activer que pour les builds de release.

### Pour la release stable

L'AOT devient pertinent quand YuyuFrame est en phase de stabilisation, quand les hooks changent rarement entre deux sessions de jeu d'un même utilisateur. Dans ce cas, le coût récurrent de la transformation à chaud disparaît et chaque lancement bénéficie du cache.

Pour un utilisateur final qui utilise YuyuFrame avec la même version de Minecraft pendant plusieurs semaines, le cache est construit une fois lors de l'installation et reste valide jusqu'à une mise à jour. Le lancement de Minecraft avec YuyuFrame devient aussi rapide que le lancement de Minecraft sans YuyuFrame.

### Estimation du gain

Le gain de démarrage de l'AOT dépend du nombre de classes transformées et de la complexité des transformations Mixin. Dans le cas de YuyuFrame, qui cible quelques dizaines de classes avec des hooks relativement simples, le gain est de l'ordre de une à trois secondes sur le temps de démarrage total de Minecraft, qui est lui-même de l'ordre de vingt à quarante secondes selon la machine. Le gain est réel mais non spectaculaire — l'AOT est une optimisation de confort, pas un changement fondamental de performance.

---

## Limitations et cas particuliers

### Les classes dynamiquement générées

Certaines classes Minecraft peuvent être générées dynamiquement par d'autres mécanismes au moment du chargement — des classes proxy, des implémentations générées par réflexion. Ces classes ne peuvent pas être mises en cache car elles n'ont pas d'identité stable dans le JAR Minecraft. Si YuyuFrame doit transformer de telles classes, elles sont toujours traitées en mode transformation à chaud même quand le cache est actif.

### La cohérence entre installations

Le cache est spécifique à la machine sur laquelle il a été construit. Il n'est pas portable entre machines, même si elles ont la même version de Minecraft et la même version de YuyuFrame, car le bytecode transformé peut dépendre subtilement de la version de la JVM utilisée pour la transformation. Distribuer un cache pré-construit avec YuyuFrame serait risqué — une incompatibilité de JVM produirait des comportements imprévisibles.

Chaque installation construit son propre cache localement. C'est la garantie que le bytecode chargé dans la JVM de l'utilisateur est rigoureusement compatible avec son environnement d'exécution.

### La corruption du cache

Un fichier de cache corrompu — suite à une coupure de courant pendant l'écriture, un disque défaillant, ou une intervention manuelle — est détecté via la vérification d'intégrité du fichier d'index. Toute divergence entre le hash attendu et le hash calculé du bytecode mis en cache déclenche l'invalidation de l'entrée concernée et sa reconstruction lors du prochain lancement en transformation à chaud.

La robustesse face à la corruption est une propriété non-négociable du système de cache : une corruption silencieuse qui produirait un bytecode incorrect injecté dans Minecraft serait catastrophique et très difficile à diagnostiquer. Il vaut toujours mieux retomber sur la transformation à chaud que de charger un bytecode dont l'intégrité n'est pas garantie.

---

## Relation avec les autres composants de YuyuFrame

L'AOT est une optimisation orthogonale à l'architecture des quatre couches. Elle n'affecte pas le comportement de YuyuFrame pendant la session de jeu — la couche 1, la couche 2, la couche 3 et la couche 4 fonctionnent de façon identique que le bytecode instrumenté ait été chargé depuis le cache ou transformé à chaud. La seule différence est dans la phase de démarrage, avant que Minecraft ne soit opérationnel.

Cette orthogonalité en fait une optimisation sûre à ajouter en dernière phase de développement, sans risque d'interaction avec la logique de simulation, de distribution, ou de synchronisation.

---

*Documentation rédigée pour le projet YuyuFrame — Architecture P2P Minecraft basée sur la confiance.*  
*Implémentation recommandée : phase de stabilisation, après les quatre couches fonctionnelles.*