use anyhow::Result;
use rusqlite::{params, Connection};

pub fn init(conn: &Connection) -> Result<()> {
    // Migrations pour les DBs existantes
    let _ = conn.execute("ALTER TABLE sync_instances ADD COLUMN save_count INTEGER NOT NULL DEFAULT 0", []);
    let _ = conn.execute("ALTER TABLE users ADD COLUMN plan TEXT NOT NULL DEFAULT 'free'", []);
    let _ = conn.execute("ALTER TABLE users ADD COLUMN plan_expires_at INTEGER", []);

    conn.execute_batch(
        "PRAGMA foreign_keys = ON;
         CREATE TABLE IF NOT EXISTS users (
             id              INTEGER PRIMARY KEY AUTOINCREMENT,
             username        TEXT    UNIQUE NOT NULL,
             password_hash   TEXT    NOT NULL,
             plan            TEXT    NOT NULL DEFAULT 'free',
             plan_expires_at INTEGER,
             created_at      INTEGER NOT NULL
         );
         CREATE TABLE IF NOT EXISTS sync_instances (
             id            INTEGER PRIMARY KEY AUTOINCREMENT,
             user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
             instance_name TEXT    NOT NULL,
             mc_version    TEXT    NOT NULL,
             loader        TEXT    NOT NULL,
             ram_mb        INTEGER NOT NULL DEFAULT 4096,
             save_count    INTEGER NOT NULL DEFAULT 0,
             updated_at    INTEGER NOT NULL,
             UNIQUE(user_id, instance_name)
         );
         CREATE TABLE IF NOT EXISTS sync_data (
             sync_instance_id INTEGER PRIMARY KEY REFERENCES sync_instances(id) ON DELETE CASCADE,
             data_zip         BLOB    NOT NULL,
             updated_at       INTEGER NOT NULL
         );",
    )?;
    Ok(())
}

pub struct User {
    pub id: i64,
    pub username: String,
    pub password_hash: String,
    pub plan: String,
    pub plan_expires_at: Option<i64>,
}

pub fn create_user(conn: &Connection, username: &str, password_hash: &str) -> Result<i64> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO users (username, password_hash, created_at) VALUES (?1, ?2, ?3)",
        params![username, password_hash, now],
    )?;
    Ok(conn.last_insert_rowid())
}

