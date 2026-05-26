package com.p2pminecraft.runtime;

/**
 * Pont Java → Rust via JNI.
 * Charge rust_core.dll (Windows) / librust_core.so (Linux).
 *
 * Si la lib native n'est pas disponible, toutes les méthodes
 * tombent sur l'implémentation Java de fallback dans DistributedChunkManager.
 */
public class RustBridge {

    static boolean NATIVE_LOADED = false;

    static {
        try {
            System.loadLibrary("rust_core");
            NATIVE_LOADED = true;
            System.out.println("[P2P] Rust native library loaded");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("[P2P] Rust library not found, using Java fallback");
        }
    }

    public static native void   init(String peerId, int x, int z);
    public static native boolean shouldTickChunk(int cx, int cz);
    public static native void   setMyPosition(int cx, int cz);
    public static native void   upsertPeer(String peerId, int x, int z);
    public static native void   removePeer(String peerId);
    public static native int    myChunkCount();
}
