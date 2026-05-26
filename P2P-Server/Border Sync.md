# Couche 4 — Border Sync

> Quatrième et dernière couche de l'architecture YuyuFrame P2P Minecraft.  
> Objectif : garantir qu'aucun phénomène physique ni aucune entité ne soit brisé par la frontière entre deux quadrants owné par des pairs différents.

---

## Le problème que cette couche résout

Quand deux quadrants adjacents sont owné par deux pairs différents, chacun simule sa portion du monde indépendamment. Cette indépendance est précisément ce qui permet la distribution de la charge. Mais elle crée un problème structurel : certains phénomènes Minecraft ne connaissent pas de frontières. L'eau coule là où la gravité et les blocs adjacents le permettent, sans se soucier de savoir qui est responsable de la simulation des blocs qu'elle atteint. Un circuit de redstone propage ses signaux le long de ses fils conducteurs, qu'ils se trouvent dans un quadrant ou dans le suivant. Un piston pousse ce qui est devant lui, indépendamment de la ligne d'ownership.

Si chaque pair simule indépendamment sans mécanisme de coordination aux frontières, ces phénomènes se brisent au moment où ils franchissent une frontière d'ownership. L'eau s'arrête là où le quadrant s'arrête. Le signal redstone ne passe plus. Le piston pousse dans le vide.

La couche 4 résout ce problème. Son architecture repose sur une observation fondamentale : les blocs et les entités sont deux catégories de phénomènes fondamentalement différentes, et ces deux catégories méritent des solutions différentes.

---

## La distinction fondamentale : blocs et entités

### Les blocs sont déterministes

Les phénomènes physiques agissant sur les blocs dans Minecraft obéissent à des règles fixes et déterministes. L'eau coule selon un algorithme précis et reproductible. La redstone propage ses signaux selon des règles logiques invariables. Un piston pousse selon une mécanique rigoureuse. Le sable tombe selon des règles de gravité strictes.

Cette déterminisme a une conséquence directe pour YuyuFrame : si un seul pair simule l'intégralité d'un phénomène physique — depuis sa source jusqu'à son terme — le résultat est unique et correct. Il n'y a pas de désaccord possible, pas d'arbitrage nécessaire, pas de synchronisation à effectuer entre deux versions du même phénomène. La solution au problème de frontière pour les blocs est donc d'éviter que la frontière soit jamais traversée par un phénomène physique en cours de simulation — en assignant temporairement la zone d'effet complète d'un phénomène à un seul pair.

### Les entités sont non-déterministes

Les entités mobiles — mobs, projectiles, items au sol — sont fondamentalement différentes. Le comportement d'un zombie n'est pas entièrement déterministe : son pathfinding dépend d'un historique de décisions, d'états internes, de composantes pseudo-aléatoires. Deux pairs qui simulent le même zombie depuis le même état initial peuvent produire des trajectoires légèrement différentes au bout de quelques ticks, à cause de différences infimes dans l'ordre d'évaluation ou les arrondis numériques.

Pour les entités, il n'est donc pas possible d'assigner une "zone d'effet" à simuler par un seul pair — les entités traversent les frontières de façon continue et imprévisible. La solution est différente : donner à chaque pair le contexte géographique minimal dont ses entités ont besoin pour se déplacer correctement près des frontières, sans dupliquer la simulation des blocs adjacents.

---

## La solution pour les blocs : l'ownership engine physic-aware

### Principe général

L'ownership engine, décrit dans la couche 2, attribue les quadrants aux pairs selon la distance. Dans la couche 4, cet ownership engine acquiert une capacité supplémentaire : avant chaque tick de simulation, il détecte les phénomènes physiques actifs près des frontières d'ownership et étend temporairement l'ownership du pair concerné pour englober l'intégralité de la zone que ce phénomène pourrait affecter pendant ce tick.

Cette extension est dynamique et temporaire. Elle est recalculée à chaque tick selon l'état actuel du monde. Elle ne modifie pas l'ownership de base des quadrants — les quadrants restent assignés selon la règle de distance minimale. Elle modifie uniquement l'ensemble des blocs que chaque pair est autorisé à simuler ce tick, en ajoutant les blocs supplémentaires nécessaires pour que les phénomènes physiques actifs se terminent entièrement dans la zone d'un seul pair.

