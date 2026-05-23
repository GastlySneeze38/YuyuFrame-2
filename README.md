# Modèle de Tarification - Launcher Minecraft

## 🎯 Vision Générale

Ce launcher résout **trois problèmes distincts** pour trois segments d'utilisateurs différents :

1. **Joueurs solo** → Installation simplifiée de modpacks
2. **Joueurs multi-PC** → Synchronisation et confort
3. **Hébergeurs de serveurs** → Infrastructure serveur clé en main

Chaque niveau de tarification correspond à un niveau de besoin croissant.

---

## 📦 Niveaux d'Abonnement

### 🆓 GRATUIT
**Pour : Les joueurs occasionnels**

**Fonctionnalités :**
- ✅ Installation et lancement de modpacks
- ✅ Mode solo et LAN classique
- ✅ Auto-update automatique des mods
- ✅ Interface moderne et intuitive
- ✅ 3 profils maximum
- ✅ Stats basiques (temps de jeu total)

**Limite :** Configuration locale uniquement, pas de synchronisation multi-PC, profils limités à 3.

---

### ⭐ PREMIUM - 7.99€/mois
**Pour : Les joueurs réguliers sur plusieurs machines**

**Problème résolu :** *"J'ai un PC fixe et un portable, je perds toujours mes configs/saves"*

**Fonctionnalités :**

#### 🔄 Synchronisation Multi-PC
- Configs, saves et mods synchronisés automatiquement
- Lance ton jeu sur n'importe quel PC avec tes données
- Changement de machine transparent
- Limite : 3 saves maximum

#### 🎮 Compte Minecraft Illimités
- Ajoute autant de compte que tu veux (vs 2 en gratuit)

#### 📊 Stats & Analytics Personnelles
- Temps de jeu détaillé par modpack
- Achievements et progression
- Historique d'activité complet
- Graphiques et visualisations

#### 🎨 Personnalisation Avancée
-ajout du sound disigne

**Pourquoi Premium ?**
- Prix accessible (5€ = 2 cafés)
- Sync multi-PC résout un vrai problème

---

### 🚀 ULTIMATE - 15.99€/mois
**Pour : Les créateurs de serveurs et communautés**

**Problème résolu :** *"Je veux héberger un serveur sans galère technique ni frais d'hébergement"*

**Inclut :** Tout Premium + Infrastructure Serveur Complète

#### 🎮 Serveur en 1 Clic
- Configuration automatique sur ton PC
- Port forwarding automatique (UPnP)
- Démarrage/arrêt depuis le launcher
- Pas besoin de compétences techniques

#### 🌐 URL Personnalisée
- Domaine automatique : `pseudo.votrelauncher.fr`
- Pas besoin de donner ton IP
- Professionnel et mémorisable

#### 🔗 Tunnel Proxy Garanti
- Fonctionne même avec NAT bloqué
- Ton PC devient serveur accessible de partout
- Pas besoin d'ouvrir de ports manuellement
- Infrastructure proxy garantie

#### ☁️ Backup Serveur Automatique
- Sauvegarde incrémentale du monde
- Protection contre les crashs
- Restauration rapide en cas de problème

#### 🔄 Synchronisation des Mods
- Mods installés automatiquement chez les joueurs
- Mise à jour propagée à tous les clients
- Résolution automatique des conflits
- Expérience fluide pour tes joueurs

#### 👥 Gestion des parametre de server depuis le launcher
- Invitations par lien partageable

#### 📊 Analytics Serveur
- Activité du serveur en temps réel
- Connexions et temps de jeu par joueur
- Heatmap d'activité
- Top joueurs/builders

**Pourquoi Ultimate ?**
- Remplace un VPS Minecraft (qui coûte 5-10€/mois minimum)
- Tunnel proxy = infrastructure coûteuse
- URL custom + backup = service premium
- Gestion whitelist web = gain de temps énorme

---

### Nouveau modèle (optimisé)
- **Gratuit :** launcher + auto-update + 3 profils
- **Premium 5€ :** sync multi-PC + profils illimités + stats avancées ✅
- **Ultimate 15€ :** infrastructure serveur complète ✅

**Avantages :**
- ✅ Chaque niveau résout un problème distinct
- ✅ Premium abordable avec sync multi-PC (vraie valeur)
- ✅ Gratuit déjà solide (auto-update inclus)
- ✅ Ultimate justifie son prix (remplace VPS + infrastructure)
- ✅ Pas de features coûteuses en ressources dans Premium

---

## 🎯 Positionnement Concurrentiel

### vs Hébergeurs Classiques (5-20€/mois)
**Notre avantage :**
- Serveur sur TON PC (pas de limite de RAM/CPU)
- Pas de frais d'hébergement mensuel
- Contrôle total de la machine

### vs Solutions DIY
**Notre avantage :**
- Port forwarding automatique
- Tunnel proxy pour NAT bloqués
- Backup cloud automatique
- Whitelist web
- Pas besoin de compétences techniques

---

## 📈 Stratégie de Conversion

### Gratuit → Premium
**Déclencheurs :**
- "Joues-tu sur plusieurs PCs ?" → Sync multi-PC
- "Tu manques d'espace pour tes profils ?" → Profils illimités
- "Tu veux voir tes stats détaillées ?" → Analytics avancées
- "Tu veux personnaliser ton launcher ?" → Thèmes custom
- "Tes téléchargements sont lents ?" → Priorité de téléchargement

### Premium → Ultimate
**Déclencheurs :**
- "Veux-tu jouer avec des amis ?" → Serveur en 1 clic
- "Ton PC ne peut pas ouvrir de ports ?" → Tunnel proxy
- "Tu veux ton propre serveur sans frais ?" → Infrastructure complète
- "Tu veux gérer facilement la whitelist ?" → Interface web

---

## 📝 Notes de Développement

### Ordre de Priorité
1. **Phase 1 :** Gratuit + Premium (sync + backup)
2. **Phase 2 :** Ultimate (serveur basique)
3. **Phase 3 :** Features avancées (tunnel, analytics)

### Métriques à Suivre
- Taux de conversion Gratuit → Premium
- Taux de conversion Premium → Ultimate
- Taux de churn par niveau
- Features les plus utilisées par niveau

### Contraintes d'Infrastructure
**Pourquoi pas de backup cloud dans Premium :**
- Coût de stockage élevé (saves Minecraft = plusieurs GB)
- Bande passante importante pour les syncs
- Backup serveur dans Ultimate justifié (plan premium à 15€)
- Premium reste léger en ressources = marges saines

**Features choisies pour Premium (faible coût) :**
- Sync multi-PC : uniquement configs + métadonnées (quelques MB)
- Stats personnelles : juste des compteurs en base de données
- Personnalisation : préférences stockées localement
- Priorité téléchargement : QoS logicielle (pas de surcoût)