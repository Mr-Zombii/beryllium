package me.zombii.beryllium.client.rendering.world;

import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesh;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMeshingGroup;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class BerylliumMeshingThread implements Runnable {

    public static final Thread THREAD = new Thread(new BerylliumMeshingThread());

    static {
        THREAD.setDaemon(true);
        THREAD.setName("Beryllium ChunkMeshing Thread");
    }

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final Queue<Runnable> chunkRunnableQueue = new ConcurrentLinkedQueue<>();
    private static final Map<Chunk, ChunkMesh> workingList = new ConcurrentHashMap<>();

    public static void setRunning(boolean running) {
        BerylliumMeshingThread.running.set(running);
    }

    public static ChunkMesh queueChunk(Chunk chunk) {
        ChunkMesh chunkMesh = new ChunkMesh(chunk, new AtomicBoolean(false));
        return queueChunk(chunkMesh);
    }

    public static void clear() {
        chunkRunnableQueue.clear();
    }

    public static ChunkMesh queueChunk(ChunkMesh chunkMesh) {
        if (workingList.containsKey(chunkMesh.getChunk())) {
            return workingList.get(chunkMesh.getChunk());
        }
        workingList.put(chunkMesh.getChunk(), chunkMesh);
        chunkRunnableQueue.add(() -> {
            chunkMesh.getIsFinished().set(false);
            ChunkMeshingGroup.meshChunk(chunkMesh.getChunk(), chunkMesh);
            chunkMesh.getIsFinished().set(true);
            workingList.remove(chunkMesh.getChunk());
        });
        return chunkMesh;
    }

    @Override
    public void run() {
        while (running.get()) {
            if (chunkRunnableQueue.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            Runnable chunkRunnable = chunkRunnableQueue.poll();
            chunkRunnable.run();
        }
    }
}