Le pair qui reçoit l'extension owne temporairement ces blocs supplémentaires pour la durée du tick. Le pair adjacent, qui aurait normalement owné ces blocs selon la règle de distance, ne les tique pas ce tick. Il reçoit le résultat via le delta normal de la couche 3 et met à jour sa représentation locale.

### Le scan de détection avant le tick

Au début de chaque tick, avant que la configuration de simulation ne soit transmise au serveur intégré, l'ownership engine effectue un scan des blocs actifs dans chaque quadrant owné qui sont proches d'une frontière avec un quadrant d'un autre pair. La zone de scan est limitée aux deux blocs les plus proches de chaque frontière — au-delà, les phénomènes du tick courant ne peuvent pas atteindre le quadrant adjacent.

Ce scan est local et peu coûteux. La grande majorité des blocs aux frontières sont statiques — de la pierre, de la terre, du bois — et ne déclenchent aucune extension d'ownership. Seuls les blocs qui correspondent à des phénomènes physiques actifs déclenchent une analyse plus approfondie.

Les catégories de blocs qui déclenchent le scan approfondi sont : les sources et blocs d'eau et de lave en cours de propagation, les fils de redstone, les répéteurs, les comparateurs, les blocs d'alimentation redstone, les pistons et pistons collants actifs, le sable et le gravier en chute libre, les rails alimentés, et les blocs de TNT amorcés.

---

## L'extension d'ownership pour les fluides

### La propagation des fluides dans Minecraft

L'eau et la lave se propagent dans Minecraft selon un algorithme de diffusion : depuis un bloc source, le fluide s'écoule vers les blocs adjacents qui sont à un niveau inférieur ou au même niveau si un chemin vers le bas existe. Chaque tick, un bloc de fluide peut s'étendre d'un bloc dans les directions horizontales et verticales selon des règles précises de niveau et d'obstruction.

### La détection du graphe de propagation

Quand l'ownership engine détecte un bloc de fluide à deux blocs ou moins d'une frontière, il construit le graphe de propagation potentielle de ce fluide pour le tick courant. Ce graphe est l'ensemble des blocs que le fluide pourrait atteindre en un seul tick selon les règles de propagation de Minecraft, en tenant compte des obstacles déjà présents dans le monde.

Ce calcul utilise les mêmes règles que le moteur Minecraft lui-même — l'ownership engine est en mesure d'anticiper où le fluide ira sans avoir à le simuler, simplement en analysant la topographie des blocs environnants. Si des blocs du graphe de propagation se trouvent dans le quadrant d'un pair adjacent, ces blocs sont temporairement inclus dans l'ownership du pair propriétaire de la source.

### La chaîne de propagation étendue

Si la source d'eau est dans le quadrant de A et que sa propagation peut atteindre des blocs dans le quadrant de B et potentiellement dans le quadrant de C, l'extension d'ownership inclut tous les blocs atteignables dans la chaîne complète. A simule l'intégralité de la propagation ce tick, produit un delta qui inclut tous les blocs modifiés y compris ceux dans les quadrants de B et C, et ces pairs reçoivent le résultat via le mécanisme de delta normal.

Cette approche garantit que la propagation des fluides est toujours simulée dans sa totalité par un seul pair, sans jamais être interrompue par une frontière d'ownership. Le joueur voit l'eau couler naturellement à travers les frontières de quadrant comme si elles n'existaient pas.

---

## L'extension d'ownership pour la redstone

### La nature de la redstone dans Minecraft

La redstone est le système de logique combinatoire de Minecraft. Ses composants — fils, répéteurs, comparateurs, pistons, lampes, portes — forment des circuits dont l'état est calculé à chaque tick selon l'état de leurs voisins. Un signal peut traverser de longues distances via des fils conducteurs et activer des mécanismes distants de la source.

### La détection du circuit connecté

Quand l'ownership engine détecte un composant redstone actif ou susceptible de changer d'état près d'une frontière, il identifie la composante connexe complète du circuit : l'ensemble des composants redstone directement ou indirectement reliés par des fils conducteurs. Si cette composante connexe traverse une frontière d'ownership, l'intégralité du circuit est temporairement assignée au pair propriétaire de la source d'alimentation ou, en l'absence de source unique claire, au pair propriétaire du composant le plus proche de la frontière.

