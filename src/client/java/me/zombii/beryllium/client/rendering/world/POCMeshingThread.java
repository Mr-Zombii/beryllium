package me.zombii.beryllium.client.rendering.world;

import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

public class POCMeshingThread implements Runnable {

    public static final Thread THREAD = new Thread(new POCMeshingThread());

    static {
        THREAD.setDaemon(true);
        THREAD.setName("Beryllium ChunkMeshing Thread");
    }

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final Queue<Runnable> normalRunnableQueue = new ConcurrentLinkedQueue<>();
    private static final Set<Chunk> workingList = new CopyOnWriteArraySet<>();

    public static void setRunning(boolean running) {
        POCMeshingThread.running.set(running);
    }

    public static void clear() {
        normalRunnableQueue.clear();
        workingList.clear();
    }

    public static boolean queueChunk(Chunk chunk) {
        if (chunk.getMeshGroup() == null) {
            chunk.initMeshGroup(BerylliumMeshGroup::new);
        }

        if (((LayeredChunkMesh)chunk.getMeshGroup().getAllMeshData()).isScheduledForDisposal()) return false;

        if (!workingList.add(chunk)) {
            return false;
        }
        Runnable r = () -> {
            LayeredChunkMesh mesh = (LayeredChunkMesh) chunk.getMeshGroup().getAllMeshData();
            mesh.setFinished(false);
            ChunkMesher.meshChunk(chunk);
            mesh.setFinished(true);
            workingList.remove(chunk);
        };
        normalRunnableQueue.add(r);
        return true;
    }

    @Override
    public void run() {
        while (running.get()) {
            if (normalRunnableQueue.isEmpty()) {
                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            Runnable chunkRunnable = normalRunnableQueue.poll();
            chunkRunnable.run();
        }
    }
}
