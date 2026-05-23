use anyhow::Result;
use rusqlite::{params, Connection};

pub fn init(conn: &Connection) -> Result<()> {
    conn.execute_batch(
        "PRAGMA foreign_keys = ON;
         CREATE TABLE IF NOT EXISTS users (
             id            INTEGER PRIMARY KEY AUTOINCREMENT,
             username      TEXT    UNIQUE NOT NULL,
             password_hash TEXT    NOT NULL,
             created_at    INTEGER NOT NULL
         );
         CREATE TABLE IF NOT EXISTS sync_instances (
             id            INTEGER PRIMARY KEY AUTOINCREMENT,
             user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
             instance_name TEXT    NOT NULL,
             mc_version    TEXT    NOT NULL,
             loader        TEXT    NOT NULL,
             ram_mb        INTEGER NOT NULL DEFAULT 4096,
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
    let mut stmt =
        conn.prepare("SELECT id, username, password_hash FROM users WHERE username = ?1")?;
    match stmt.query_row(params![username], |r| {
        Ok(User {
            id: r.get(0)?,
            username: r.get(1)?,
            password_hash: r.get(2)?,
        })
    }) {
        Ok(u) => Ok(Some(u)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

// ── Sync instances ─────────────────────────────────────────────────────────────

pub struct SyncInstanceRow {
    pub id: i64,
    pub user_id: i64,
    pub instance_name: String,
    pub mc_version: String,
    pub loader: String,
    pub ram_mb: u32,
    pub has_data: bool,
    pub updated_at: i64,
}

pub fn sync_list(conn: &Connection, user_id: i64) -> Result<Vec<SyncInstanceRow>> {
    let mut stmt = conn.prepare(
        "SELECT si.id, si.user_id, si.instance_name, si.mc_version, si.loader, si.ram_mb,
                (sd.sync_instance_id IS NOT NULL) as has_data, si.updated_at
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
                has_data: r.get::<_, i64>(6)? != 0,
                updated_at: r.get(7)?,
            })
        })?
        .collect::<rusqlite::Result<Vec<_>>>()?;
    Ok(rows)
}

pub fn sync_get(conn: &Connection, id: i64) -> Result<Option<SyncInstanceRow>> {
    let mut stmt = conn.prepare(
        "SELECT si.id, si.user_id, si.instance_name, si.mc_version, si.loader, si.ram_mb,
                (sd.sync_instance_id IS NOT NULL) as has_data, si.updated_at
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
            has_data: r.get::<_, i64>(6)? != 0,
            updated_at: r.get(7)?,
        })
    }) {
        Ok(row) => Ok(Some(row)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn sync_upsert(
    conn: &Connection,
    user_id: i64,
    instance_name: &str,
    mc_version: &str,
    loader: &str,
    ram_mb: u32,
) -> Result<i64> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO sync_instances (user_id, instance_name, mc_version, loader, ram_mb, updated_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6)
         ON CONFLICT(user_id, instance_name) DO UPDATE SET
             mc_version = excluded.mc_version,
             loader     = excluded.loader,
             ram_mb     = excluded.ram_mb,
             updated_at = excluded.updated_at",
        params![user_id, instance_name, mc_version, loader, ram_mb, now],
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
