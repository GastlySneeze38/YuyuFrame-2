package net.minecraft.server.level;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

/** Stub compile-only — la vraie classe vient du JAR Minecraft mappé. */
public abstract class ServerLevel extends Level {
    protected ServerLevel() {}
    public void tickChunk(LevelChunk chunk, int randomTickSpeed) {}
}
