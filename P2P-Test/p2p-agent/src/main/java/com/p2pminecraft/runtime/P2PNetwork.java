package com.p2pminecraft.runtime;

import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Réseau P2P : connexion au serveur de signaling, échange de positions.
 * Utilise java.net.http.WebSocket (Java 11+, disponible dans le JVM Minecraft).
 *
 * Format des messages JSON (même protocole que rust-core/signaling) :
 *   join     : { type, id, name }
 *   position : { type, id, x, z }
 *   peer_list, peer_joined, peer_left, position (entrant)
 */
public class P2PNetwork {

    private static final String DEFAULT_SIGNALING = "ws://127.0.0.1:8765";

    private volatile WebSocket ws;
    private final String peerId;
    private final String peerName;
    private final String signalingUrl;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "p2p-network");
        t.setDaemon(true);
        return t;
    });

    public P2PNetwork(String peerId, String peerName, String signalingUrl) {
        this.peerId = peerId;
        this.peerName = peerName;
        this.signalingUrl = signalingUrl != null ? signalingUrl : DEFAULT_SIGNALING;
    }

    public void start() {
        scheduler.execute(this::connect);
    }

    private void connect() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            ws = client.newWebSocketBuilder()
                .buildAsync(URI.create(signalingUrl), new Listener())
                .get(10, TimeUnit.SECONDS);

            send("{\"type\":\"join\",\"id\":\"" + peerId + "\",\"name\":\"" + peerName + "\"}");
            send("{\"type\":\"position\",\"id\":\"" + peerId + "\",\"x\":0,\"z\":0}");
            connected.set(true);

            System.out.println("[P2P] Connecté au signaling : " + signalingUrl);
        } catch (Exception e) {
            System.err.println("[P2P] Connexion au signaling impossible : " + e.getMessage());
            // Retry dans 5 secondes
            scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
        }
    }

    public void updateMyPosition(int cx, int cz) {
        DistributedChunkManager.setMyPosition(cx, cz);
        if (connected.get()) {
            send("{\"type\":\"position\",\"id\":\"" + peerId + "\",\"x\":" + cx + ",\"z\":" + cz + "}");
        }
    }

    public void stop() {
        connected.set(false);
        if (ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
        scheduler.shutdownNow();
    }

    private void send(String msg) {
        if (ws != null) ws.sendText(msg, true);
    }

    // ---- Listener WebSocket ----

    private class Listener implements WebSocket.Listener {

        private final StringBuilder buf = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                handleMessage(buf.toString());
                buf.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            connected.set(false);
            System.out.println("[P2P] Déconnecté, reconnexion dans 5s...");
            scheduler.schedule(P2PNetwork.this::connect, 5, TimeUnit.SECONDS);
            return null;
        }
    }

    // ---- Parser JSON minimal (sans dépendance Gson au runtime) ----

    private void handleMessage(String json) {
        String type = extract(json, "type");
        if (type == null) return;

        switch (type) {
            case "peer_list" -> handlePeerList(json);
            case "peer_joined" -> {
                String id = extract(json, "id");
                int x = extractInt(json, "x");
                int z = extractInt(json, "z");
                if (id != null) {
                    DistributedChunkManager.upsertPeer(id, x, z);
                    System.out.println("[P2P] → peer rejoint: " + id.substring(0, 8));
                }
            }
            case "peer_left" -> {
                String id = extract(json, "id");
                if (id != null) {
                    DistributedChunkManager.removePeer(id);
                    System.out.println("[P2P] ← peer parti: " + id.substring(0, 8));
                }
            }
            case "position" -> {
                String id = extract(json, "id");
                int x = extractInt(json, "x");
                int z = extractInt(json, "z");
                if (id != null && !id.equals(peerId)) {
                    DistributedChunkManager.upsertPeer(id, x, z);
                }
            }
        }
    }

    private void handlePeerList(String json) {
        // peers:[{id,name,x,z}, ...]
        int start = json.indexOf("\"peers\"");
        if (start < 0) return;
        String arr = json.substring(start);
        int idx = 0;
        while ((idx = arr.indexOf("\"id\"", idx)) >= 0) {
            String id = extractFrom(arr, "id", idx);
            int x = extractIntFrom(arr, "x", idx);
            int z = extractIntFrom(arr, "z", idx);
            if (id != null) DistributedChunkManager.upsertPeer(id, x, z);
            idx++;
        }
        System.out.println("[P2P] " + DistributedChunkManager.stats());
    }

    // Mini-parseur JSON par regexp (évite dépendance Gson au runtime)
    private static String extract(String json, String key) {
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        if (i < 0) return null;
        i = json.indexOf(':', i) + 1;
        while (i < json.length() && json.charAt(i) == ' ') i++;
        if (json.charAt(i) == '"') {
            int end = json.indexOf('"', i + 1);
            return json.substring(i + 1, end);
        }
        return null;
    }

    private static String extractFrom(String json, String key, int from) {
        return extract(json.substring(from), key);
    }

    private static int extractInt(String json, String key) {
        String search = "\"" + key + "\"";
        int i = json.indexOf(search);
        if (i < 0) return 0;
        i = json.indexOf(':', i) + 1;
        while (i < json.length() && json.charAt(i) == ' ') i++;
        int end = i;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(i, end)); } catch (NumberFormatException e) { return 0; }
    }

    private static int extractIntFrom(String json, String key, int from) {
        return extractInt(json.substring(from), key);
    }
}
