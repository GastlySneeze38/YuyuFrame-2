# YuyuFrame — Roadmap Freemium

Priorités d'implémentation pour rendre le modèle freemium viable.
Chaque phase débloque la suivante — ne pas sauter d'étape.

---

## Phase 0 — Infrastructure comptes cloud (prérequis absolu)

**Pourquoi en premier :** Le compte YuyuFrame est actuellement 100% local (SQLite).
Sans backend cloud, impossible de gérer les abonnements ni de synchroniser quoi que ce soit.

### À implémenter

- [ ] **API backend** (Node/Fastify ou Rust/Axum) avec base de données cloud (PostgreSQL)
- [ ] **Authentification YuyuFrame cloud** — migrer de la DB locale vers JWT + refresh token via API
- [ ] **Champ `plan`** sur chaque compte : `free | premium | ultimate`
- [ ] **Middleware de feature gating** — chaque appel API vérifie le plan avant d'exécuter

### Résultat attendu
Un utilisateur peut créer un compte, se connecter depuis n'importe quel PC,
et le launcher sait quel plan il a.

---

## Phase 1 — Améliorer le tier gratuit (acquisition)

**Pourquoi :** Le tier gratuit doit être *genuinement utile* pour attirer des utilisateurs.
Actuellement il ne fait que lancer Minecraft — n'importe quel launcher fait ça.

### 1.1 Profils / Instances multiples

Ce qui différencie un bon launcher : gérer plusieurs configurations indépendantes.

- [ ] **Modèle `Instance`** — nom, version, loader (Vanilla/Forge/Fabric), dossier dédié, RAM
- [ ] **UI de gestion des instances** — créer, renommer, supprimer, dupliquer
- [ ] **Isolation des dossiers** — chaque instance a son propre `.minecraft` (mods, worlds, configs)
- [ ] **Limite tier gratuit : 3 instances max** (hook vers Premium = instances illimitées)

### 1.2 Bibliothèque de modpacks (CurseForge / Modrinth)

- [ ] **Recherche de modpacks** via API CurseForge + Modrinth
- [ ] **Installation en 1 clic** d'un modpack sur une instance
- [ ] **Mise à jour automatique** des modpacks installés

### 1.3 Système de Quêtes (déjà prévu dans l'UI, section vide)

La section "Quêtes" existe déjà dans [Frontend/src/pages/Home.tsx](Frontend/src/pages/Home.tsx#L447).
C'est un levier de rétention fort et gratuit à activer.

- [ ] **Quêtes d'onboarding** — créer son premier profil, lancer une première fois, installer un mod
- [ ] **Récompenses cosmétiques** (thèmes de launcher, badges de profil) — pas de pay-to-win
- [ ] **Quêtes hebdomadaires** pour maintenir l'engagement

---

## Phase 2 — Système d'abonnement (monétisation)

**Pourquoi maintenant :** Une fois que des utilisateurs actifs sont là (Phase 1),
il faut le tunnel de conversion.

### 2.1 Intégration Stripe

- [ ] **Stripe Checkout** pour souscrire Premium (5€/mois) et Ultimate (10€/mois)
- [ ] **Stripe Customer Portal** pour gérer / annuler l'abonnement
- [ ] **Webhook Stripe** → met à jour le champ `plan` en base en temps réel

### 2.2 UI de gestion du plan dans le launcher

- [ ] **Page "Mon abonnement"** — plan actuel, date de renouvellement, bouton upgrade
- [ ] **Badges visuels** dans la sidebar — couronne Premium / Ultimate visible
- [ ] **Paywall élégant** — quand un utilisateur gratuit touche une feature Premium,
  afficher ce qu'il débloque et un CTA "Passer Premium" (pas un message d'erreur)

---

## Phase 3 — Premier hook Premium : Backup cloud (5€/mois)

**Pourquoi ce feature en premier :** C'est la feature Premium la plus facile à valoriser
("tes mondes ne disparaissent jamais") et la plus facile à implémenter sans infra complexe.

- [ ] **Backup incrémental** des dossiers `saves/` et `mods/` vers S3 ou Cloudflare R2
- [ ] **Limite Free : 0 backup** / **Limite Premium : 30 jours d'historique, 5 Go**
- [ ] **UI de restauration** — liste des snapshots avec date, bouton "Restaurer"
- [ ] **Backup automatique** au lancement et à l'arrêt du jeu

---

## Phase 4 — Feature flagship : Serveur 1-clic (Premium)

C'est la feature la plus différenciante du plan. À implémenter après avoir des abonnés
pour valider la demande.

### 4.1 Lancement local (UPnP)

- [ ] **Détection et lancement** d'un serveur Minecraft sur le PC local
- [ ] **UPnP automatique** pour ouvrir les ports (marche sur ~80% des box)
- [ ] **Interface de contrôle** — démarrer, arrêter, voir les logs, voir les joueurs connectés

### 4.2 Tunnel proxy (Ultimate)

Pour les utilisateurs avec NAT bloqué (FAI, IPv6 only, etc.).

- [ ] **Client tunnel** côté launcher (WebSocket ou WireGuard) vers serveur relay
- [ ] **Infrastructure relay** — VPS avec frpc/frps ou solution custom
- [ ] **Domaine custom** `pseudo.yuyuframe.fr` pointant vers le tunnel (Ultimate uniquement)

---

## Phase 5 — Features sociales (rétention long terme)

À implémenter une fois le modèle économique validé (abonnés payants existants).

- [ ] **Système d'amis** — ajouter par nom YuyuFrame, voir statut en ligne / en jeu
- [ ] **Annuaire de serveurs** — serveurs Premium listés, filtres par modpack et langue
- [ ] **Gestion whitelist web** — inviter par lien, rôles admin/modo/joueur

---

## Résumé des limites par tier

| Feature | Gratuit | Premium 5€/mois | Ultimate 10€/mois |
|---|---|---|---|
| Instances Minecraft | 3 max | Illimitées | Illimitées |
| Modpacks CurseForge/Modrinth | Oui | Oui | Oui |
| Quêtes & cosmétiques | Oui | Oui + exclusifs | Oui + exclusifs |
| Backup cloud | Non | 30 jours / 5 Go | 30 jours / illimité |
| Serveur 1-clic (UPnP) | Non | Oui | Oui |
| Tunnel proxy (NAT bloqué) | Non | Non | Oui |
| Domaine custom | Non | Non | Oui |
| Annuaire de serveurs | Lecture | Listing | Listing prioritaire |

---

## Stack technique recommandée

| Composant | Choix |
|---|---|
| Backend API | Axum (Rust) ou Fastify (Node) |
| Base de données | PostgreSQL (Supabase pour démarrer vite) |
| Stockage backup | Cloudflare R2 (moins cher que S3) |
| Paiement | Stripe |
| Tunnel proxy | frp (Fast Reverse Proxy) sur un VPS |
| Auth cloud | JWT + refresh token, argon2 déjà en place côté Rust |
