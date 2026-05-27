# Couche 2 — Ownership Engine

> Deuxième couche de l'architecture YuyuFrame P2P Minecraft.  
> Objectif : déterminer, à chaque instant et de façon identique sur toutes les machines, quel joueur est responsable de la simulation de chaque portion du monde.

---

## Rôle de cette couche

L'ownership engine est le cerveau du système distribué. Sans lui, chaque joueur ne saurait pas ce qu'il doit simuler, et rien ne garantirait qu'une portion du monde soit simulée une seule fois — ni qu'elle le soit du tout.

Son rôle est strictement défini : pour chaque unité de simulation (appelée quadrant, détaillée plus bas), répondre à la question « qui est responsable de la calculer ce tick ? ». Cette réponse doit être identique sur toutes les machines connectées, sans qu'elles aient besoin de se consulter pour y parvenir. C'est la propriété fondamentale qui rend le système viable : la convergence déterministe.

Cette couche ne touche pas à la simulation elle-même. Elle ne connaît pas les blocs, les entités, la redstone ou la physique. Elle ne fait que maintenir une table d'attribution et la mettre à jour quand la situation des joueurs évolue. La simulation est le travail de la couche 3. L'ownership engine lui dit simplement quoi simuler.

---

## Vocabulaire et modèle de données

### Le chunk

Minecraft divise son monde en colonnes verticales appelées chunks. Chaque chunk couvre une zone de seize blocs sur seize blocs en largeur et profondeur, sur toute la hauteur du monde. Les chunks sont identifiés par des coordonnées entières : le chunk `(cx, cz)` couvre les blocs dont la coordonnée X va de `cx × 16` à `cx × 16 + 15`, et de même pour l'axe Z.

Un chunk est l'unité naturelle de chargement dans Minecraft. Le serveur ne simule que les chunks situés dans le rayon de vue des joueurs connectés. En dehors de ce rayon, les chunks sont gelés : aucun mob ne bouge, aucune redstone ne clignote, aucune culture ne pousse. Cette propriété existante de Minecraft est conservée dans notre architecture.

### Le quadrant

Le quadrant est l'unité d'ownership de notre système. Un chunk est divisé en quatre quadrants égaux, chacun couvrant une zone de huit blocs sur huit blocs. On les nomme par leur position cardinale dans le chunk : nord-ouest (NW), nord-est (NE), sud-ouest (SW), sud-est (SE). En coordonnées monde, le centre de chaque quadrant est calculable directement depuis les coordonnées du chunk parent et la position cardinale du quadrant.

Le choix du quadrant comme unité — plutôt que le chunk entier, ou un découpage plus fin — est délibéré. Un chunk entier attribué à un seul joueur crée un déséquilibre dès que deux joueurs se retrouvent dans la même zone : l'un simule tout, l'autre ne fait rien. Un découpage trop fin (par bloc, par section de deux blocs) génère un overhead de synchronisation disproportionné par rapport au gain. Le quadrant offre un bon équilibre : quatre unités par chunk permettent une distribution significative de la charge sans multiplier le nombre de frontières à synchroniser.

### La position monde d'un quadrant

Chaque quadrant a un centre géométrique exprimé en coordonnées monde (en blocs, nombres flottants). C'est ce centre qui sert de référence pour l'algorithme d'attribution. Il est calculé de façon déterministe depuis les coordonnées du chunk et la position cardinale : on connaît l'origine du chunk (coin nord-ouest), on ajoute quatre blocs sur chaque axe pour NW, douze blocs en X et quatre en Z pour NE, quatre en X et douze en Z pour SW, douze blocs sur chaque axe pour SE. Ce calcul est identique sur toutes les machines, il ne nécessite aucune communication.

### L'état d'un pair (PeerState)

L'ownership engine maintient une représentation locale de chaque joueur connecté. Cet état contient l'identifiant unique du pair (le PeerId libp2p, dérivé de sa clé cryptographique), sa position dans le monde en coordonnées flottantes (X et Z en blocs), et un horodatage de dernière mise à jour.

La position d'un pair est mise à jour dès que celui-ci envoie un paquet de mouvement. Dans Minecraft, les joueurs envoient leur position à chaque tick où ils bougent, soit potentiellement vingt fois par seconde. L'ownership engine ne réagit pas à chaque mise à jour individuelle — seulement quand la position du joueur franchit une frontière significative, c'est-à-dire quand il change de quadrant ou s'en approche suffisamment pour modifier les attributions. Ce filtrage évite de recalculer la table d'ownership vingt fois par seconde pour un joueur qui marche au milieu d'un chunk.

