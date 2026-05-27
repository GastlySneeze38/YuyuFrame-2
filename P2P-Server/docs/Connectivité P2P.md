# Couche 1 — Connectivité P2P

> Première couche de l'architecture YuyuFrame P2P Minecraft.  
> Objectif : permettre à deux joueurs de se connecter directement sur Internet, sans port forwarding, sans serveur central permanent.

---

## Rôle de cette couche

Cette couche ne touche pas à Minecraft. Elle résout un problème purement réseau : deux machines situées derrière des routeurs domestiques ne peuvent pas se joindre directement, car leurs adresses IP publiques sont partagées et leurs ports sont fermés par défaut.

La couche 1 s'occupe exclusivement de créer un tunnel de communication fiable entre les deux agents P2P des joueurs. Une fois ce tunnel établi, toutes les couches supérieures (ownership, simulation, border sync) l'utilisent comme canal de transport sans se soucier du réseau sous-jacent.

---

## Vocabulaire essentiel

**Peer (pair)** — chaque joueur participant au réseau. Chaque peer possède un identifiant unique (PeerID) dérivé de sa clé cryptographique publique. Cet identifiant ne change pas entre les sessions si les clés sont persistées sur disque.

**Multiaddr** — format d'adresse universel utilisé par libp2p. Une multiaddr encode le protocole, l'IP et le port dans une seule chaîne lisible. Un peer peut avoir plusieurs multiaddrs (IPv4, IPv6, relay).

**Relay server** — serveur léger et peu coûteux dont le seul rôle est de mettre deux peers en contact. Il ne transporte jamais de données Minecraft. Une fois les peers connectés directement, le relay sort de la boucle et n'est plus sollicité jusqu'à la prochaine session.

**Hole punching** — technique permettant à deux machines derrière des NAT de s'atteindre directement. Les deux peers envoient simultanément un paquet vers l'adresse de l'autre, ce qui "perce" le NAT de chaque côté et ouvre un chemin direct.