pub fn get_user(conn: &Connection, username: &str) -> Result<Option<User>> {
    let mut stmt = conn.prepare(
        "SELECT id, username, password_hash, plan, plan_expires_at FROM users WHERE username = ?1",
    )?;
    match stmt.query_row(params![username], |r| {
        Ok(User {
            id: r.get(0)?,
            username: r.get(1)?,
            password_hash: r.get(2)?,
            plan: r.get::<_, String>(3).unwrap_or_else(|_| "free".into()),
            plan_expires_at: r.get(4)?,
        })
    }) {
        Ok(u) => Ok(Some(u)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn get_user_by_id(conn: &Connection, id: i64) -> Result<Option<User>> {
    let mut stmt = conn.prepare(
        "SELECT id, username, password_hash, plan, plan_expires_at FROM users WHERE id = ?1",
    )?;
    match stmt.query_row(params![id], |r| {
        Ok(User {
            id: r.get(0)?,
            username: r.get(1)?,
            password_hash: r.get(2)?,
            plan: r.get::<_, String>(3).unwrap_or_else(|_| "free".into()),
            plan_expires_at: r.get(4)?,
        })
    }) {
        Ok(u) => Ok(Some(u)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

/// Vérifie si l'utilisateur a un plan actif (premium ou ultimate non expiré).
pub fn check_premium(conn: &Connection, user_id: i64) -> Result<bool> {
    let row = conn.query_row(
        "SELECT plan, plan_expires_at FROM users WHERE id = ?1",
        params![user_id],
        |r| Ok((r.get::<_, String>(0)?, r.get::<_, Option<i64>>(1)?)),
    );
    match row {
        Ok((plan, expires_at)) => {
            let is_paid = plan == "premium" || plan == "ultimate";
            let not_expired = expires_at
                .map(|exp| exp > chrono::Utc::now().timestamp())
                .unwrap_or(true);
            Ok(is_paid && not_expired)
        }
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(false),
        Err(e) => Err(e.into()),
    }
}

/// Retourne le plan actif de l'utilisateur ("free" si expiré ou inconnu).
pub fn get_active_plan(conn: &Connection, user_id: i64) -> Result<String> {
    let row = conn.query_row(
        "SELECT plan, plan_expires_at FROM users WHERE id = ?1",
        params![user_id],
        |r| Ok((r.get::<_, String>(0)?, r.get::<_, Option<i64>>(1)?)),
    );
    match row {
        Ok((plan, expires_at)) => {
            let is_paid = plan == "premium" || plan == "ultimate";
            let not_expired = expires_at
                .map(|exp| exp > chrono::Utc::now().timestamp())
                .unwrap_or(true);
            if is_paid && not_expired { Ok(plan) } else { Ok("free".into()) }
        }
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok("free".into()),
        Err(e) => Err(e.into()),
    }
}

pub fn set_user_plan(
    conn: &Connection,
    user_id: i64,
    plan: &str,
    expires_at: Option<i64>,
) -> Result<()> {
    conn.execute(
        "UPDATE users SET plan = ?1, plan_expires_at = ?2 WHERE id = ?3",
        params![plan, expires_at, user_id],
    )?;
    Ok(())
}

// ── Sync instances ─────────────────────────────────────────────────────────────

pub struct SyncInstanceRow {
    pub id: i64,
    pub user_id: i64,
    pub instance_name: String,
    pub mc_version: String,
    pub loader: String,
    pub ram_mb: u32,
    pub save_count: u32,
    pub has_data: bool,
    pub updated_at: i64,
}

pub fn sync_list(conn: &Connection, user_id: i64) -> Result<Vec<SyncInstanceRow>> {
    let mut stmt = conn.prepare(
        "SELECT si.id, si.user_id, si.instance_name, si.mc_version, si.loader, si.ram_mb,
                si.save_count, (sd.sync_instance_id IS NOT NULL) as has_data, si.updated_at
         FROM sync_instances si
         LEFT JOIN sync_data sd ON sd.sync_instance_id = si.id
         WHERE si.user_id = ?1
         ORDER BY si.updated_at DESC",
    )?;
    let rows = stmt
        .query_map(params![user_id], |r| {
            Ok(SyncInstanceRow {
                id: r.get(0)?,
                user_id: r.get(1)?,
                instance_name: r.get(2)?,
                mc_version: r.get(3)?,
                loader: r.get(4)?,
                ram_mb: r.get::<_, u32>(5)?,
                save_count: r.get::<_, u32>(6)?,
                has_data: r.get::<_, i64>(7)? != 0,
                updated_at: r.get(8)?,
            })
        })?
        .collect::<rusqlite::Result<Vec<_>>>()?;
    Ok(rows)
}

pub fn sync_get(conn: &Connection, id: i64) -> Result<Option<SyncInstanceRow>> {
    let mut stmt = conn.prepare(
        "SELECT si.id, si.user_id, si.instance_name, si.mc_version, si.loader, si.ram_mb,
                si.save_count, (sd.sync_instance_id IS NOT NULL) as has_data, si.updated_at
         FROM sync_instances si
         LEFT JOIN sync_data sd ON sd.sync_instance_id = si.id
         WHERE si.id = ?1",
    )?;
    match stmt.query_row(params![id], |r| {
        Ok(SyncInstanceRow {
            id: r.get(0)?,
            user_id: r.get(1)?,
            instance_name: r.get(2)?,
            mc_version: r.get(3)?,
            loader: r.get(4)?,
            ram_mb: r.get::<_, u32>(5)?,
            save_count: r.get::<_, u32>(6)?,
            has_data: r.get::<_, i64>(7)? != 0,
            updated_at: r.get(8)?,
        })
    }) {
        Ok(row) => Ok(Some(row)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn sync_get_by_name(
    conn: &Connection,
    user_id: i64,
    instance_name: &str,
) -> Result<Option<SyncInstanceRow>> {
    let mut stmt = conn.prepare(
        "SELECT si.id, si.user_id, si.instance_name, si.mc_version, si.loader, si.ram_mb,
                si.save_count, (sd.sync_instance_id IS NOT NULL) as has_data, si.updated_at
         FROM sync_instances si
         LEFT JOIN sync_data sd ON sd.sync_instance_id = si.id
         WHERE si.user_id = ?1 AND si.instance_name = ?2",
    )?;
    match stmt.query_row(params![user_id, instance_name], |r| {
        Ok(SyncInstanceRow {
            id: r.get(0)?,
            user_id: r.get(1)?,
            instance_name: r.get(2)?,
            mc_version: r.get(3)?,
            loader: r.get(4)?,
            ram_mb: r.get::<_, u32>(5)?,
            save_count: r.get::<_, u32>(6)?,
            has_data: r.get::<_, i64>(7)? != 0,
            updated_at: r.get(8)?,
        })
    }) {
        Ok(row) => Ok(Some(row)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

/// Returns (total_instances, total_saves) for a user.
pub fn sync_count_user(conn: &Connection, user_id: i64) -> Result<(i64, i64)> {
    conn.query_row(
        "SELECT COUNT(*), COALESCE(SUM(save_count), 0) FROM sync_instances WHERE user_id = ?1",
        params![user_id],
        |r| Ok((r.get::<_, i64>(0)?, r.get::<_, i64>(1)?)),
    )
    .map_err(|e| e.into())
}

pub fn sync_upsert(
    conn: &Connection,
    user_id: i64,
    instance_name: &str,
    mc_version: &str,
    loader: &str,
    ram_mb: u32,
    save_count: u32,
) -> Result<i64> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO sync_instances (user_id, instance_name, mc_version, loader, ram_mb, save_count, updated_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
         ON CONFLICT(user_id, instance_name) DO UPDATE SET
             mc_version = excluded.mc_version,
             loader     = excluded.loader,
             ram_mb     = excluded.ram_mb,
             save_count = excluded.save_count,
             updated_at = excluded.updated_at",
        params![user_id, instance_name, mc_version, loader, ram_mb, save_count, now],
    )?;
    let id = conn.query_row(
        "SELECT id FROM sync_instances WHERE user_id = ?1 AND instance_name = ?2",
        params![user_id, instance_name],
        |r| r.get(0),
    )?;
    Ok(id)
}

pub fn sync_delete(conn: &Connection, id: i64) -> Result<()> {
    conn.execute("DELETE FROM sync_instances WHERE id = ?1", params![id])?;
    Ok(())
}

pub fn sync_push_data(conn: &Connection, sync_instance_id: i64, data: &[u8]) -> Result<()> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO sync_data (sync_instance_id, data_zip, updated_at) VALUES (?1, ?2, ?3)
         ON CONFLICT(sync_instance_id) DO UPDATE SET
             data_zip   = excluded.data_zip,
             updated_at = excluded.updated_at",
        params![sync_instance_id, data, now],
    )?;
    Ok(())
}

pub fn sync_pull_data(conn: &Connection, sync_instance_id: i64) -> Result<Option<Vec<u8>>> {
    match conn.query_row(
        "SELECT data_zip FROM sync_data WHERE sync_instance_id = ?1",
        params![sync_instance_id],
        |r| r.get::<_, Vec<u8>>(0),
    ) {
        Ok(data) => Ok(Some(data)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}
