# YuyuFrame — Minecraft Agent (Hybride Java + Rust)

## Concept

Un **Java Agent** injecté par YuyuFrame au lancement de Minecraft.  
Il modifie les classes de Minecraft à la volée (bytecode) pour ajouter :

- Une interface Modrinth in-game (shaders, textures)
- Des tweaks d'optimisation personnalisés
- Une vérification d'identité liée au compte YuyuFrame

L'agent est **invisible dans le dossier `/mods`** — il est géré exclusivement par le launcher,
stocké dans `%AppData%/YuyuFrame/agent/`, et protégé par un token HMAC éphémère.

Toute la logique lourde (API Modrinth, téléchargements, auth) reste dans le **Backend Rust déjà existant**.
Le Java n'est qu'une fine couche de ~200–400 lignes dont le seul rôle est de s'accrocher à la JVM.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Launcher Tauri (Rust — Backend/)                               │
│                                                                 │
│  • Génère le token HMAC (secret + timestamp + instance_id)      │
│  • Stocke yuyu-agent.jar dans %AppData%/YuyuFrame/agent/        │
│  • Ouvre un socket TCP local (127.0.0.1:3848)                   │
│  • Lance Minecraft avec les JVM args ci-dessous                 │
└────────────────┬────────────────────────────────────────────────┘
                 │ -javaagent:%AppData%/YuyuFrame/agent/yuyu-agent.jar
                 │ -Dyuyu.token=<HMAC>
                 │ -Dyuyu.expire=<timestamp+60s>
                 │ -Dyuyu.port=3848
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  JVM Minecraft                                                  │
│                                                                 │
│  yuyu-agent.jar (Java — yuyu-agent/)                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 1. premain() → vérifie HMAC, refuse si invalide/expiré  │   │
│  │ 2. Enregistre MixinBootstrap (injection bytecode)       │   │
│  │ 3. Ouvre connexion TCP → 127.0.0.1:3848 (Rust)          │   │
│  │ 4. Injecte via Mixin :                                   │   │
│  │    • Bouton "Yuyu" dans TitleScreen                     │   │
│  │    • Écran custom → envoie requêtes au Rust             │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  Fabric / Forge (si actif) — coexistent normalement            │
└─────────────────────────────────────────────────────────────────┘
                 │ TCP 127.0.0.1:3848 (JSON)
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│  Backend Rust — YuyuAgentServer (nouveau module)                │
│                                                                 │
│  • Reçoit les requêtes de l'agent                               │
│  • Appelle l'API Modrinth (api.modrinth.com/v2)                 │
│  • Télécharge shaders → .minecraft/shaderpacks/                 │
│  • Télécharge textures → .minecraft/resourcepacks/              │
│  • Valide le JWT YuyuFrame pour les features premium            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Protocole de communication Java ↔ Rust

Communication par **JSON sur TCP**, newline-delimited (une ligne = un message).

### Requêtes envoyées par l'agent (Java → Rust)

```json
{ "type": "search", "category": "shader", "query": "complementary", "limit": 20 }
{ "type": "search", "category": "resourcepack", "query": "faithful", "limit": 20 }
{ "type": "download", "project_id": "AANobbMI", "version_id": "xyz", "filename": "Complementary.zip" }
{ "type": "list_installed", "category": "shader" }
{ "type": "ping" }
```

### Réponses envoyées par Rust (Rust → Java)

```json
{ "type": "search_result", "items": [ { "id": "...", "name": "...", "description": "...", "icon_url": "...", "downloads": 1234567 } ] }
{ "type": "download_progress", "filename": "Complementary.zip", "percent": 42 }
{ "type": "download_done", "filename": "Complementary.zip" }
{ "type": "error", "message": "Modrinth API timeout" }
{ "type": "pong" }
```

---

## Structure du projet

```
YuyuFrame 2/
├── Backend/                        ← Rust/Tauri existant
│   └── src/
│       ├── commands/
│       │   └── agent_server.rs     ← NOUVEAU : socket TCP 3848 + bridge Modrinth
│       └── minecraft/
│           └── launcher.rs         ← MODIFIER : ajouter -javaagent dans build_jvm_args
│
└── yuyu-agent/                     ← NOUVEAU : projet Java (Gradle)
    ├── build.gradle
    ├── src/main/java/fr/yuyuframe/agent/
    │   ├── YuyuAgent.java          ← premain() + vérif HMAC
    │   ├── AgentConnection.java    ← socket TCP vers Rust
    │   ├── ModrinthScreen.java     ← GUI Minecraft custom
    │   └── mixin/
    │       └── TitleScreenMixin.java ← injection dans l'écran titre
    └── src/main/resources/
        ├── META-INF/MANIFEST.MF    ← Premain-Class: fr.yuyuframe.agent.YuyuAgent
        └── mixins.yuyu.json
```

---

## Java Agent — Détails techniques

### Dépendances (`build.gradle`)

