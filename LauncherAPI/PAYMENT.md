# Logique de paiement — YuyuFrame LauncherAPI

## Vue d'ensemble

YuyuFrame utilise un système de paiement **entièrement automatisé** basé sur des webhooks.
L'utilisateur paie sur une page hébergée par le processeur de paiement (Stripe ou Lemon Squeezy),
et le plan est activé **sans intervention manuelle** via un webhook signé.

Les données de carte bancaire ne transitent **jamais** par la LauncherAPI ni par le client Tauri.

---

## Flux de paiement complet

```
┌─────────────┐        ┌──────────────────┐        ┌─────────────────────┐
│  App Tauri  │        │   LauncherAPI    │        │  Stripe / LemonSq.  │
└──────┬──────┘        └────────┬─────────┘        └──────────┬──────────┘
       │                        │                             │
       │  POST /payments/create-checkout                      │
       │  { user_id, plan }     │                             │
       │───────────────────────>│                             │
       │                        │  Crée session checkout      │
       │                        │  metadata: { user_id, plan}│
       │                        │────────────────────────────>│
       │                        │  ← checkout_url             │
       │  ← { checkout_url }    │                             │
       │                        │                             │
       │  Ouvre checkout_url    │                             │
       │  dans le navigateur    │          Utilisateur paie   │
       │                        │                             │
       │  Poll GET /auth/me     │   POST /stripe/webhook      │
       │  toutes les 3 secondes │<────────────────────────────│
       │                        │  Vérifie signature HMAC     │
       │                        │  Vérifie idempotency        │
       │                        │  UPDATE users SET plan      │
       │                        │  ← 200 OK                   │
       │                        │                             │
       │  GET /auth/me          │                             │
       │───────────────────────>│                             │
       │  ← { plan: "premium" } │                             │
       │  → Affiche plan actif  │                             │
```

---

## Ce qui est implémenté

### Vérification de signature webhook (`routes/stripe.rs`)

Implémentation HMAC-SHA256 selon la spec Stripe :

```
signed_payload = timestamp + "." + raw_body
signature      = hex(HMAC-SHA256(STRIPE_WEBHOOK_SECRET, signed_payload))
```

- Le `raw_body` est utilisé **avant** désérialisation JSON (obligatoire)
- La signature fournie est comparée à celle calculée

### Événements webhook gérés

| Événement | Action |
|-----------|--------|
| `checkout.session.completed` | Active le plan — expiration dans 31 jours |
| `invoice.paid` | Renouvelle le plan — expiration dans 31 jours |
| `customer.subscription.deleted` | Rétrograde vers `free` |
| `invoice.payment_failed` | Rétrograde vers `free` |

### Activation via admin (`routes/admin.rs`)

Pour activer un plan manuellement (tests, support) :

```http
PUT /admin/users/{id}/plan
X-Admin-Secret: <ADMIN_SECRET>
Content-Type: application/json

{ "plan": "premium", "expires_at": null }
```

`expires_at` : timestamp Unix (null = permanent).

---

## Ce qui reste à implémenter avant la mise en production

### 1. Idempotency des webhooks (CRITIQUE)