Cette identification de la composante connexe est une traversée de graphe effectuée sur la représentation locale des blocs que l'ownership engine maintient. Elle n'est pas coûteuse car les circuits redstone sont généralement de taille limitée et localisés.

### Les cas complexes de la redstone

Certains circuits redstone sont intentionnellement conçus pour être larges et traverser de nombreux chunks. Une horloge redstone alimentant un système de ferme automatisée peut traverser plusieurs quadrants. Dans ce cas, l'ownership engine peut avoir à étendre l'ownership sur une large zone.

Pour limiter cette extension à une taille raisonnable, un plafond configurable est appliqué : si la composante connexe dépasse un certain nombre de blocs, seule la portion active du circuit — les composants dont l'état va changer ce tick — est incluse dans l'extension. Les portions du circuit dont l'état est stable ce tick restent dans leur quadrant d'ownership naturel et ne nécessitent pas d'extension.

---

## L'extension d'ownership pour les pistons et blocs en mouvement

### Le mécanisme des pistons

Un piston, quand il est activé, pousse une colonne de blocs dans la direction de son extension. Ces blocs se déplacent physiquement d'une position à une autre en un seul tick. Si la colonne de blocs poussée traverse une frontière d'ownership, les blocs de destination se trouvent dans le quadrant d'un autre pair.

### La prédiction de la zone de destination

Quand l'ownership engine détecte un piston actif dont l'axe de poussée pointe vers une frontière d'ownership, il calcule la zone de destination de tous les blocs que ce piston va déplacer ce tick. Cette zone inclut non seulement les positions finales des blocs poussés, mais aussi les positions de départ qui seront libérées — le piston crée du vide derrière lui.

L'intégralité de cette zone — départ et destination — est incluse dans l'ownership étendu du pair propriétaire du piston. Ce pair simule l'animation complète du piston, produit un delta qui inclut tous les changements de blocs dans les deux quadrants, et les pairs concernés reçoivent le résultat.

### Les blocs en chute libre

Le sable et le gravier soumis à la gravité tombent vers le bas quand le bloc qui les supporte disparaît. Si la colonne de chute d'un tel bloc traverse une frontière d'ownership horizontale, l'ownership engine inclut toute la trajectoire de chute dans l'ownership étendu du pair propriétaire du bloc source.

---

## La résolution des conflits d'extension

### Quand deux phénomènes s'étendent vers la même zone

Il est possible que deux phénomènes physiques appartenant à deux pairs différents cherchent simultanément à étendre leur ownership vers les mêmes blocs. Le cas classique est la rencontre de deux fluides : l'eau de A s'écoule vers l'est en même temps que la lave de B s'écoule vers l'ouest, et les deux graphes de propagation se chevauchent dans une zone intermédiaire.

L'ownership engine résout ce conflit via sa règle déterministe habituelle : le pair dont la source du phénomène est la plus proche des blocs contestés remporte l'extension sur ces blocs. En cas d'égalité parfaite de distance, le pair avec l'identifiant le plus bas l'emporte. Cette règle est calculée localement et de façon identique sur toutes les machines — le résultat est le même partout sans qu'aucune communication ne soit nécessaire.

Le pair qui perd l'extension sur les blocs contestés s'arrête à la limite de l'extension du pair gagnant. Son phénomène physique se termine là où l'ownership de l'autre commence. Dans le cas de l'eau et de la lave, ce comportement est naturellement correct : la rencontre produit de la pierre ou du cobblestone selon les règles Minecraft, simulée par un seul pair, et le résultat est diffusé aux autres via delta.

### Stabilité de la résolution

La résolution déterministe garantit qu'il n'y a jamais de situation où deux pairs simulent le même bloc simultanément. À chaque tick, chaque bloc potentiellement affecté par un phénomène physique appartient à exactement un pair. Les deltas ne produisent donc jamais de changements conflictuels sur les mêmes positions — chaque position de bloc n'apparaît que dans le delta d'un seul pair.

---

## La solution pour les entités : le ghost layer

### Principe

