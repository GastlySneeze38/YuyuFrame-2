package com.p2pminecraft.runtime;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Phase 4 : représentation visuelle des joueurs distants.
 *
 * Chaque pair connecté est représenté par un Zombie avec CustomName "P2P_<id>".
 * Les commandes sont enfilées depuis le thread WebSocket et exécutées sur le
 * thread serveur (via flush() appelé dans afterChunkTick).
 */
public class GhostManager {

    private static final Map<String, String> peerGhosts = new ConcurrentHashMap<>();
    private static final Set<String> spawned = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<String> pendingCmds = new ConcurrentLinkedQueue<>();

    // ── API publique ──────────────────────────────────────────────────────────

    public static void updateRemotePlayer(String peerId,
                                          double x, double y, double z,
                                          float yaw, float pitch) {
        String name = peerGhosts.computeIfAbsent(peerId,
            id -> "P2P_" + id.substring(0, Math.min(8, id.length())));

        if (spawned.add(name)) {
            String nbt = String.format(
                "{CustomName:'{\"text\":\"%s\"}',NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b}",
                name);
            pendingCmds.offer(String.format(
                "summon minecraft:zombie %.3f %.3f %.3f %s", x, y, z, nbt));
            System.out.println("[P2P] Ghost spawné : " + name);
        } else {
            pendingCmds.offer(String.format(
                "tp @e[name=%s,type=minecraft:zombie,limit=1] %.3f %.3f %.3f %.1f %.1f",
                name, x, y, z, (double) yaw, (double) pitch));
        }
    }

    public static void removeGhost(String peerId) {
        String name = peerGhosts.remove(peerId);
        if (name != null && spawned.remove(name)) {
            pendingCmds.offer("kill @e[name=" + name + ",type=minecraft:zombie]");
            System.out.println("[P2P] Ghost supprimé : " + name);
        }
    }

    public static void flush() {
        String cmd;
        int limit = 16;
        while ((cmd = pendingCmds.poll()) != null && limit-- > 0) {
            executeServerCommand(cmd);
        }
    }

    // ── Exécution via le serveur intégré ──────────────────────────────────────

    private static boolean executeServerCommand(String cmd) {
        try {
            String mcClass = "net/minecraft/client/Minecraft";
            Class<?> mcCls = MappingsRegistry.loadClass(mcClass);

            String getInstanceName = MappingsRegistry.getObfMethodName(mcClass, "getInstance");
            Object mc = mcCls.getMethod(getInstanceName).invoke(null);

            String getSspName = MappingsRegistry.getObfMethodName(mcClass, "getSingleplayerServer");
            Object server = mcCls.getMethod(getSspName).invoke(mc);
            if (server == null) return false;

            String serverClass = "net/minecraft/server/MinecraftServer";
            String sourceName   = MappingsRegistry.getObfMethodName(serverClass, "createCommandSourceStack");
            String commandsName = MappingsRegistry.getObfMethodName(serverClass, "getCommands");

            Object source   = server.getClass().getMethod(sourceName).invoke(server);
            Object commands = server.getClass().getMethod(commandsName).invoke(server);

            // performPrefixedCommand(CommandSourceStack, String) → "a" en obfusqué
            // performCommand(ParseResults, String) mappe aussi vers "a" — on les distingue
            // car ParseResults vient de brigadier (non obfusqué, package "com.mojang.brigadier")
            String cmdClass = "net/minecraft/commands/Commands";
            String performName = MappingsRegistry.getObfMethodName(cmdClass, "performPrefixedCommand");
            for (Method m : commands.getClass().getMethods()) {
                if (!m.getName().equals(performName) || m.getParameterCount() != 2) continue;
                if (!m.getParameterTypes()[1].equals(String.class)) continue;
                if (m.getParameterTypes()[0].getName().contains("brigadier")) continue;
                m.invoke(commands, source, cmd);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("[P2P] Ghost cmd échoué (" + cmd + "): " + e.getMessage());
            return false;
        }
    }
}
