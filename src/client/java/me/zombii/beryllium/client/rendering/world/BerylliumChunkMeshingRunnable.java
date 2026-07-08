package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectSet;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;

public class BerylliumChunkMeshingRunnable implements Runnable {

    // A lot of this is basically just taken from base game code

    private final BerylliumChunkMeshingThread chunkMeshingThread;
    final ObjectSet<Chunk> immediateChunks = new ObjectSet<>();
    final ObjectSet<Chunk> chunks = new ObjectSet<>();
    public volatile boolean hasMeshedChunks = false;
    volatile boolean resortingChunksNeeded;
    public volatile boolean immediateResortingRequested;
    Array<Chunk> unMeshedChunksByDist = new Array<>(Chunk.class);

    private static final Vector3 tmpCamPos = new Vector3();
    IntMap<Array<Chunk>> buckets = new IntMap<>();
    IntArray bucketKeys = new IntArray();

    public BerylliumChunkMeshingRunnable(BerylliumChunkMeshingThread thread){
        this.chunkMeshingThread = thread;
    }

    private int getBucketKey(Chunk chunk) {
        float d = Vector3.dst(tmpCamPos.x, tmpCamPos.y, tmpCamPos.z, (float)chunk.blockX, (float)chunk.blockY, (float)chunk.blockZ);
        return (int)(d / 16.0F);
    }

    private void rebuildUnmeshedArray() {
        this.unMeshedChunksByDist.clear();
        tmpCamPos.set(InGame.IN_GAME.getWorldCamera().position);
        synchronized(this.chunks) {
            if (this.immediateChunks.isEmpty()) {
                this.unMeshedChunksByDist.ensureCapacity(this.chunks.size);

                Chunk chunk;
                Array<Chunk> bucket;
                for(ObjectSet.ObjectSetIterator<Chunk> iter = this.chunks.iterator(); iter.hasNext(); bucket.add(chunk)) {
                    chunk = iter.next();
                    int key = this.getBucketKey(chunk);
                    bucket = this.buckets.get(key);
                    if (bucket == null) {
                        bucket = new Array<>();
                        this.buckets.put(key, bucket);
                        this.bucketKeys.add(key);
                        this.bucketKeys.sort();
                    }
                }

                for(int i = this.bucketKeys.size - 1; i >= 0; --i) {
                    int key = this.bucketKeys.get(i);
                    Array<Chunk> a = this.buckets.get(key);
                    this.unMeshedChunksByDist.addAll(a);
                    a.clear();
                }

                this.resortingChunksNeeded = false;
            }
        }
    }

    private void meshForChunk(Chunk chunk){
        BerylliumMeshGroup group = (BerylliumMeshGroup) chunk.getMeshGroup();
        LayeredChunkMesh mesh = group.getAllMeshData();
        mesh.setFinished(false);
        ChunkMesher.meshChunk(chunk);
        mesh.setFinished(true);
        hasMeshedChunks = true;
        System.out.println("Meshed chunk " + chunk);
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        this.immediateResortingRequested = false;
        if (this.immediateChunks.notEmpty()) {
            synchronized(this.immediateChunks) {
                for (Chunk chunk : this.immediateChunks) {
                    this.meshForChunk(chunk);
                }

                this.immediateChunks.clear();
            }
        }

        this.rebuildUnmeshedArray();

        while(this.unMeshedChunksByDist.notEmpty()) {
            Chunk chunk;
            synchronized(this.chunks) {
                if (this.unMeshedChunksByDist.isEmpty()) {
                    break;
                }

                chunk = this.unMeshedChunksByDist.pop();
                this.chunks.remove(chunk);
            }

            this.meshForChunk(chunk);
            if (this.immediateResortingRequested || !this.immediateChunks.isEmpty() || this.resortingChunksNeeded && System.currentTimeMillis() - start > 125L) {
                break;
            }
        }

        if (this.chunks.isEmpty() && this.immediateChunks.isEmpty()) {
            this.chunkMeshingThread.pauseableThread.onPause();
        }
    }
}