Le ghost layer est une couche de blocs en lecture seule que chaque pair maintient aux frontières de ses quadrants. Elle contient l'état des blocs appartenant aux quadrants adjacents, mis à jour une fois par tick via un échange léger entre pairs voisins.

Le ghost layer ne sert pas à simuler les blocs adjacents. Ces blocs sont simulés par leurs pairs propriétaires. Il sert uniquement à fournir un contexte géographique aux entités qui évoluent près des frontières, sans lequel leur comportement serait incorrect — un mob qui marche vers un mur dans le quadrant voisin ne devrait pas pouvoir traverser ce mur simplement parce qu'il appartient à un autre pair.

### Ce que le ghost layer contient

Le ghost layer contient l'état des blocs dans une bande de deux blocs de largeur de chaque côté de chaque frontière entre quadrants de pairs différents. Pour une frontière verticale entre deux quadrants adjacents, la bande couvre deux colonnes de blocs du côté de chaque pair, sur toute la hauteur du monde dans les sections actives.

Deux blocs de largeur est suffisant pour tous les cas d'usage des entités : la détection de collision dans Minecraft s'étend au maximum à un bloc de distance dans toutes les directions pour les entités de taille standard, et le pathfinding n'a besoin que d'un bloc de contexte pour évaluer si une case est franchissable.

### La mise à jour du ghost layer

À chaque tick, après la phase de simulation et la capture de delta, chaque pair extrait l'état de ses deux blocs de bordure pour chaque frontière avec un pair adjacent. Ces données sont transmises au pair voisin via un paquet dédié distinct du delta principal.

Ce paquet de ghost layer est séparé du delta pour une raison structurelle : le delta contient uniquement les changements produits dans les quadrants owné, optimisé pour n'inclure que ce qui a changé. Le ghost layer, lui, est transmis en totalité même si les blocs frontière n'ont pas changé, parce que le pair adjacent doit pouvoir confirmer que son contexte est à jour. En pratique, quand les blocs frontière sont stables (le cas la grande majorité du temps), ce paquet est minimal — une simple confirmation de non-changement encodée en quelques octets.

### Le coût réseau du ghost layer

Le ghost layer est le seul trafic réseau produit par la couche 4. Son volume est proportionnel au nombre de frontières entre pairs et à la hauteur des chunks actifs dans ces zones. Pour deux pairs avec une frontière commune, le ghost layer représente deux colonnes de blocs sur deux blocs de large et la hauteur active du monde, échangée dans les deux sens à chaque tick.

En pratique, avec une hauteur active de cent vingt-huit blocs et deux blocs de large de chaque côté, chaque frontier échange quelques centaines de blocs par tick. Avec la compression et le fait que la majorité de ces blocs sont stables d'un tick à l'autre (donc encodés comme non-changés), le volume réseau effectif du ghost layer est de l'ordre de quelques dizaines d'octets par frontière par tick dans les situations calmes.

---

## L'utilisation du ghost layer par les entités

### La détection de collision

Quand une entité dans le quadrant de A s'approche de la frontière avec le quadrant de B, son algorithme de collision a besoin de connaître l'état des blocs juste de l'autre côté de la frontière pour savoir s'il peut les traverser ou s'il doit s'arrêter. Sans ghost layer, l'entité n'a aucune information sur ces blocs — elle verrait le vide et le traverserait.

Avec le ghost layer, l'entité dispose d'une représentation locale des blocs de B jusqu'à deux blocs de profondeur dans le quadrant adjacent. Elle peut détecter un mur, un bord de falaise, une surface d'eau, et adapter son déplacement en conséquence. La collision est correcte sans que les blocs de B aient à être simulés par A.

### Le pathfinding

L'algorithme A* utilisé par les mobs pour trouver leur chemin vers une cible évalue les cases traversables dans un rayon autour de leur position courante. Si leur position est proche d'une frontière, certaines cases évaluées se trouvent dans le quadrant adjacent. Sans ghost layer, ces cases seraient évaluées comme vides — l'algorithme produirait des chemins qui traversent des murs ou évitent des chemins praticables.

Avec le ghost layer, les cases dans le quadrant adjacent jusqu'à deux blocs de profondeur sont évaluées correctement. Le mob peut trouver son chemin à travers une frontière d'ownership aussi naturellement qu'à travers n'importe quelle zone de son propre quadrant.

