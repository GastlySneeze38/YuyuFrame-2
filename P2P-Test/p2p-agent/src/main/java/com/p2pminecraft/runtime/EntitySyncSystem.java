package com.p2pminecraft.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Appelé par EntityTransformer pour les entités "ghost" (Phase 3).
 * Stub pour Phase 1 — structure en place, logique à compléter.
 */
public class EntitySyncSystem {

    private static final Set<Integer> ghostIds = ConcurrentHashMap.newKeySet();

    public static boolean isGhost(int entityId) {
        return ghostIds.contains(entityId);
    }

    public static void tickGhost(Object entity) {
        // Phase 3 : interpolation de position (lerpTo)
        // Pour l'instant, no-op
    }

    public static void markGhost(int entityId) {
        ghostIds.add(entityId);
    }

    public static void unmarkGhost(int entityId) {
        ghostIds.remove(entityId);
    }
}
