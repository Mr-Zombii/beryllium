package me.zombii.beryllium.client.rendering.world.chunk;

import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.BerylliumMesh;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkMesh extends BerylliumMesh {

    public int scale = 1;
    Chunk chunk;
    AtomicBoolean isFinished;
    AtomicBoolean scheduledForDisposal;

    public ChunkMesh(Chunk chunk, AtomicBoolean isFinished) {
        super(128, true);
        this.chunk = chunk;
        this.isFinished = isFinished;
        this.scheduledForDisposal = new AtomicBoolean(false);
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

    public void scheduleForDisposal() {
        scheduledForDisposal.set(true);
    }

    public boolean isScheduledForDisposal() {
        return scheduledForDisposal.get();
    }
}