### La limite du ghost layer pour les entités

Le ghost layer fournit un contexte en lecture seule. Il permet aux entités de naviguer correctement près des frontières mais ne leur permet pas d'interagir avec les blocs adjacents. Un mob ne peut pas casser un bloc dans le quadrant d'un autre pair, ne peut pas déclencher un mécanisme de redstone dans ce quadrant, ne peut pas allumer du feu sur ces blocs.

Ces interactions croisées — une entité dans le quadrant de A qui agit sur un bloc dans le quadrant de B — sont rares dans le comportement naturel des mobs Minecraft et ne justifient pas un mécanisme supplémentaire complexe. Dans le modèle de confiance de YuyuFrame, si un joueur humain dans le quadrant de A interagit avec un bloc dans le quadrant de B, cette interaction traverse naturellement la frontière et est traitée par le mécanisme de transfer d'entité combiné au delta de la couche 3.

---

## Le transfer d'entité revisité

### Le transfer comme mécanisme central de la couche 4 pour les entités

Le transfer d'entité, décrit dans la couche 3, est le mécanisme qui prend en charge les entités qui franchissent complètement une frontière d'ownership. Dans le contexte de la couche 4, il est le complément naturel du ghost layer : le ghost layer gère les entités qui évoluent near d'une frontière sans la franchir, le transfer prend en charge celles qui la franchissent effectivement.

La combinaison des deux mécanismes couvre l'intégralité des cas : une entité peut s'approcher indéfiniment d'une frontière, naviguer le long d'elle, rebrousser chemin, ou la franchir. Dans tous ces cas, son comportement est correct et cohérent.

### La zone de pre-transfer

Pour les entités qui s'approchent d'une frontière avec une trajectoire qui pourrait la franchir dans les prochains ticks, une zone de pre-transfer est définie. Cette zone correspond à une distance d'un bloc de la frontière. Quand une entité entre dans cette zone, le pair qui la simule commence à préparer son paquet de transfer — il sérialise l'état complet de l'entité en arrière-plan, de sorte que quand le franchissement effectif a lieu, le paquet est prêt à être envoyé immédiatement.

Cette préparation anticipée réduit la fenêtre pendant laquelle l'entité n'est simulée par personne lors du transfer effectif. Au lieu de sérialiser l'état complet au moment du franchissement (coûteux), on envoie immédiatement le paquet pré-calculé.

---

## Ce que la couche 4 ne fait pas

### Elle ne synchronise pas les blocs entre pairs

C'est la propriété la plus importante de la couche 4. Contrairement à une approche naïve du border sync qui échangerait l'état des blocs frontière à chaque tick pour maintenir deux représentations cohérentes, la couche 4 ne synchronise pas de blocs. Les blocs sont toujours la responsabilité d'un unique pair. Les phénomènes physiques sont toujours simulés dans leur intégralité par un unique pair. Le ghost layer est en lecture seule et ne donne jamais à un pair l'autorité de modifier les blocs d'un autre.

Cette absence de synchronisation de blocs est rendue possible par l'ownership engine physic-aware : en évitant que les phénomènes physiques traversent les frontières d'ownership, on supprime le besoin de synchroniser des blocs entre pairs. Le problème est résolu à sa source plutôt qu'à ses symptômes.

### Elle ne gère pas les interactions joueur-frontière

Si un joueur humain interagit directement avec un bloc dans le quadrant d'un autre pair — en posant ou cassant un bloc juste de l'autre côté d'une frontière d'ownership — cette interaction est traitée directement par le mécanisme de delta de la couche 3. La couche 4 n'est pas impliquée. Le delta du pair propriétaire du quadrant concerné reflètera le changement de bloc résultant, et tous les pairs recevront le résultat.

### Elle ne valide pas les deltas reçus

Comme toutes les couches de YuyuFrame, la couche 4 ne valide pas le contenu des messages qu'elle reçoit. Les paquets de ghost layer sont acceptés et appliqués de confiance. Cette absence de validation est cohérente avec le modèle de confiance global du système.

---

## Analyse des performances

### Le coût de l'ownership physic-aware

