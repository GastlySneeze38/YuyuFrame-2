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