L'horodatage de dernière mise à jour sert à détecter les pairs silencieux. Un pair qui ne répond plus depuis un seuil configurable (cinq secondes par défaut) est considéré comme déconnecté. Ses quadrants sont alors redistribués automatiquement.

### La table d'ownership

La table d'ownership est une structure associative qui mappe chaque quadrant actif vers l'identifiant du pair qui en est responsable. Elle n'est pas globale : elle ne contient que les quadrants situés dans le rayon de vue d'au moins un joueur. Les quadrants en dehors de ce rayon n'existent tout simplement pas dans la table — ils ne sont ni attribués ni simulés.

Cette table est recalculée localement sur chaque machine à partir des mêmes données d'entrée (les positions des pairs), en appliquant le même algorithme déterministe. Le résultat est identique partout. Il n'y a ni vote, ni synchronisation de la table elle-même, ni élu qui décide et diffuse le résultat. Chaque machine arrive à la même conclusion indépendamment.

---

## L'ensemble des chunks actifs

### Définition

À chaque instant, un chunk est dit « actif » s'il se trouve dans le rayon de vue d'au moins un joueur connecté. Le rayon de vue est un paramètre configurable, typiquement exprimé en chunks. Avec un rayon de dix chunks, un joueur rend actif un carré de vingt et un chunks de côté centré sur sa position, soit quatre cent quarante et un chunks, soit mille sept cent soixante-quatre quadrants.

L'ensemble des chunks actifs évolue en permanence au fil des déplacements des joueurs. Quand un joueur avance vers le nord, des chunks s'activent devant lui et se désactivent derrière. L'ownership engine suit ces changements et met à jour la table en conséquence.

### Pourquoi cet ensemble est fondamental

La notion de chunks actifs est la clé de la scalabilité du système. Un monde Minecraft est potentiellement infini. On ne peut pas maintenir une table d'ownership pour l'intégralité du monde — cela serait non seulement impossible en mémoire, mais surtout inutile. Seuls les chunks où des joueurs sont présents ont besoin d'être simulés. Les autres sont gelés, exactement comme dans un serveur Minecraft classique.

Dans notre architecture, un chunk hors du rayon de vue de tous les joueurs n'appartient à personne. Si un joueur s'en approche, il entre dans l'ensemble actif et reçoit immédiatement une attribution. Si c'est un chunk déjà visité, son état est chargé depuis le stockage local du pair qui le prend en charge. Si c'est un chunk jamais visité, il est généré procéduralement par ce pair.

### Calcul de l'ensemble actif

L'ensemble actif est l'union des zones de vue de tous les pairs connectés. Deux joueurs proches l'un de l'autre produisent des zones qui se chevauchent largement. Ce chevauchement n'est pas un problème : un chunk est actif dès qu'il est dans la zone de vue d'au moins un joueur, et un seul pair en sera propriétaire.

Deux joueurs aux extrémités opposées d'un monde produisent deux ensembles disjoints, chacun simulé indépendamment. La charge est alors réellement distribuée : chaque joueur simule ses propres chunks, et il n'y a aucune communication entre les deux îles de simulation tant qu'elles restent séparées.

---

## L'algorithme d'attribution

### La règle de distance minimale

Pour chaque quadrant actif, le propriétaire est le pair dont la position monde est la plus proche du centre géométrique du quadrant. La distance utilisée est la distance euclidienne dans le plan horizontal (X et Z), ignorant la coordonnée verticale Y. Ce choix est naturel pour Minecraft, où les joueurs évoluent principalement en deux dimensions et où la hauteur ne reflète pas la proximité géographique (un joueur sous terre est « près » d'un chunk à la surface s'il a les mêmes coordonnées X et Z).

La distance au carré est utilisée en interne pour éviter le calcul de la racine carrée, qui n'est pas nécessaire pour la comparaison.

### Le tiebreak par identifiant

