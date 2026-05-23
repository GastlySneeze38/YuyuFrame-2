use anyhow::Result;
use rusqlite::{params, Connection};
use std::path::Path;

pub fn init_db(path: &Path) -> Result<Connection> {
    let conn = Connection::open(path)?;
    tracing::info!("Base de données : {}", path.display());

    conn.execute_batch(
        "PRAGMA foreign_keys = ON;

         CREATE TABLE IF NOT EXISTS mc_sessions (
             id               INTEGER PRIMARY KEY AUTOINCREMENT,
             yuyu_user_id     INTEGER NOT NULL,
             mc_username      TEXT    NOT NULL,
             mc_uuid          TEXT    NOT NULL,
             access_token     TEXT    NOT NULL,
             ms_refresh_token TEXT    NOT NULL,
             expires_at       INTEGER NOT NULL,
             updated_at       INTEGER NOT NULL,
             UNIQUE(yuyu_user_id, mc_uuid)
         );

         CREATE TABLE IF NOT EXISTS active_mc (
             yuyu_user_id INTEGER PRIMARY KEY,
             mc_uuid      TEXT    NOT NULL
         );

         CREATE TABLE IF NOT EXISTS yuyu_session (
             id        INTEGER PRIMARY KEY CHECK (id = 1),
             jwt       TEXT    NOT NULL,
             user_id   INTEGER NOT NULL,
             username  TEXT    NOT NULL,
             saved_at  INTEGER NOT NULL
         );

         CREATE TABLE IF NOT EXISTS instances (
             id           TEXT    PRIMARY KEY,
             yuyu_user_id INTEGER NOT NULL DEFAULT 0,
             name         TEXT    NOT NULL,
             mc_version   TEXT    NOT NULL,
             loader       TEXT    NOT NULL,
             ram_mb       INTEGER NOT NULL DEFAULT 4096,
             favorite     INTEGER NOT NULL DEFAULT 0,
             created_at   INTEGER NOT NULL
         );",
    )?;

    // Migrations pour les DBs existantes
    let _ = conn.execute("ALTER TABLE instances ADD COLUMN yuyu_user_id INTEGER NOT NULL DEFAULT 0", []);
    let _ = conn.execute("ALTER TABLE instances ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0", []);

    Ok(conn)
}

// ── Types ─────────────────────────────────────────────────────────────────────

#[derive(Clone)]
pub struct McSessionRow {
    pub mc_username: String,
    pub mc_uuid: String,
    pub access_token: String,
    pub ms_refresh_token: String,
    pub expires_at: i64,
}

// ── JWT session YuyuFrame (stocké localement pour restauration au démarrage) ──

pub struct YuyuSessionRow {
    pub jwt: String,
    pub user_id: i64,
    pub username: String,
}

pub fn save_yuyu_jwt(conn: &Connection, user_id: i64, username: &str, jwt: &str) -> Result<()> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO yuyu_session (id, jwt, user_id, username, saved_at) VALUES (1, ?1, ?2, ?3, ?4)
         ON CONFLICT(id) DO UPDATE SET jwt=excluded.jwt, user_id=excluded.user_id,
             username=excluded.username, saved_at=excluded.saved_at",
        params![jwt, user_id, username, now],
    )?;
    Ok(())
}

