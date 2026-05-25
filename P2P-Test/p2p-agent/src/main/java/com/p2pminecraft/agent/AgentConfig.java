package com.p2pminecraft.agent;

import java.util.UUID;

/**
 * Configuration passée via : -javaagent:p2p-agent.jar=peerId=xxx,name=Alice,server=ws://...
 */
public class AgentConfig {

    public String peerId;
    public String peerName;
    public String signalingUrl;

    private static AgentConfig current;

    public static AgentConfig parse(String args) {
        AgentConfig cfg = new AgentConfig();
        cfg.peerId      = UUID.randomUUID().toString();
        cfg.peerName    = "Player_" + cfg.peerId.substring(0, 4);
        cfg.signalingUrl = "ws://127.0.0.1:8765";

        if (args != null && !args.isEmpty()) {
            for (String pair : args.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length != 2) continue;
                switch (kv[0].trim()) {
                    case "peerId"   -> cfg.peerId      = kv[1].trim();
                    case "name"     -> cfg.peerName    = kv[1].trim();
                    case "server"   -> cfg.signalingUrl = kv[1].trim();
                }
            }
        }

        current = cfg;
        return cfg;
    }

    public static AgentConfig getCurrent() {
        return current != null ? current : parse(null);
    }
}