Deux pairs peuvent se trouver exactement à la même distance d'un quadrant. Ce cas, bien qu'improbable en conditions réelles, doit être traité de façon déterministe pour que toutes les machines aboutissent au même résultat. On utilise l'identifiant du pair comme critère de départage : en cas d'égalité parfaite de distance, le pair dont l'identifiant est le plus petit (selon l'ordre lexicographique sur les octets du PeerId) l'emporte.

Ce tiebreak est arbitraire dans son choix (on aurait pu prendre le plus grand, ou appliquer un hash), mais il est universel : tous les pairs connaissent les PeerIDs de tous les autres, et l'ordre lexicographique est défini de la même façon partout. Le résultat est donc identique sur toutes les machines.

### Pourquoi cette règle est suffisante

La règle de distance minimale peut sembler simpliste, mais elle possède plusieurs propriétés désirables pour ce système.

Elle est **localement sensée** : attribuer un quadrant au joueur le plus proche maximise la probabilité que ce joueur soit celui qui interagit le plus avec ce quadrant. Ses actions (poser un bloc, frapper un mob) sont traitées localement, sans latence réseau supplémentaire pour la couche de simulation.

Elle est **continue** : quand un joueur se déplace, les frontières d'ownership se déplacent progressivement avec lui. Il n'y a pas de saut brutal où des centaines de quadrants changent de propriétaire simultanément — sauf dans les cas particuliers abordés plus bas.

Elle est **équilibrée** : deux joueurs éloignés l'un de l'autre se partagent le monde en deux zones approximativement égales. Deux joueurs côte à côte se partagent les quadrants autour d'eux en deux moitiés. La charge est distribuée naturellement.

Elle est **scalable** : avec dix joueurs, chaque joueur simule en moyenne un dixième des quadrants actifs. L'algorithme ne change pas, seul le nombre de candidats dans la comparaison augmente.

### Ce que l'algorithme ne garantit pas

L'algorithme ne garantit pas une répartition parfaitement équitable de la charge. Un joueur seul dans une zone dense en entités simule plus de calcul par quadrant qu'un joueur dans une plaine vide. Ce déséquilibre est inhérent à Minecraft et non résolu par l'ownership — c'est la nature du monde simulé qui dicte la charge, pas la découpe géographique.

L'algorithme ne garantit pas non plus que la frontière d'ownership entre deux joueurs soit stable quand ils marchent l'un vers l'autre. Quand leurs zones de vue se chevauchent, les quadrants proches de la frontière changent rapidement de propriétaire. C'est prévu et géré par le mécanisme de handoff décrit ci-dessous.

---

## Les événements qui déclenchent un recalcul

L'ownership engine ne recalcule pas la table à chaque tick. Cela serait inutilement coûteux pour un résultat presque toujours identique. Le recalcul est déclenché par des événements discrets qui signalent un changement potentiel dans la distribution optimale.

### Changement de quadrant d'un pair

Quand un joueur se déplace, sa position est diffusée aux autres pairs via la couche 1. L'ownership engine compare la nouvelle position à l'ancienne et vérifie si le joueur a changé de quadrant. Si oui, certains quadrants pourraient changer de propriétaire, et un recalcul est planifié. Si le joueur est resté dans le même quadrant, aucun recalcul n'est nécessaire.

Ce filtrage est important. Un joueur qui tourne sur place ou qui fait de petits pas envoie des paquets de position à vingt ticks par seconde, mais ne change de quadrant qu'une fois toutes les quelques secondes. Le recalcul est donc bien plus rare que les mises à jour de position.

### Arrivée d'un nouveau pair

Quand un pair rejoint la session, il apporte avec lui sa position initiale et ses informations de connexion. Son arrivée peut modifier l'attribution de nombreux quadrants, notamment dans la zone qu'il rejoint. Un recalcul complet est déclenché.

L'arrivée d'un pair est aussi l'occasion de lui transmettre l'état courant de la table d'ownership et les positions de tous les autres pairs, pour qu'il puisse construire sa propre copie locale de façon cohérente.

### Départ ou timeout d'un pair

Quand un pair se déconnecte proprement (message de départ reçu) ou disparaît silencieusement (timeout après cinq secondes sans message), ses quadrants doivent être redistribués. Un recalcul est déclenché après avoir retiré ce pair de la liste des candidats.

