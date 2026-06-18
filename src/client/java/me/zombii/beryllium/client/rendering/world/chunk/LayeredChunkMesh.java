package me.zombii.beryllium.client.rendering.world.chunk;

import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;

import java.util.concurrent.atomic.AtomicBoolean;

public class LayeredChunkMesh {

    private final ChunkMesh[] layers = new ChunkMesh[RenderLayers.LAYER_REGISTRY.names().size()];

    public ChunkMesh getLayer(int idx) {
        return layers[idx];
    }

    public LayeredChunkMesh setLayer(int idx, ChunkMesh mesh) {
        layers[idx] = mesh;
        return this;
    }

    public ChunkMesh[] getLayers() {
        return layers;
    }

    public int scale = 1;
    Chunk chunk;
    AtomicBoolean isFinished;
    AtomicBoolean scheduledForDisposal;
    AtomicBoolean canRenderOldMesh;

    public LayeredChunkMesh(Chunk chunk, AtomicBoolean isFinished) {
        this.chunk = chunk;
        this.isFinished = isFinished;
        this.scheduledForDisposal = new AtomicBoolean(false);
        for (int i = 0; i < this.layers.length; i++) {
            this.layers[i] = new ChunkMesh(this);
        }
        this.canRenderOldMesh = new AtomicBoolean(false);
    }

    public Chunk getChunk() {
        return chunk;
    }

    public boolean isFinished() {
        return isFinished.get();
    }
    public void setFinished(boolean finished) {
        canRenderOldMesh.set(!finished);
        isFinished.set(finished);
    }

    public boolean canRenderOldMesh() {
        return canRenderOldMesh.get();
    }

    public void scheduleForDisposal() {
        scheduledForDisposal.set(true);
    }

    public boolean isScheduledForDisposal() {
        return scheduledForDisposal.get();
    }

    public void dispose() {
        for (ChunkMesh layer : this.layers) {
            if (layer != null) layer.dispose();
        }
    }
}