pub fn load_yuyu_jwt(conn: &Connection) -> Result<Option<YuyuSessionRow>> {
    match conn.query_row(
        "SELECT jwt, user_id, username FROM yuyu_session WHERE id = 1",
        [],
        |r| Ok(YuyuSessionRow { jwt: r.get(0)?, user_id: r.get(1)?, username: r.get(2)? }),
    ) {
        Ok(row) => Ok(Some(row)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn delete_yuyu_jwt(conn: &Connection) -> Result<()> {
    conn.execute("DELETE FROM yuyu_session WHERE id = 1", [])?;
    Ok(())
}

// ── Minecraft sessions ─────────────────────────────────────────────────────────

pub fn upsert_mc_session(
    conn: &Connection,
    yuyu_user_id: i64,
    mc_username: &str,
    mc_uuid: &str,
    access_token: &str,
    ms_refresh_token: &str,
    expires_at: i64,
) -> Result<()> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO mc_sessions
             (yuyu_user_id, mc_username, mc_uuid, access_token, ms_refresh_token, expires_at, updated_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
         ON CONFLICT(yuyu_user_id, mc_uuid) DO UPDATE SET
             mc_username      = excluded.mc_username,
             access_token     = excluded.access_token,
             ms_refresh_token = excluded.ms_refresh_token,
             expires_at       = excluded.expires_at,
             updated_at       = excluded.updated_at",
        params![yuyu_user_id, mc_username, mc_uuid, access_token, ms_refresh_token, expires_at, now],
    )?;
    Ok(())
}

pub fn list_mc_sessions(conn: &Connection, yuyu_user_id: i64) -> Result<Vec<McSessionRow>> {
    let mut stmt = conn.prepare(
        "SELECT mc_username, mc_uuid, access_token, ms_refresh_token, expires_at
         FROM mc_sessions WHERE yuyu_user_id = ?1",
    )?;
    let rows = stmt
        .query_map(params![yuyu_user_id], |r| {
            Ok(McSessionRow {
                mc_username: r.get(0)?,
                mc_uuid: r.get(1)?,
                access_token: r.get(2)?,
                ms_refresh_token: r.get(3)?,
                expires_at: r.get(4)?,
            })
        })?
        .collect::<rusqlite::Result<Vec<_>>>()?;
    Ok(rows)
}

pub fn get_mc_session(
    conn: &Connection,
    yuyu_user_id: i64,
    mc_uuid: &str,
) -> Result<Option<McSessionRow>> {
    let mut stmt = conn.prepare(
        "SELECT mc_username, mc_uuid, access_token, ms_refresh_token, expires_at
         FROM mc_sessions WHERE yuyu_user_id = ?1 AND mc_uuid = ?2",
    )?;
    match stmt.query_row(params![yuyu_user_id, mc_uuid], |r| {
        Ok(McSessionRow {
            mc_username: r.get(0)?,
            mc_uuid: r.get(1)?,
            access_token: r.get(2)?,
            ms_refresh_token: r.get(3)?,
            expires_at: r.get(4)?,
        })
    }) {
        Ok(r) => Ok(Some(r)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn delete_mc_session(conn: &Connection, yuyu_user_id: i64, mc_uuid: &str) -> Result<()> {
    conn.execute(
        "DELETE FROM mc_sessions WHERE yuyu_user_id = ?1 AND mc_uuid = ?2",
        params![yuyu_user_id, mc_uuid],
    )?;
    Ok(())
}

pub fn update_mc_tokens(
    conn: &Connection,
    yuyu_user_id: i64,
    mc_uuid: &str,
    access_token: &str,
    ms_refresh_token: &str,
    expires_at: i64,
) -> Result<()> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "UPDATE mc_sessions
         SET access_token=?1, ms_refresh_token=?2, expires_at=?3, updated_at=?4
         WHERE yuyu_user_id=?5 AND mc_uuid=?6",
        params![access_token, ms_refresh_token, expires_at, now, yuyu_user_id, mc_uuid],
    )?;
    Ok(())
}

// ── Active MC session ──────────────────────────────────────────────────────────

pub fn set_active_mc(conn: &Connection, yuyu_user_id: i64, mc_uuid: &str) -> Result<()> {
    conn.execute(
        "INSERT INTO active_mc (yuyu_user_id, mc_uuid) VALUES (?1, ?2)
         ON CONFLICT(yuyu_user_id) DO UPDATE SET mc_uuid = excluded.mc_uuid",
        params![yuyu_user_id, mc_uuid],
    )?;
    Ok(())
}

pub fn get_active_mc_uuid(conn: &Connection, yuyu_user_id: i64) -> Result<Option<String>> {
    match conn.query_row(
        "SELECT mc_uuid FROM active_mc WHERE yuyu_user_id = ?1",
        params![yuyu_user_id],
        |r| r.get(0),
    ) {
        Ok(uuid) => Ok(Some(uuid)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn clear_active_mc(conn: &Connection, yuyu_user_id: i64, mc_uuid: &str) -> Result<()> {
    conn.execute(
        "DELETE FROM active_mc WHERE yuyu_user_id = ?1 AND mc_uuid = ?2",
        params![yuyu_user_id, mc_uuid],
    )?;
    Ok(())
}

// ── Instances ──────────────────────────────────────────────────────────────────

pub struct InstanceRow {
    pub id: String,
    pub name: String,
    pub mc_version: String,
    pub loader: String,
    pub ram_mb: u32,
    pub favorite: bool,
}

pub fn instance_list(conn: &Connection, user_id: i64) -> Result<Vec<InstanceRow>> {
    let mut stmt = conn.prepare(
        "SELECT id, name, mc_version, loader, ram_mb, favorite FROM instances
         WHERE yuyu_user_id = ?1 ORDER BY created_at ASC",
    )?;
    let rows = stmt
        .query_map(params![user_id], |r| {
            Ok(InstanceRow {
                id: r.get(0)?,
                name: r.get(1)?,
                mc_version: r.get(2)?,
                loader: r.get(3)?,
                ram_mb: r.get::<_, u32>(4)?,
                favorite: r.get::<_, i64>(5)? != 0,
            })
        })?
        .collect::<rusqlite::Result<Vec<_>>>()?;
    Ok(rows)
}

pub fn instance_get(conn: &Connection, id: &str, user_id: i64) -> Result<Option<InstanceRow>> {
    let mut stmt = conn.prepare(
        "SELECT id, name, mc_version, loader, ram_mb, favorite FROM instances
         WHERE id = ?1 AND yuyu_user_id = ?2",
    )?;
    match stmt.query_row(params![id, user_id], |r| {
        Ok(InstanceRow {
            id: r.get(0)?,
            name: r.get(1)?,
            mc_version: r.get(2)?,
            loader: r.get(3)?,
            ram_mb: r.get::<_, u32>(4)?,
            favorite: r.get::<_, i64>(5)? != 0,
        })
    }) {
        Ok(row) => Ok(Some(row)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn instance_set_favorite(conn: &Connection, id: &str, user_id: i64, favorite: bool) -> Result<()> {
    conn.execute(
        "UPDATE instances SET favorite = ?1 WHERE id = ?2 AND yuyu_user_id = ?3",
        params![favorite as i64, id, user_id],
    )?;
    Ok(())
}

pub fn instance_insert(
    conn: &Connection,
    id: &str,
    user_id: i64,
    name: &str,
    mc_version: &str,
    loader: &str,
    ram_mb: u32,
) -> Result<()> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO instances (id, yuyu_user_id, name, mc_version, loader, ram_mb, created_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
        params![id, user_id, name, mc_version, loader, ram_mb, now],
    )?;
    Ok(())
}

pub fn instance_update(
    conn: &Connection,
    id: &str,
    user_id: i64,
    name: &str,
    mc_version: &str,
    loader: &str,
    ram_mb: u32,
) -> Result<()> {
    let n = conn.execute(
        "UPDATE instances SET name=?1, mc_version=?2, loader=?3, ram_mb=?4
         WHERE id=?5 AND yuyu_user_id=?6",
        params![name, mc_version, loader, ram_mb, id, user_id],
    )?;
    if n == 0 {
        return Err(anyhow::anyhow!("Instance introuvable"));
    }
    Ok(())
}

pub fn instance_delete(conn: &Connection, id: &str, user_id: i64) -> Result<()> {
    conn.execute(
        "DELETE FROM instances WHERE id = ?1 AND yuyu_user_id = ?2",
        params![id, user_id],
    )?;
    Ok(())
}

/// Reassigne les instances orphelines (yuyu_user_id = 0) à l'utilisateur qui vient de se connecter.
pub fn instance_claim_unclaimed(conn: &Connection, user_id: i64) -> Result<()> {
    conn.execute(
        "UPDATE instances SET yuyu_user_id = ?1 WHERE yuyu_user_id = 0",
        params![user_id],
    )?;
    Ok(())
}
