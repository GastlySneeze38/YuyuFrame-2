package test;

/** Simule net.minecraft.world.level.chunk.LevelChunk */
public class FakeChunk {
    public final int x, z;
    public int tickCount = 0;

    public FakeChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    /** Ce que l'agent Java intercepterait normalement */
    public void tick() {
        tickCount++;
    }

    @Override
    public String toString() {
        return String.format("Chunk(%d,%d)", x, z);
    }
}