```groovy
dependencies {
    // Minecraft au compile time (provided — déjà dans la JVM au runtime)
    compileOnly "com.mojang:minecraft:1.21.4:client-extra"

    // Mixin standalone — injection bytecode sans Fabric
    implementation "org.spongepowered:mixin:0.8.5"

    // ASM (transitif via Mixin, mais déclaré explicitement)
    implementation "org.ow2.asm:asm:9.7"
    implementation "org.ow2.asm:asm-tree:9.7"
}

jar {
    manifest {
        attributes(
            "Premain-Class": "fr.yuyuframe.agent.YuyuAgent",
            "Can-Redefine-Classes": "true",
            "Can-Retransform-Classes": "true"
        )
    }
    // Fat JAR : Mixin + ASM inclus dans l'agent
    from { configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
}
```

### `YuyuAgent.java` — Structure

```java
public class YuyuAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        // 1. Lire les propriétés système
        String token  = System.getProperty("yuyu.token",  "");
        String expire = System.getProperty("yuyu.expire", "0");
        int    port   = Integer.parseInt(System.getProperty("yuyu.port", "3848"));

        // 2. Vérifier le token HMAC-SHA256
        if (!TokenValidator.isValid(token, expire)) {
            System.err.println("[YuyuAgent] Token invalide — agent désactivé");
            return; // Silencieux pour l'utilisateur
        }

        // 3. Connexion TCP vers le Backend Rust
        AgentConnection.connect(port);

        // 4. Démarrer les Mixins
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("mixins.yuyu.json");
    }
}
```

### `TitleScreenMixin.java` — Injection du bouton

```java
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    @Inject(method = "init", at = @At("TAIL"))
    private void addYuyuButton(CallbackInfo ci) {
        // Ajoute un bouton "Mods Yuyu" sous les boutons existants
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Mods Yuyu"),
            btn -> this.client.setScreen(new ModrinthScreen(this))
        ).dimensions(this.width / 2 - 100, this.height / 4 + 120, 200, 20).build());
    }
}
```

---

## Rust Backend — Nouveaux éléments

### `commands/agent_server.rs` — Socket TCP

```rust
use tokio::net::TcpListener;
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};

pub async fn start_agent_server(app: tauri::AppHandle) {
    let listener = TcpListener::bind("127.0.0.1:3848").await.unwrap();

    tokio::spawn(async move {
        loop {
            let Ok((stream, _)) = listener.accept().await else { continue };
            let app = app.clone();
            tokio::spawn(handle_agent_client(stream, app));
        }
    });
}

async fn handle_agent_client(stream: tokio::net::TcpStream, app: tauri::AppHandle) {
    let (reader, mut writer) = stream.into_split();
    let mut lines = BufReader::new(reader).lines();

    while let Ok(Some(line)) = lines.next_line().await {
        let Ok(msg) = serde_json::from_str::<serde_json::Value>(&line) else { continue };

        let response = match msg["type"].as_str() {
            Some("search")   => handle_search(&msg).await,
            Some("download") => handle_download(&msg, &app).await,
            Some("ping")     => Ok(serde_json::json!({ "type": "pong" })),
            _                => Err("unknown message type".to_string()),
        };

        let out = match response {
            Ok(v)  => v.to_string(),
            Err(e) => serde_json::json!({ "type": "error", "message": e }).to_string(),
        };

        let _ = writer.write_all(format!("{}\n", out).as_bytes()).await;
    }
}
```

### Modrinth API depuis Rust

```rust
async fn handle_search(msg: &serde_json::Value) -> Result<serde_json::Value, String> {
    let query    = msg["query"].as_str().unwrap_or("");
    let category = msg["category"].as_str().unwrap_or("shader");
    let limit    = msg["limit"].as_u64().unwrap_or(20);

    // Facets Modrinth : "shader" → shaderpacks, "resourcepack" → texture packs
    let facets = match category {
        "shader"       => r#"[["project_type:shader"]]"#,
        "resourcepack" => r#"[["project_type:resourcepack"]]"#,
        _              => r#"[["project_type:mod"]]"#,
    };

    let url = format!(
        "https://api.modrinth.com/v2/search?query={}&facets={}&limit={}",
        urlencoding::encode(query), urlencoding::encode(facets), limit
    );

    let client = reqwest::Client::builder()
        .user_agent("YuyuFrame/2.0 (contact@yuyuframe.fr)")
        .build().map_err(|e| e.to_string())?;

    let data: serde_json::Value = client.get(&url).send().await
        .map_err(|e| e.to_string())?
        .json().await
        .map_err(|e| e.to_string())?;

    Ok(serde_json::json!({ "type": "search_result", "items": data["hits"] }))
}
```

---

## Intégration dans le launcher existant

### Modification de `launcher.rs`

Le point d'injection est `build_jvm_args` ([Backend/src/minecraft/launcher.rs:437](Backend/src/minecraft/launcher.rs#L437)).  
Et l'assemblage final des args ligne [265](Backend/src/minecraft/launcher.rs#L265).

