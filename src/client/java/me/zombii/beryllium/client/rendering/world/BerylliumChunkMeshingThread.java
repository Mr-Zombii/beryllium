package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.utils.PauseableThread;
import finalforeach.cosmicreach.rendering.IChunkMeshGroup;
import finalforeach.cosmicreach.rendering.IWorldRenderingMeshGenThread;
import finalforeach.cosmicreach.rendering.IZoneRenderer;
import finalforeach.cosmicreach.util.Threads;
import finalforeach.cosmicreach.world.Chunk;

public class BerylliumChunkMeshingThread implements IWorldRenderingMeshGenThread {
    public PauseableThread pauseableThread;
    BerylliumChunkMeshingRunnable chunkMeshRunnable = new BerylliumChunkMeshingRunnable(this);
    private boolean started = false;

    public BerylliumChunkMeshingThread() {
        this.pauseableThread = Threads.createPauseableThread("BerylliumChunkMeshingThread", this.chunkMeshRunnable);
    }

    @Override
    public void requestImmediateResorting() {
        this.chunkMeshRunnable.immediateResortingRequested = true;
    }

    @Override
    public void addChunk(Chunk chunk) {
        IChunkMeshGroup<?> meshGroup = chunk.getMeshGroup();
        if (meshGroup.isFlaggedForImmediateRemesh()) {
            synchronized(this.chunkMeshRunnable.immediateChunks) {
                this.chunkMeshRunnable.immediateChunks.add(chunk);
            }

            meshGroup.setToRemeshImmediately(false);
        } else if (this.chunkMeshRunnable.chunks.add(chunk)) {
            this.chunkMeshRunnable.resortingChunksNeeded = true;
        }
    }

    @Override
    public void meshChunks(IZoneRenderer iZoneRenderer) {
        if (this.hasChunksToMesh()) {
            if (!this.started) {
                this.pauseableThread.start();
                this.started = true;
            } else {
                this.pauseableThread.onResume();
            }
        }

        if (this.chunkMeshRunnable.hasMeshedChunks) {
            iZoneRenderer.onChunkMeshed(null);
            this.chunkMeshRunnable.hasMeshedChunks = false;
        }
    }

    @Override
    public void stopThread() {
        this.pauseableThread.stopThread();
    }

    @Override
    public Object getAddChunkLock() {
        return this.chunkMeshRunnable.chunks;
    }

    @Override
    public void unload() {
        this.pauseableThread.onPause();
        synchronized(this.chunkMeshRunnable.chunks) {
            this.chunkMeshRunnable.unMeshedChunksByDist.clear();
            this.chunkMeshRunnable.chunks.clear();
            this.chunkMeshRunnable.hasMeshedChunks = false;
        }
    }

    @Override
    public boolean hasChunksToMesh() {
        return this.chunkMeshRunnable.chunks.size > 0;
    }
}
