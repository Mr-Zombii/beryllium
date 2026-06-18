package me.zombii.beryllium.client.rendering.world;

import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BerylliumMeshingThread implements Runnable {

    public static final Thread THREAD = new Thread(new BerylliumMeshingThread());

    static {
        THREAD.setDaemon(true);
        THREAD.setName("Beryllium ChunkMeshing Thread");
    }

    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static final Queue<Runnable> normalRunnableQueue = new ConcurrentLinkedQueue<>();
    private static final Queue<Runnable> immediateRunnableQueue = new ConcurrentLinkedQueue<>();
    private static final Map<Chunk, LayeredChunkMesh> workingList = new ConcurrentHashMap<>();

    public static void setRunning(boolean running) {
        BerylliumMeshingThread.running.set(running);
    }

    public static LayeredChunkMesh queueChunk(Chunk chunk, boolean immediate) {
        LayeredChunkMesh chunkMesh = new LayeredChunkMesh(chunk, new AtomicBoolean(false));
        return queueChunk(chunkMesh, immediate);
    }

    public static void clear() {
        normalRunnableQueue.clear();
        immediateRunnableQueue.clear();
    }

    public static LayeredChunkMesh queueChunk(LayeredChunkMesh chunkMesh, boolean immediate) {
        if (workingList.containsKey(chunkMesh.getChunk())) {
            return workingList.get(chunkMesh.getChunk());
        }
        workingList.put(chunkMesh.getChunk(), chunkMesh);
        Runnable r = () -> {
            chunkMesh.setFinished(false);
            ChunkMesher.meshChunk(chunkMesh.getChunk(), chunkMesh);
            chunkMesh.setFinished(true);
            workingList.remove(chunkMesh.getChunk());
        };
        if (immediate) immediateRunnableQueue.add(r); else normalRunnableQueue.add(r);
        return chunkMesh;
    }

    @Override
    public void run() {
        while (running.get()) {
            if (normalRunnableQueue.isEmpty() || immediateRunnableQueue.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            if (!normalRunnableQueue.isEmpty()) {
                Runnable chunkRunnable = normalRunnableQueue.poll();
                chunkRunnable.run();
            }
            if (!immediateRunnableQueue.isEmpty()) {
                Runnable immediateRunnable = immediateRunnableQueue.poll();
                immediateRunnable.run();
            }
        }
    }
}
