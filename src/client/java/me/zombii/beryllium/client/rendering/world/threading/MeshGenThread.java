package me.zombii.beryllium.client.rendering.world.threading;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.PauseableThread;
import finalforeach.cosmicreach.rendering.*;
import finalforeach.cosmicreach.rendering.meshes.MeshData;
import finalforeach.cosmicreach.util.Threads;
import finalforeach.cosmicreach.world.Chunk;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class MeshGenThread implements IWorldRenderingMeshGenThread, Runnable {
    public PauseableThread pauseableThread;
    private boolean started = false;
    public volatile boolean immediateResortingRequested;
    static final ObjectSet<Chunk> immediateChunks = new ObjectOpenHashSet<>();
    static final ObjectSet<Chunk> chunks = new ObjectOpenHashSet<>();
    static final ObjectSet<Chunk> meshedChunks = new ObjectOpenHashSet<>();
    public MeshGenThread() {
        this.pauseableThread = Threads.createPauseableThread("WorldRenderingMeshGenThread_br", this);
    }

    public void addChunk(Chunk chunk) {
        IChunkMeshGroup<?> meshGroup = chunk.getMeshGroup();
        if (meshGroup.isFlaggedForImmediateRemesh()) {
            synchronized (immediateChunks) {
               immediateChunks.add(chunk);
            }

            meshGroup.setToRemeshImmediately(false);
        } else if (chunks.add(chunk)) {
          // resortingChunksNeeded = true; //TODO
        }
    }

    public Object getAddChunkLock() {
        return chunks;
    }

    public void meshChunks(IZoneRenderer zoneRenderer) {
        if (this.hasChunksToMesh()) {
            if (!this.started) {
                this.pauseableThread.start();
                this.started = true;
            } else {
                this.pauseableThread.onResume();
            }
        }

        if (!meshedChunks.isEmpty()){
            for (Chunk chunk : meshedChunks) {
                zoneRenderer.onChunkMeshed(chunk);
//                meshedChunks.
            }
            meshedChunks.clear();
        }
    }

    public void unload() {
        this.pauseableThread.onPause();
        synchronized (chunks) {
            chunks.clear();
            meshedChunks.clear();
        }
    }

    public boolean hasChunksToMesh() {
        return !chunks.isEmpty();
    }

    public void requestImmediateResorting() {
        immediateResortingRequested = true;
    }

    public boolean shouldImmediatelyResort() {
        return immediateResortingRequested;
    }

    public void stopThread() {
        this.pauseableThread.stopThread();
    }

    @Override
    public void run() {
        if(!immediateChunks.isEmpty()) {
            for (Chunk chunk : immediateChunks) {
                LayeredChunkMesh chunkMesh = (LayeredChunkMesh) chunk.getMeshGroup().getAllMeshData();
                chunkMesh.setFinished(false);
                ChunkMesher.meshChunk(chunk);
                chunkMesh.setFinished(true);
                meshedChunks.add(chunk);
            }
        }
        if(!chunks.isEmpty()) {
            for (Chunk chunk : chunks) {
                LayeredChunkMesh chunkMesh = (LayeredChunkMesh) chunk.getMeshGroup().getAllMeshData();
                chunkMesh.setFinished(false);
                ChunkMesher.meshChunk(chunk);
                chunkMesh.setFinished(true);
                meshedChunks.add(chunk);
            }
        }
        immediateChunks.clear();
        chunks.clear();
    }
}