Le scan de détection des phénomènes physiques actifs près des frontières est effectué une fois par tick sur les deux blocs de bordure de chaque quadrant owné. Dans un monde calme sans phénomène physique aux frontières, ce scan est trivial — il parcourt quelques dizaines de blocs et ne trouve rien d'actif. Dans un monde avec des fluides en cours de propagation ou des circuits redstone actifs aux frontières, le scan est plus approfondi mais reste limité à l'analyse des composantes connexes pertinentes.

La complexité de cette analyse est bornée par la taille maximale des phénomènes physiques dans Minecraft. Un fluide ne peut pas s'étendre indéfiniment en un seul tick — ses règles de propagation limitent l'extension à quelques blocs par tick. Un circuit redstone a une taille physiquement limitée par la longueur maximale des fils conducteurs. Ces bornes naturelles font que l'ownership physic-aware reste computationnellement léger même dans les mondes les plus complexes.

### Le coût du ghost layer

Le ghost layer représente un échange réseau supplémentaire par rapport aux deltas de la couche 3. Mais cet échange est structurellement différent des deltas : il est petit, fréquent et prévisible. Sa taille est déterminée par la géométrie des frontières et la hauteur des chunks, pas par l'activité du monde. Les pairs peuvent allouer et réutiliser les mêmes buffers pour ces échanges tick après tick, sans allocation mémoire dynamique.

La latence de réception du ghost layer est identique à celle des deltas — elle dépend de la connexion réseau entre les pairs concernés. Une frontière entre un pair à 10ms et un pair à 90ms aura un ghost layer reçu avec 90ms de retard du côté du pair lent. Les entités du pair lent navigueront donc avec un contexte géographique légèrement en retard. En pratique, les blocs aux frontières changent rarement d'un tick à l'autre, et ce retard est imperceptible pour les entités dont le comportement change sur des dizaines de ticks.

---

## Vue d'ensemble du système complet

La couche 4 clôt l'architecture de YuyuFrame. En combinant l'ownership physic-aware pour les blocs et le ghost layer pour les entités, elle résout le problème des frontières sans introduire de synchronisation d'état entre pairs, sans double simulation, et sans mécanisme d'arbitrage complexe.

La propriété fondamentale qui émerge de cette architecture est que **chaque bloc et chaque phénomène physique dans le monde est à tout instant la responsabilité d'un et d'un seul pair**. Il n'y a pas d'état partagé en écriture entre pairs — seulement des lectures du ghost layer et des réceptions de deltas. Cette exclusivité d'écriture est la garantie de cohérence du système : deux pairs ne peuvent jamais produire des résultats conflictuels sur le même bloc au même tick, parce qu'il est impossible que deux pairs soient simultanément propriétaires du même bloc.

---

## Interface avec les autres couches

### Avec la couche 2 (Ownership Engine)

La couche 4 étend l'ownership engine de la couche 2 avec la capacité de détection des phénomènes physiques et d'extension dynamique de l'ownership. Ces deux couches sont donc étroitement couplées : l'ownership engine physic-aware est une évolution directe de l'ownership engine de base, pas un composant séparé. Il incorpore la logique de scan et d'extension en plus de la logique d'attribution par distance.

### Avec la couche 3 (Simulation et delta)

Le ghost layer est transmis via les mêmes streams libp2p que les deltas de la couche 3, mais dans des paquets distincts identifiés par un type différent. Le cœur Rust gère les deux types de paquets dans son pipeline de réception et de sérialisation. Le ghost layer est appliqué à `ClientWorld` en même temps que les deltas, au début de chaque tick.

L'extension d'ownership produite par la couche 4 est transmise à la couche 3 comme une liste de blocs supplémentaires à inclure dans la simulation locale du serveur intégré. Du point de vue de la couche 3, ces blocs supplémentaires sont traités exactement comme des blocs normalement owné — ils sont simulés, leurs changements sont capturés dans le delta, et le delta est diffusé aux pairs concernés.

---

*Documentation rédigée pour le projet YuyuFrame — Architecture P2P Minecraft basée sur la confiance.*  
*Couche précédente : [Couche 3 — Simulation locale et broadcast des deltas](./README_couche3_v2_simulation_et_delta.md)*  
*Fin de la documentation des couches. Voir le README principal pour la vue d'ensemble de l'architecture complète.*