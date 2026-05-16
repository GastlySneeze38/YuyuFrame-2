use anyhow::Result;
use serde::Deserialize;
use std::collections::HashMap;

const VERSION_MANIFEST: &str =
    "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";

#[derive(Debug, Deserialize)]
struct VersionManifest {
    versions: Vec<VersionInfo>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct VersionInfo {
    pub id: String,
    #[serde(rename = "type")]
    pub version_type: String,
    pub url: String,
}

pub async fn fetch_version_list() -> Result<Vec<VersionInfo>> {
    let client = reqwest::Client::new();
    let manifest: VersionManifest = client
        .get(VERSION_MANIFEST)
        .send()
        .await?
        .json()
        .await?;
    Ok(manifest
        .versions
        .into_iter()
        .filter(|v| v.version_type == "release" || v.version_type == "snapshot")
        .collect())
}

#[derive(Debug, Deserialize)]
pub struct VersionDetails {
    pub id: String,
    #[serde(rename = "mainClass")]
    pub main_class: String,
    #[serde(rename = "minecraftArguments")]
    pub minecraft_arguments: Option<String>,
    pub arguments: Option<Arguments>,
    pub downloads: Downloads,
    pub libraries: Vec<Library>,
    #[serde(rename = "assetIndex")]
    pub asset_index: AssetIndex,
}

#[derive(Debug, Deserialize)]
pub struct Arguments {
    pub game: Vec<serde_json::Value>,
    pub jvm: Vec<serde_json::Value>,
}

#[derive(Debug, Deserialize)]
pub struct Downloads {
    pub client: Artifact,
}

#[derive(Debug, Clone, Deserialize)]
pub struct Artifact {
    pub url: String,
    pub sha1: String,
    pub size: u64,
}

#[derive(Debug, Deserialize)]
pub struct Library {
    pub name: String,
    pub downloads: Option<LibraryDownloads>,
    pub rules: Option<Vec<serde_json::Value>>,
}

#[derive(Debug, Deserialize)]
pub struct LibraryDownloads {
    pub artifact: Option<Artifact>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct AssetIndex {
    pub id: String,
    pub url: String,
}

#[derive(Debug, Deserialize)]
pub struct AssetIndexFile {
    pub objects: HashMap<String, AssetObject>,
}

#[derive(Debug, Deserialize)]
pub struct AssetObject {
    pub hash: String,
    pub size: u64,
}

pub async fn fetch_version_details(url: &str) -> Result<VersionDetails> {
    let client = reqwest::Client::new();
    Ok(client.get(url).send().await?.json().await?)
}

pub async fn fetch_asset_index(url: &str) -> Result<AssetIndexFile> {
    let client = reqwest::Client::new();
    Ok(client.get(url).send().await?.json().await?)
}
