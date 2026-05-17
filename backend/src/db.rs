use anyhow::Result;
use rusqlite::{params, Connection};

pub fn init_db() -> Result<Connection> {
    let conn = Connection::open("yuyu.db")?;
    tracing::info!("Base de données : yuyu.db ({})", std::env::current_dir().unwrap_or_default().display());

    conn.execute_batch(
        "PRAGMA journal_mode = WAL;
         PRAGMA foreign_keys = ON;

         CREATE TABLE IF NOT EXISTS yuyu_users (
             id            INTEGER PRIMARY KEY AUTOINCREMENT,
             username      TEXT    UNIQUE NOT NULL,
             password_hash TEXT    NOT NULL,
             created_at    INTEGER NOT NULL
         );

         CREATE TABLE IF NOT EXISTS mc_sessions (
             id               INTEGER PRIMARY KEY AUTOINCREMENT,
             yuyu_user_id     INTEGER NOT NULL REFERENCES yuyu_users(id) ON DELETE CASCADE,
             mc_username      TEXT    NOT NULL,
             mc_uuid          TEXT    NOT NULL,
             access_token     TEXT    NOT NULL,
             ms_refresh_token TEXT    NOT NULL,
             expires_at       INTEGER NOT NULL,
             updated_at       INTEGER NOT NULL,
             UNIQUE(yuyu_user_id, mc_uuid)
         );

         CREATE TABLE IF NOT EXISTS active_mc (
             yuyu_user_id INTEGER PRIMARY KEY REFERENCES yuyu_users(id) ON DELETE CASCADE,
             mc_uuid      TEXT    NOT NULL
         );

         CREATE TABLE IF NOT EXISTS yuyu_tokens (
             user_id    INTEGER PRIMARY KEY REFERENCES yuyu_users(id) ON DELETE CASCADE,
             token      TEXT    NOT NULL,
             created_at INTEGER NOT NULL
         );",
    )?;

    Ok(conn)
}

// ── Types ─────────────────────────────────────────────────────────────────────

pub struct YuyuUser {
    pub id: i64,
    pub username: String,
    pub password_hash: String,
}

#[derive(Clone)]
pub struct McSessionRow {
    pub mc_username: String,
    pub mc_uuid: String,
    pub access_token: String,
    pub ms_refresh_token: String,
    pub expires_at: i64,
}

// ── YuyuFrame users ────────────────────────────────────────────────────────────

pub fn account_exists(conn: &Connection) -> Result<bool> {
    let n: i64 = conn.query_row("SELECT COUNT(*) FROM yuyu_users", [], |r| r.get(0))?;
    Ok(n > 0)
}

pub fn create_user(conn: &Connection, username: &str, password_hash: &str) -> Result<i64> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO yuyu_users (username, password_hash, created_at) VALUES (?1, ?2, ?3)",
        params![username, password_hash, now],
    )?;
    Ok(conn.last_insert_rowid())
}

pub fn get_user(conn: &Connection, username: &str) -> Result<Option<YuyuUser>> {
    let mut stmt = conn
        .prepare("SELECT id, username, password_hash FROM yuyu_users WHERE username = ?1")?;
    match stmt.query_row(params![username], |r| {
        Ok(YuyuUser {
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

// ── YuyuFrame session token ────────────────────────────────────────────────────

pub struct YuyuTokenRow {
    pub user_id: i64,
    pub username: String,
    pub token: String,
}

pub fn save_yuyu_token(conn: &Connection, user_id: i64, token: &str) -> Result<()> {
    let now = chrono::Utc::now().timestamp();
    conn.execute(
        "INSERT INTO yuyu_tokens (user_id, token, created_at) VALUES (?1, ?2, ?3)
         ON CONFLICT(user_id) DO UPDATE SET token = excluded.token, created_at = excluded.created_at",
        params![user_id, token, now],
    )?;
    Ok(())
}

pub fn load_yuyu_token(conn: &Connection) -> Result<Option<YuyuTokenRow>> {
    let mut stmt = conn.prepare(
        "SELECT t.user_id, u.username, t.token
         FROM yuyu_tokens t JOIN yuyu_users u ON u.id = t.user_id
         LIMIT 1",
    )?;
    match stmt.query_row([], |r| {
        Ok(YuyuTokenRow {
            user_id: r.get(0)?,
            username: r.get(1)?,
            token: r.get(2)?,
        })
    }) {
        Ok(row) => Ok(Some(row)),
        Err(rusqlite::Error::QueryReturnedNoRows) => Ok(None),
        Err(e) => Err(e.into()),
    }
}

pub fn delete_yuyu_token(conn: &Connection, user_id: i64) -> Result<()> {
    conn.execute("DELETE FROM yuyu_tokens WHERE user_id = ?1", params![user_id])?;
    Ok(())
}