```rust
// Dans download_and_launch(), avant la construction de args :
let agent_args = build_agent_args(); // retourne Vec<String> avec -javaagent et -D props
let mut args = build_jvm_args(ram_mb, &natives_dir, java_major);
args.extend(agent_args);             // inséré avant -cp
args.extend(extra_jvm_args);
// ...
```

```rust
pub fn build_agent_args() -> Vec<String> {
    use std::time::{SystemTime, UNIX_EPOCH};
    use hmac::{Hmac, Mac};
    use sha2::Sha256;

    let agent_path = dirs::data_dir()
        .unwrap_or_default()
        .join("YuyuFrame")
        .join("agent")
        .join("yuyu-agent.jar");

    if !agent_path.exists() {
        return vec![]; // Agent pas encore installé, on passe silencieusement
    }

    // Token éphémère valable 60 secondes
    let expire = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs() + 60;

    let secret = b"YUYU_HMAC_SECRET_CHANGE_IN_PROD"; // → env var en prod
    let mut mac = Hmac::<Sha256>::new_from_slice(secret).unwrap();
    mac.update(expire.to_string().as_bytes());
    let token = hex::encode(mac.finalize().into_bytes());

    vec![
        format!("-javaagent:{}", agent_path.display()),
        format!("-Dyuyu.token={}", token),
        format!("-Dyuyu.expire={}", expire),
        "-Dyuyu.port=3848".to_string(),
    ]
}
```

---

## Protection et sécurité

### Token HMAC éphémère

| Propriété | Valeur |
|-----------|--------|
| Algorithme | HMAC-SHA256 |
| Durée de validité | 60 secondes après génération |
| Secret | Variable d'environnement `YUYU_HMAC_SECRET` en prod |
| Vérification | Java Agent recompute le HMAC et compare avec expiry |

Si Minecraft est lancé **sans passer par YuyuFrame**, le token est absent → l'agent se désactive silencieusement.

### Stockage de l'agent

```
%AppData%/YuyuFrame/agent/yuyu-agent.jar   ← Windows
~/.local/share/YuyuFrame/agent/            ← Linux
~/Library/Application Support/YuyuFrame/  ← macOS
```

Jamais dans `.minecraft/mods/`. L'utilisateur ne le voit pas dans l'interface de mods Minecraft.

### Obfuscation

Build pipeline : `Gradle → fat JAR → ProGuard → yuyu-agent-obf.jar`

```
# proguard.pro
-keep class fr.yuyuframe.agent.YuyuAgent { public static void premain(...); }
-keep class **.mixin.** { *; }  # Mixins doivent rester lisibles par ASM
-obfuscationdictionary dict.txt
-classobfuscationdictionary dict.txt
```

Le secret HMAC dans le bytecode obfusqué est lisible par décompilation avancée → en production,
la validation finale se fait côté serveur (le Rust peut vérifier le JWT YuyuFrame en plus du HMAC).

---

## Compatibilité Minecraft

| Scénario | Fonctionne ? |
|----------|-------------|
| Vanilla | Oui |
| Fabric  | Oui — l'agent se charge avant Fabric, coexistent |
| Forge   | Oui — même principe |
| Fabric + Forge (rare) | Oui |
| Version < 1.16 | Oui si Mixin compatible (MC 1.8+) |

---

## Plan d'implémentation

| Étape | Fichier(s) | Description |
|-------|-----------|-------------|
| 1 | `yuyu-agent/` | Créer le projet Gradle, `premain()` vide + vérif HMAC |
| 2 | `launcher.rs` | Ajouter `build_agent_args()` et l'insérer dans les JVM args |
| 3 | `agent_server.rs` | Socket TCP 3848, handler ping/pong |
| 4 | `agent_server.rs` | Intégrer l'API Modrinth (search shaders + textures) |
| 5 | `TitleScreenMixin.java` | Injection du bouton dans l'écran titre |
| 6 | `ModrinthScreen.java` | Interface in-game : liste, recherche, download |
| 7 | Build pipeline | Gradle fat JAR + ProGuard + copie dans AppData |
| 8 | Tests | Lancer MC depuis YuyuFrame, vérifier bouton visible |

---

## APIs Modrinth utilisées

```
GET https://api.modrinth.com/v2/search
    ?query=<terme>
    &facets=[["project_type:shader"]]   # ou resourcepack
    &limit=20
    &offset=0

GET https://api.modrinth.com/v2/project/{id}/version
    ?loaders=["iris","optifine"]        # pour shaders
    &game_versions=["1.21.4"]

GET https://api.modrinth.com/v2/version/{id}
    → files[].url : URL de téléchargement CDN
    → files[].filename
```

Destination des téléchargements :
- Shaders → `<game_dir>/shaderpacks/`
- Textures → `<game_dir>/resourcepacks/`

(`game_dir` = répertoire de l'instance, ex. `%AppData%/YuyuFrame/.minecraft/instances/<id>/`)
