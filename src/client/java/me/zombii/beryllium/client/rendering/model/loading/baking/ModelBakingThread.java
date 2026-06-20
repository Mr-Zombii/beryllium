package me.zombii.beryllium.client.rendering.model.loading.baking;

import me.zombii.beryllium.client.rendering.model.BerylliumModel;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelBakingThread implements Runnable {

    public static final Thread THREAD = new Thread(new ModelBakingThread());
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static final Queue<Runnable> BAKING_QUEUE = new ConcurrentLinkedQueue<>();

    private static final AtomicBoolean IS_FINISHED = new AtomicBoolean(false);

    public static void requestMassBake(Collection<BerylliumModel> models) {
        BAKING_QUEUE.add(() -> {
            IS_FINISHED.set(false);
            int count = 0;
            for (BerylliumModel model : models) {
                count++;
                if (count % 10 == 0) {
                    System.out.println("Baked " + count + "/" + models.size() + " texture maps.");
                }
                ModelBaker.bakeTextures(model.getTextureMap());
            }
            System.out.println("Baked " + models.size() + "/" + models.size() + " texture maps.");
            ModelBaker.requestAtlasUpdateFromMainThread();

            CountDownLatch vertexLatch = new CountDownLatch(models.size());
            for (BerylliumModel model : models) {
                THREAD_POOL.submit(() -> {
                    synchronized (vertexLatch) {
                        long remaining = models.size() - vertexLatch.getCount();
                        if (remaining % 10 == 0) {
                            System.out.println("Baked " + remaining + "/" + models.size() + " models.");
                        }
                        ModelBaker.bakeModelVertices(model);
                        vertexLatch.countDown();
                    }
                });
            }
            try {
                vertexLatch.await();
                System.out.println("Baked " + models.size() + "/" + models.size() + " models.");
                THREAD_POOL.shutdown();
                System.out.println("Finished baking");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            ModelBaker.requestFaceBufferUpdateFromMainThread(IS_FINISHED);
        });
    }

    public static void postRunnable(Runnable runnable) {
        BAKING_QUEUE.add(runnable);
    }

    public static void start() {
        ModelBakingThread.THREAD.setName("Baking Thread");
        ModelBakingThread.THREAD.setDaemon(true);
        ModelBakingThread.THREAD.start();
        System.out.println("------------ STARTING MODEL THREAD -------------");
    }

    public static boolean isDoneBaking() {
        return IS_FINISHED.get();
    }

    @Override
    public void run() {
        while (true) {
            if (BAKING_QUEUE.isEmpty()) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (BAKING_QUEUE.isEmpty()) continue;

            Runnable result = Objects.requireNonNull(BAKING_QUEUE.poll());
            result.run();
        }
    }
}