Le cas du départ silencieux est le plus délicat. Un pair peut perdre sa connexion internet momentanément et la retrouver quelques secondes plus tard. Pour éviter de redistribuer ses quadrants puis de les lui rendre cinq secondes après — ce qui implique deux handoffs consécutifs — on peut configurer une fenêtre de grâce avant de considérer un pair comme définitivement absent.

### Entrée ou sortie de chunks dans la zone active

Quand un joueur avance et que de nouveaux chunks entrent dans son rayon de vue, ces chunks et leurs quadrants n'existent pas encore dans la table. Ils sont ajoutés et immédiatement attribués. Quand des chunks sortent du rayon de vue de tous les joueurs, leurs quadrants sont retirés de la table. Aucun recalcul global n'est nécessaire — juste l'insertion ou la suppression des entrées concernées.

---

## Le handoff — transfert d'un quadrant entre pairs

### Quand a lieu un handoff

Un handoff a lieu quand un quadrant change de propriétaire. Cela arrive dans trois situations : un joueur se déplace et ses quadrants se redistribuent, un nouveau pair arrive et prend possession de quadrants proches de lui, ou un pair part et ses quadrants sont repris par les voisins.

Le handoff est le moment critique du système. Pendant la fraction de seconde où un quadrant change de main, il ne doit pas être simulé deux fois (par l'ancien et le nouveau propriétaire simultanément) ni zéro fois (ni l'un ni l'autre ne simule pendant la transition).

### Le protocole de handoff

Le handoff se déroule en trois étapes séquentielles.

D'abord, l'ancien propriétaire détecte qu'il perd l'ownership d'un quadrant suite au recalcul local. Il cesse immédiatement de simuler ce quadrant mais continue à en tenir l'état en mémoire. Il prépare un paquet de handoff contenant l'état complet du quadrant à l'instant du transfert : les données de tous les blocs modifiés depuis le dernier chargement, l'état des entités présentes, les états de redstone actifs.

Ensuite, l'ancien propriétaire envoie ce paquet au nouveau propriétaire via le tunnel libp2p de la couche 1. Ce transfert peut prendre quelques dizaines de millisecondes selon la latence du réseau.

Enfin, le nouveau propriétaire reçoit le paquet, intègre l'état reçu dans sa simulation locale, et commence à simuler le quadrant à partir du tick suivant. Il envoie un accusé de réception à l'ancien propriétaire, qui peut alors libérer l'état du quadrant de sa mémoire.

### Cohérence pendant le transfert

Entre le moment où l'ancien propriétaire cesse de simuler et le moment où le nouveau commence, il y a une fenêtre de quelques dizaines de millisecondes pendant laquelle le quadrant n'est simulé par personne. Dans un système basé sur la confiance entre amis, cette fenêtre est acceptable : les mobs se figent momentanément, la redstone s'arrête un instant. Les joueurs ne le remarqueront généralement pas si le réseau est correct.

Si la latence est faible (moins de vingt millisecondes, cas courant sur un réseau local ou entre voisins), cette fenêtre est imperceptible. Si la latence est élevée (connexion internationale), des ajustements sont possibles — comme continuer à simuler en read-only pendant le transfert et fusionner les deltas — mais ces optimisations sortent du périmètre de la couche 2.

### Les handoffs en cascade

Un recalcul peut entraîner des dizaines de handoffs simultanés, notamment quand un joueur traverse rapidement une zone ou quand un pair rejoint/quitte la session. L'implémentation doit gérer ces handoffs de façon concurrente et non bloquante. En Rust avec Tokio, chaque handoff est une tâche asynchrone indépendante. La table d'ownership est protégée par un verrou en lecture-écriture (RwLock) : les handoffs lisent la table pour connaître le destinataire, et le recalcul acquiert une écriture exclusive le temps de la mise à jour.

---

## Gestion de la déconnexion

### La stratégie du gel

Quand un pair se déconnecte, ses quadrants sont redistribués aux pairs les plus proches par le recalcul. Mais le nouveau propriétaire n'a pas nécessairement reçu l'état courant de ces quadrants — le pair est parti sans envoyer de paquet de handoff.

La stratégie du gel consiste à traiter ces quadrants comme des chunks non chargés. Le nouveau propriétaire charge leur dernier état connu depuis le stockage local (le fichier de région Minecraft sur le disque). Les modifications effectuées par le pair déconnecté depuis la dernière sauvegarde sont perdues — exactement comme ce qui se passe quand un serveur Minecraft vanilla plante sans sauvegarder.

Pour minimiser la perte de données, chaque pair sauvegarde ses quadrants à intervalles réguliers (toutes les trente secondes par défaut, configurable). La perte maximale est donc bornée par cet intervalle. Dans un contexte de jeu entre amis, c'est parfaitement acceptable.

### La stratégie de la reprise

Une stratégie plus ambitieuse consiste à envoyer l'état courant des quadrants au relay server à intervalles réguliers, pas seulement sur le disque local. Quand un pair prend ownership de quadrants orphelins, il les télécharge depuis le relay plutôt que de les charger depuis le disque.

Cette stratégie garantit une perte de données maximale égale à l'intervalle de sauvegarde vers le relay, indépendamment de l'état du disque local. Elle est particulièrement utile si le pair déconnecté utilisait des chunks qui n'étaient pas encore sauvegardés localement (chunks nouvellement générés, par exemple).

L'implémentation de la stratégie de reprise nécessite que le relay server joue un rôle de stockage temporaire léger — pas de données de jeu permanentes, juste des snapshots chiffrés valides pour la durée de la session. Le relay purge ces données dès que tous les pairs ont confirmé avoir récupéré les quadrants concernés.

Pour un premier déploiement, la stratégie du gel est recommandée. La reprise peut être ajoutée en couche ultérieure sans modifier l'architecture de l'ownership engine.

### La fenêtre de grâce et la reconnexion

Si un pair qui s'est déconnecté revient dans les trente secondes (fenêtre de grâce configurable), l'ownership engine peut lui restituer ses quadrants sans handoff complet. Les quadrants ne sont pas redistribués tant que la fenêtre de grâce n'est pas expirée. Pendant ce temps, ils sont gelés — personne ne les simule — ou, si une entité critique (un autre joueur) les traverse, ils sont temporairement pris en charge par le voisin le plus proche sans mise à jour formelle de la table.

À la reconnexion, le pair reçoit l'état courant de la table d'ownership et les positions de tous les pairs. Il recalcule localement sa copie de la table, commence à simuler ses quadrants, et la session reprend comme si rien ne s'était passé. Si la fenêtre de grâce a expiré et que ses anciens quadrants ont été redistribués, il reçoit les handoffs correspondants depuis leurs nouveaux propriétaires.

---

## Cas particuliers et comportements à la frontière

### Deux joueurs dans le même quadrant

Quand deux joueurs se trouvent dans le même quadrant de huit blocs sur huit, leurs distances aux quadrants voisins sont quasiment identiques. L'ownership des quadrants immédiats peut basculer rapidement d'un joueur à l'autre à chaque déplacement. Pour éviter une cascade de handoffs intempestifs, un mécanisme d'hystérésis est appliqué : un quadrant ne change de propriétaire que si la différence de distance entre l'ancien et le nouveau propriétaire dépasse un seuil (deux blocs par défaut). Ce seuil empêche les oscillations rapides tout en garantissant que la redistribution a lieu quand un joueur s'éloigne réellement.

### Un joueur seul dans une zone

Un joueur seul possède l'intégralité des quadrants actifs autour de lui. Il simule tout — comportement identique à l'option A classique (LAN Minecraft avec hôte). La couche 2 n'apporte pas de bénéfice dans ce cas en termes de distribution de charge, mais elle ne coûte rien non plus : la table d'ownership est triviale (tout mappe vers le pair unique), et aucun handoff n'est nécessaire.

### La frontière entre chunks de deux joueurs éloignés

Quand deux joueurs sont suffisamment éloignés pour que leurs zones de vue ne se chevauchent pas, il n'y a pas de frontière d'ownership entre eux — simplement deux ensembles disjoints de quadrants, chacun entièrement attribué à son joueur. Le risque de conflits à la frontière est nul car les deux zones ne sont pas adjacentes.

Quand leurs zones commencent à se rejoindre (les joueurs s'approchent), les premiers quadrants partagés apparaissent dans la table. L'algorithme de distance attribue naturellement la moitié de ces quadrants à chaque joueur, et les handoffs initiaux sont déclenchés pour transférer les chunks que l'approchant doit maintenant simuler depuis celui qui les simulait auparavant.

### La redstone à la frontière de deux quadrants

La redstone est le cas le plus sensible de la frontière entre quadrants. Un circuit de redstone peut traverser la frontière entre deux quadrants owné par des pairs différents. La couche 2 ne résout pas ce problème — elle le pose clairement pour que la couche 4 (border sync) le gère. La règle est simple : la propagation de la redstone s'arrête à la frontière du quadrant. Le pair propriétaire du quadrant adjacent reçoit le signal par le mécanisme de border sync et continue la propagation de son côté. Le délai introduit est d'un tick de border sync, typiquement un tick Minecraft (cinquante millisecondes), ce qui est imperceptible pour tous les circuits sauf les plus précis au tick.

### De nombreux joueurs dans la même zone

Avec dix joueurs dans un rayon de trente blocs, l'algorithme de distance produit des attributions très fragmentées : chaque joueur possède quelques quadrants immédiatement autour de lui, mais les frontières sont denses et bougent à chaque déplacement. Dans ce cas extrême, le coût des handoffs peut dépasser le bénéfice de la distribution. Un mécanisme de regroupement optionnel peut forcer l'attribution de chunks entiers (quatre quadrants d'un même chunk au même pair) quand plusieurs pairs sont dans le même chunk, pour réduire le nombre de frontières internes. Ce mécanisme est désactivé par défaut et peut être activé dans la configuration.

---

## Diffusion des positions et cohérence de la table

### Comment chaque pair connaît la position des autres

La table d'ownership est recalculée localement sur chaque machine à partir des positions de tous les pairs. Pour que ce calcul soit cohérent, chaque pair doit connaître les positions de tous les autres. C'est la responsabilité de la couche 3 : à chaque mouvement significatif, un pair diffuse sa nouvelle position à tous les autres via le tunnel libp2p. L'ownership engine écoute ces diffusions et met à jour son état interne en conséquence.

Cette diffusion est légère : une position Minecraft tient en vingt-quatre octets (trois doubles pour X, Y, Z). À vingt ticks par seconde, c'est moins de cinq cents octets par seconde par pair, bien en deçà de ce que la couche 1 peut transporter.

### Le problème de la réception désordonnée

Les paquets réseau peuvent arriver dans un ordre différent de celui dans lequel ils ont été envoyés. Si le pair A reçoit la position « t+2 » de B avant la position « t+1 », il ne doit pas régresser la position de B vers t+1 quand ce paquet arrive enfin. L'ownership engine doit ignorer tout paquet de position dont l'horodatage est inférieur au dernier horodatage reçu pour ce pair.

Cette règle simple suffit dans un contexte de jeu en temps réel où les paquets récents sont toujours plus pertinents que les paquets anciens. On ne cherche pas à reconstituer l'historique exact des mouvements — seulement à avoir la position la plus récente possible.

### La divergence temporaire de la table

Entre le moment où un pair envoie sa position et le moment où les autres la reçoivent, il y a un délai réseau. Pendant ce délai, la table d'ownership locale de chaque pair peut diverger légèrement de celle des autres. Pair A pense qu'il possède un quadrant que Pair B pense lui appartenir déjà.

Dans un système basé sur la confiance, cette divergence transitoire est acceptable. Elle dure au maximum quelques dizaines de millisecondes (le délai aller-retour du réseau) et se résorbe dès que tous les pairs ont reçu et intégré la nouvelle position. Pendant ce court laps de temps, le pire cas est qu'un quadrant soit simulé deux fois (par les deux pairs qui pensent l'avoir chacun). Ce doublon transitoire est imperceptible pour les joueurs et ne crée pas d'incohérence durable dans l'état du monde.

---

## Performance et optimisations

### Complexité algorithmique

Le recalcul de la table d'ownership est `O(Q × P)` où `Q` est le nombre de quadrants actifs et `P` est le nombre de pairs. Avec un rayon de vue de dix chunks et deux pairs, `Q` vaut au plus environ trois mille cinq cents quadrants (les zones se chevauchent), et `P` vaut deux. Le recalcul prend moins d'une milliseconde sur du matériel moderne.

Avec vingt pairs et un rayon de dix chunks chacun, `Q` peut monter à plusieurs dizaines de milliers si les joueurs sont dispersés, et `P` vaut vingt. Le recalcul reste sous la dizaine de millisecondes — bien en deçà d'un tick de cinquante millisecondes.

### Mise en cache et recalcul partiel

Pour les configurations avec de nombreux pairs et un monde étendu, un recalcul partiel peut être implémenté : quand un pair se déplace, seuls les quadrants dans un rayon autour de son ancienne et nouvelle position sont recalculés. Les quadrants éloignés de sa trajectoire ne peuvent pas avoir changé de propriétaire et n'ont pas besoin d'être revérifiés.

Ce recalcul partiel réduit la complexité à `O(R² × P)` où `R` est le rayon de recalcul (typiquement le double du rayon de vue d'un pair), ce qui est une amélioration significative pour les grandes sessions.

### Représentation mémoire

La table d'ownership est une table de hachage dont les clés sont des paires d'entiers (coordonnées de chunk et index de quadrant). Les clés et valeurs sont petites (quelques octets chacune). Une table de dix mille entrées occupe moins d'un mégaoctet de mémoire, ce qui est négligeable.

L'ensemble des chunks actifs peut être représenté comme un HashSet de coordonnées de chunks, mis à jour à chaque changement de position des pairs. Cette structure est recalculée en `O(P × V²)` où `V` est le rayon de vue en chunks — de l'ordre de la milliseconde pour des valeurs courantes.

---

## Ce que cette couche ne fait pas

L'ownership engine ne simule rien. Il ne connaît pas les blocs, les entités, la redstone, la physique de l'eau, la météo ou le cycle jour-nuit. Il répond uniquement à la question « qui simule quoi ».

Il ne résout pas les conflits. Si deux pairs effectuent simultanément des actions conflictuelles dans des quadrants adjacents — poser le même bloc, frapper la même entité — le conflit est géré par la couche 3 et la couche 4, pas par l'ownership engine.

Il ne gère pas le protocole de border sync. La synchronisation de l'état des blocs à la frontière entre deux quadrants est le travail de la couche 4. L'ownership engine lui fournit la liste des frontières (paires de quadrants adjacents owné par des pairs différents), mais ne s'implique pas dans l'échange de données.

Il ne persiste pas l'état du monde. La sauvegarde sur disque des chunks owné par un pair est la responsabilité de la couche 3. L'ownership engine sait seulement à qui appartient chaque quadrant à l'instant présent.

---

## Interface avec les autres couches

### Avec la couche 1 (Connectivité P2P)

L'ownership engine reçoit les paquets de position des pairs via le tunnel libp2p établi par la couche 1. Il utilise ce même tunnel pour envoyer les paquets de handoff. Il n'a pas besoin de connaître les détails du protocole réseau sous-jacent — il envoie et reçoit des messages adressés à des PeerIDs.

### Avec la couche 3 (Simulation locale)

L'ownership engine expose à la couche 3 la liste des quadrants que le pair local doit simuler à chaque tick : c'est simplement l'ensemble des quadrants actifs attribués au pair local. Quand cette liste change (suite à un recalcul), la couche 3 est notifiée : elle cesse de simuler les quadrants perdus et commence à simuler les quadrants acquis dès réception du paquet de handoff correspondant.

### Avec la couche 4 (Border sync)

L'ownership engine expose à la couche 4 l'ensemble des frontières actives : toutes les paires de quadrants adjacents dont les propriétaires sont différents. C'est sur ces frontières que la couche 4 opère, en échangeant l'état des blocs frontière à chaque tick pour maintenir la cohérence visible entre les zones simulées par des pairs différents.

---

## Estimation de la charge réseau

L'ownership engine lui-même génère peu de trafic réseau. Les paquets de position des pairs, qui déclenchent les recalculs, sont produits par la couche 3. Les paquets de handoff, produits par l'ownership engine, sont rares (quelques kilooctets par handoff, quelques handoffs par minute en usage normal) et bursty (pic lors d'une arrivée ou d'un départ). Entre deux handoffs, l'ownership engine ne génère aucun trafic.

Le trafic dominant reste celui de la couche 3 (deltas de simulation) et de la couche 4 (border sync). L'overhead de l'ownership engine est négligeable devant ces flux.

---

*Documentation rédigée pour le projet YuyuFrame — Architecture P2P Minecraft basée sur la confiance.*  
*Couche précédente : [Couche 1 — Connectivité P2P](./README_couche1_connectivite_p2p.md)*  
*Couche suivante : [Couche 3 — Simulation locale par quadrant](./README_couche3_simulation_locale.md)*