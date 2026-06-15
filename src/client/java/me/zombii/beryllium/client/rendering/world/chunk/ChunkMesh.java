package me.zombii.beryllium.client.rendering.world.chunk;

import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.BerylliumMesh;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkMesh extends BerylliumMesh {

    Chunk chunk;
    AtomicBoolean isFinished;

    public ChunkMesh(Chunk chunk, AtomicBoolean isFinished) {
        super(128, true);
        this.chunk = chunk;
        this.isFinished = isFinished;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public boolean isFinished() {
        return isFinished.get();
    }

    public AtomicBoolean getIsFinished() {
        return isFinished;
    }
}