**Circuit relay** — mécanisme de secours. Quand le hole punching échoue (NAT symétrique, réseau d'entreprise, VPN restrictif), les données transitent via le relay server. La connexion reste fonctionnelle mais légèrement plus lente.

---

## Les trois phases de connexion

### Phase 1 — Signaling

Les deux agents démarrent et se connectent au relay server. Chacun publie son PeerID et ses multiaddrs (adresses sur lesquelles il est joignable). En échange, il reçoit un code court — une suite de six chiffres ou lettres — qu'il affiche au joueur.

Le joueur A communique son code à B (par message, voix, ou tout autre moyen hors-bande). B entre ce code dans son agent. L'agent de B interroge le relay server avec ce code et récupère les informations de connexion de A.

Cette phase se passe entièrement via le relay server. Aucun paquet ne transite directement entre A et B à ce stade.

### Phase 2 — Échange d'adresses

Une fois que B connaît le PeerID de A, son agent tente de se connecter à A via le relay server (circuit relay). Cette première connexion relayée sert de canal sécurisé pour que les deux agents échangent leurs adresses réseau réelles : IP publique, IP locale, ports disponibles.

Pendant cet échange, le protocole `dcutr` (Direct Connection Upgrade through Relay) coordonne les deux agents pour qu'ils tentent simultanément la connexion directe. Le relay server facilite la synchronisation de ce moment sans voir le contenu des données.

### Phase 3 — Hole punch et connexion directe

Les deux agents envoient simultanément des paquets UDP vers l'adresse publique de l'autre. Cette simultanéité est la clé du hole punching : chaque routeur voit partir un paquet vers l'extérieur avant de recevoir un paquet entrant depuis la même adresse, ce qui l'amène à considérer la connexion comme légitime et à la laisser passer.

Si le hole punching réussit — ce qui est le cas dans la grande majorité des configurations domestiques — la connexion bascule automatiquement sur le chemin direct, sans passer par le relay. La latence chute immédiatement au niveau du ping brut entre les deux machines.

Si le hole punching échoue (NAT symétrique, double NAT, configurations réseau restrictives), la connexion reste sur le circuit relay. Le jeu fonctionne toujours, avec une latence légèrement plus élevée.

---

## Architecture de l'agent

L'agent est un programme indépendant qui tourne en arrière-plan pendant la session de jeu. Minecraft ne sait pas qu'il existe. Du point de vue du client Minecraft, il se connecte à un serveur local classique. Du point de vue du serveur (ou du mini-serveur de simulation), il reçoit des connexions TCP ordinaires.

L'agent fait le lien entre ces deux mondes : il intercepte les connexions TCP de Minecraft et les fait transiter sur le tunnel libp2p, dans les deux sens, en temps réel. Il ne lit pas, n'interprète pas et ne modifie pas les paquets Minecraft — il les transporte tels quels.

```
Minecraft (client)
      │ TCP local
      ▼
  [ Agent P2P ]  ←──── tunnel libp2p chiffré ────►  [ Agent P2P ]
                                                           │ TCP local
                                                           ▼
                                                   Mini-serveur de simulation
```

Cette transparence totale vis-à-vis de Minecraft est une propriété fondamentale de la couche 1. Elle garantit que toute évolution du protocole Minecraft ou de la couche de simulation n'impacte pas le code réseau.

---

## Choix technologique : libp2p

libp2p est retenu plutôt que WebRTC pour plusieurs raisons concrètes.

**WebRTC** a été conçu pour les navigateurs web. Son stack est volumineux, sa configuration est complexe, et la gestion des NAT symétriques nécessite un serveur TURN — un serveur qui fait transiter toutes les données en permanence, ce qui représente un coût d'infrastructure non négligeable et un point de défaillance central.

**libp2p** est une bibliothèque réseau modulaire, initialement développée pour IPFS et aujourd'hui utilisée dans Ethereum, Filecoin et de nombreux projets distribués. Elle intègre nativement le hole punching, le circuit relay, le chiffrement (protocole Noise), le multiplexage de streams (Yamux) et la gestion automatique du NAT (UPnP). Elle est disponible en Rust via la crate `rust-libp2p`, maintenue par Protocol Labs et la communauté Rust.

L'implémentation en **Rust** est particulièrement bien adaptée à ce projet : la crate `rust-libp2p` est mature, activement maintenue, et tire pleinement parti du modèle de concurrence asynchrone de Rust via Tokio. Les performances sont excellentes pour un agent qui doit transporter des données en temps réel avec une latence minimale, et la gestion mémoire sans garbage collector élimine les pauses imprévisibles — un avantage direct pour un jeu à 20 ticks par seconde.

---

## Le relay server

Le relay server est délibérément minimaliste. Son seul rôle est de stocker temporairement les informations de connexion d'un peer (PeerID, multiaddrs, code court) et de les restituer à l'autre peer qui présente le bon code.

Il ne stocke aucune donnée de jeu. Il ne voit pas le contenu du tunnel libp2p (les données sont chiffrées de bout en bout). Il peut être arrêté une fois la session démarrée sans affecter le jeu en cours, sauf si la connexion relayée est utilisée en fallback.

Une instance unique suffit pour gérer des dizaines de sessions simultanées, car la charge par session est négligeable (quelques kilooctets au moment du handshake, puis silence). Le serveur peut être hébergé sur le plan gratuit de n'importe quel fournisseur cloud.

---

## Gestion des cas dégradés

**Déconnexion temporaire d'un peer** — si un peer perd la connexion pendant moins de trente secondes, l'agent tente une reconnexion automatique. Le tunnel libp2p et le swarm gèrent cette reconnexion sans intervention du joueur. Minecraft peut afficher un message de lag, mais la session reprend sans rechargement.

**Déconnexion prolongée** — au-delà d'un seuil configurable (trente secondes par défaut), les chunks owné par le peer déconnecté sont considérés orphelins. La couche 2 (ownership engine) prend en charge le transfert de propriété vers le peer le plus proche. La couche 1 continue de tenter la reconnexion en arrière-plan.

**NAT symétrique** — couvert par le circuit relay en fallback. La connexion reste fonctionnelle, seule la latence augmente de quelques dizaines de millisecondes. Ce cas concerne principalement les réseaux d'entreprise et certains opérateurs mobiles.

**Pair malveillant** — dans notre modèle basé sur la confiance, ce cas n'est pas géré au niveau de la couche 1. Le chiffrement Noise garantit l'authenticité du PeerID (un pair ne peut pas usurper l'identité d'un autre), mais la validation du contenu des données reste volontairement absente. C'est un choix de conception explicite, cohérent avec l'hypothèse de confiance mutuelle entre amis.

---

## Ce que cette couche ne fait pas

La couche 1 ne connaît pas Minecraft. Elle ne parse pas les paquets, ne gère pas les chunks, n'implémente aucune logique de jeu. Elle ne sait pas combien de joueurs sont connectés ni ce qu'ils font.

Elle ne gère pas non plus l'authentification Mojang, la vérification de licence, ou la compatibilité de version. Ces questions relèvent du client Minecraft lui-même ou de couches supérieures.

Son périmètre est strictement : établir un tunnel TCP transparent entre deux agents, le maintenir fiable, et le réparer automatiquement en cas de coupure.

---

## Dépendances externes

| Dépendance | Rôle | Hébergement |
|---|---|---|
| Relay server | Signaling initial | Instance cloud légère (plan gratuit) |
| Crate rust-libp2p | Transport P2P | Crate Rust, embarquée dans l'agent |
| Protocole Noise | Chiffrement du tunnel | Intégré à libp2p |
| Yamux | Multiplexage de streams | Intégré à libp2p |
| UPnP (optionnel) | Ouverture automatique de port | Router du joueur |

Le relay server est la seule infrastructure externe à maintenir. Toutes les autres dépendances sont embarquées dans l'agent et ne nécessitent aucune configuration côté joueur.

---

## Estimation de charge réseau

La couche 1 elle-même génère très peu de trafic propre : quelques kilooctets lors du handshake, puis des paquets de keepalive légers toutes les dix secondes pour maintenir le tunnel ouvert à travers les NAT.

Le trafic effectif sur le tunnel est celui des couches supérieures : les deltas de simulation, les états des entités, les synchronisations de bordure. Ces volumes sont détaillés dans la documentation de la couche 3.

---

*Documentation rédigée pour le projet YuyuFrame — Architecture P2P Minecraft basée sur la confiance.*  
*Couche suivante : [Couche 2 — Ownership Engine](./README_couche2_ownership_engine.md)*