Les processeurs de paiement **rejouent les webhooks** pendant 3 jours en cas de timeout
(Stripe : backoff exponentiel sur 72h, jusqu'à ~15 tentatives).
Sans idempotency, un utilisateur peut être facturé ou activé plusieurs fois.

**Schéma DB à ajouter :**

```sql
CREATE TABLE webhook_events (
    event_id    TEXT    PRIMARY KEY,
    processor   TEXT    NOT NULL,
    event_type  TEXT    NOT NULL,
    processed_at INTEGER NOT NULL
);
```

**Logique à ajouter dans le handler avant tout traitement :**

```rust
// 1. Extraire l'event_id depuis le payload JSON
let event_id = event["id"].as_str().unwrap_or("");

// 2. Vérifier si déjà traité
if db::webhook_event_exists(&conn, event_id)? {
    return Ok(StatusCode::OK); // Déjà traité, on ignore
}

// 3. Traiter l'événement
handle_payment_success(&state, &event)?;

// 4. Marquer comme traité (dans la même transaction idéalement)
db::insert_webhook_event(&conn, event_id, "stripe", event_type)?;
```

### 2. Protection anti-replay (CRITIQUE)

Stripe inclut un timestamp `t=` dans la signature. Un webhook valide mais vieux
peut être rejoué par un attaquant qui l'a intercepté.

**Vérification à ajouter dans `verify_signature` :**

```rust
let ts: i64 = timestamp.parse().map_err(|_| ())?;
let now = chrono::Utc::now().timestamp();
if (now - ts).abs() > 300 {
    return Err(()); // Fenêtre de 5 minutes
}
```

### 3. Endpoint `POST /payments/create-checkout`

Doit :
1. Recevoir `{ user_id, plan }` avec JWT utilisateur
2. Appeler l'API Stripe/Lemon Squeezy pour créer une session
3. Passer `metadata: { user_id, plan }` — c'est ce que le webhook récupère
4. Retourner `{ checkout_url }` à l'app Tauri

**Variables d'environnement nécessaires :**

```env
STRIPE_SECRET_KEY=sk_live_...
# ou pour Lemon Squeezy :
LEMON_SQUEEZY_API_KEY=...
LEMON_SQUEEZY_STORE_ID=...
LEMON_SQUEEZY_VARIANT_PREMIUM=...   # variant_id du plan Premium
LEMON_SQUEEZY_VARIANT_ULTIMATE=...  # variant_id du plan Ultimate
```

### 4. Polling côté app Tauri

Après ouverture du checkout dans le navigateur, l'app doit détecter
l'activation automatiquement sans que l'utilisateur ait à cliquer
sur "Rafraîchir".

```ts
// Dans Plans.tsx, après ouverture du checkout_url
const pollPlan = async () => {
  for (let i = 0; i < 20; i++) {         // 20 × 3s = 60s max
    await new Promise(r => setTimeout(r, 3000))
    const resp = await api.yuyu.refreshPlan()
    if (resp.plan !== 'free') {
      setYuyuPlan(resp.plan, resp.plan_expires_at)
      break
    }
  }
}
```

---

## Choix du processeur de paiement

### Lemon Squeezy (recommandé pour MVP)

- **Merchant of Record** : Lemon Squeezy collecte et reverse la TVA européenne à ta place
- Aucune déclaration fiscale EU à gérer
- Setup simple : créer les variantes de produit dans le dashboard, récupérer les `variant_id`
- Format webhook différent de Stripe (voir doc Lemon Squeezy)

### Stripe

- Plus flexible, 40+ types d'événements webhook
- **TVA EU non gérée** : tu es responsable de la collecter et la reverser pays par pays
- Recommandé si l'audience est principalement hors EU ou si tu passes par un comptable

---

## Variables d'environnement

| Variable | Obligatoire | Description |
|----------|-------------|-------------|
| `JWT_SECRET` | ✅ | Clé de signature des JWT utilisateurs |
| `ADMIN_SECRET` | ✅ | Clé pour les routes admin |
| `STRIPE_WEBHOOK_SECRET` | ✅ (si Stripe) | Secret de vérification des webhooks Stripe (`whsec_...`) |
| `STRIPE_SECRET_KEY` | ✅ (si Stripe) | Clé API Stripe côté serveur (`sk_live_...`) |
| `LEMON_SQUEEZY_API_KEY` | ✅ (si LS) | Clé API Lemon Squeezy |
| `LEMON_SQUEEZY_WEBHOOK_SECRET` | ✅ (si LS) | Secret webhook Lemon Squeezy |
| `DB_PATH` | ❌ | Chemin vers `launcher.db` (défaut : `./launcher.db`) |
| `LISTEN_ADDR` | ❌ | Adresse d'écoute (défaut : `0.0.0.0:3000`) |

En développement, copier `.env.example` → `.env` et remplir les valeurs.

---

## Conformité

### PCI DSS
Aucune donnée de carte ne passe par la LauncherAPI ni par le client Tauri.
L'utilisateur saisit ses données directement sur la page hébergée par Stripe ou Lemon Squeezy.
Ce modèle correspond au **niveau SAQ A** (le plus bas) — aucun audit annuel requis.

### RGPD
La LauncherAPI ne stocke que l'identifiant utilisateur interne et le plan activé.
Les données personnelles de paiement (nom, email, adresse de facturation) sont gérées
exclusivement par le processeur de paiement et ne transitent pas par nos serveurs.
Une mention dans la politique de confidentialité suffit : `"Les paiements sont traités par [Stripe / Lemon Squeezy]"`.

### Strong Customer Authentication (SCA / PSD2)
Géré automatiquement par le processeur de paiement (3D Secure 2).
Aucun code supplémentaire requis.

---

## Tests en développement

Sans paiement réel, le plan peut être défini manuellement via l'API admin :

```bash
# Récupérer l'user_id depuis les logs de la LauncherAPI au login,
# puis :
curl -X PUT http://localhost:3000/admin/users/1/plan \
  -H "X-Admin-Secret: changeme-set-ADMIN_SECRET-in-prod" \
  -H "Content-Type: application/json" \
  -d '{"plan": "premium", "expires_at": null}'
```

Ensuite cliquer sur **Rafraîchir mon plan** dans l'app — le plan est mis à jour immédiatement.

Pour simuler un webhook Stripe sans compte Stripe, utiliser la CLI Stripe :

```bash
stripe listen --forward-to localhost:3000/stripe/webhook
stripe trigger checkout.session.completed
```
