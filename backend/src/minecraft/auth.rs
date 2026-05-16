use anyhow::{anyhow, Result};
use serde::Deserialize;
use std::path::PathBuf;

use crate::state::MinecraftSession;

const MS_CLIENT_ID: &str = "00000000402b5328";
const DEVICE_CODE_URL: &str =
    "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
const TOKEN_URL: &str = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
const XBL_URL: &str = "https://user.auth.xboxlive.com/user/authenticate";
const XSTS_URL: &str = "https://xsts.auth.xboxlive.com/xsts/authorize";
const MC_AUTH_URL: &str = "https://api.minecraftservices.com/authentication/login_with_xbox";
const MC_PROFILE_URL: &str = "https://api.minecraftservices.com/minecraft/profile";

pub struct DeviceCodeResponse {
    pub device_code: String,
    pub user_code: String,
    pub verification_uri: String,
    pub expires_in: i64,
}

#[derive(Deserialize)]
struct MsDeviceCodeResp {
    device_code: String,
    user_code: String,
    verification_uri: String,
    expires_in: i64,
}

#[derive(Deserialize)]
struct MsTokenResp {
    access_token: Option<String>,
    refresh_token: Option<String>,
    error: Option<String>,
}

#[derive(Deserialize)]
struct XblResp {
    #[serde(rename = "Token")]
    token: String,
    #[serde(rename = "DisplayClaims")]
    display_claims: XblDisplayClaims,
}

#[derive(Deserialize)]
struct XblDisplayClaims {
    xui: Vec<XblXui>,
}

#[derive(Deserialize)]
struct XblXui {
    uhs: String,
}

#[derive(Deserialize)]
struct XstsResp {
    #[serde(rename = "Token")]
    token: String,
}

#[derive(Deserialize)]
struct McAuthResp {
    access_token: String,
}

#[derive(Deserialize)]
struct McProfileResp {
    id: String,
    name: String,
}

pub async fn start_device_auth() -> Result<DeviceCodeResponse> {
    let client = reqwest::Client::new();
    let params = [
        ("client_id", MS_CLIENT_ID),
        ("scope", "XboxLive.signin offline_access"),
    ];
    let resp: MsDeviceCodeResp = client
        .post(DEVICE_CODE_URL)
        .form(&params)
        .send()
        .await?
        .json()
        .await?;
    Ok(DeviceCodeResponse {
        device_code: resp.device_code,
        user_code: resp.user_code,
        verification_uri: resp.verification_uri,
        expires_in: resp.expires_in,
    })
}

pub async fn poll_device_auth(device_code: &str) -> Result<Option<MinecraftSession>> {
    let client = reqwest::Client::new();
    let params = [
        ("client_id", MS_CLIENT_ID),
        ("device_code", device_code),
        ("grant_type", "urn:ietf:params:oauth:grant-type:device_code"),
    ];
    let resp: MsTokenResp = client
        .post(TOKEN_URL)
        .form(&params)
        .send()
        .await?
        .json()
        .await?;

    if let Some(err) = resp.error {
        if err == "authorization_pending" {
            return Ok(None);
        }
        return Err(anyhow!("Auth error: {}", err));
    }

    let ms_token = resp.access_token.ok_or_else(|| anyhow!("No access token"))?;
    let refresh_token = resp.refresh_token;

    // Xbox Live
    let xbl_body = serde_json::json!({
        "Properties": {
            "AuthMethod": "RPS",
            "SiteName": "user.auth.xboxlive.com",
            "RpsTicket": format!("d={}", ms_token)
        },
        "RelyingParty": "http://auth.xboxlive.com",
        "TokenType": "JWT"
    });
    let xbl_resp: XblResp = client
        .post(XBL_URL)
        .json(&xbl_body)
        .header("Accept", "application/json")
        .send()
        .await?
        .json()
        .await?;

    let userhash = xbl_resp
        .display_claims
        .xui
        .first()
        .ok_or_else(|| anyhow!("No userhash"))?
        .uhs
        .clone();

    // XSTS
    let xsts_body = serde_json::json!({
        "Properties": {
            "SandboxId": "RETAIL",
            "UserTokens": [xbl_resp.token]
        },
        "RelyingParty": "rp://api.minecraftservices.com/",
        "TokenType": "JWT"
    });
    let xsts_resp: XstsResp = client
        .post(XSTS_URL)
        .json(&xsts_body)
        .header("Accept", "application/json")
        .send()
        .await?
        .json()
        .await?;

    // Minecraft auth
    let mc_body = serde_json::json!({
        "identityToken": format!("XBL3.0 x={};{}", userhash, xsts_resp.token)
    });
    let mc_resp: McAuthResp = client
        .post(MC_AUTH_URL)
        .json(&mc_body)
        .send()
        .await?
        .json()
        .await?;

    // Profile
    let profile: McProfileResp = client
        .get(MC_PROFILE_URL)
        .bearer_auth(&mc_resp.access_token)
        .send()
        .await?
        .json()
        .await?;

    Ok(Some(MinecraftSession {
        username: profile.name,
        uuid: profile.id,
        access_token: mc_resp.access_token,
        refresh_token,
        expires_at: chrono::Utc::now().timestamp() + 86400,
    }))
}

fn session_path() -> PathBuf {
    dirs::config_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join("YuyuFrame")
        .join("session.json")
}

pub async fn save_session(session: &MinecraftSession) -> Result<()> {
    let path = session_path();
    tokio::fs::create_dir_all(path.parent().unwrap()).await?;
    tokio::fs::write(path, serde_json::to_string_pretty(session)?).await?;
    Ok(())
}

pub async fn load_session() -> Result<MinecraftSession> {
    let json = tokio::fs::read_to_string(session_path()).await?;
    let session: MinecraftSession = serde_json::from_str(&json)?;
    if session.expires_at < chrono::Utc::now().timestamp() {
        return Err(anyhow!("Session expired"));
    }
    Ok(session)
}

pub async fn delete_session() -> Result<()> {
    let path = session_path();
    if path.exists() {
        tokio::fs::remove_file(path).await?;
    }
    Ok(())
}
