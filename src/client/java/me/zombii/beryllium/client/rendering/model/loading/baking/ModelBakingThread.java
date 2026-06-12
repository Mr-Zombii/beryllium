package me.zombii.beryllium.client.rendering.model.loading.baking;

import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import me.zombii.beryllium.common.BerylliumConfig;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ModelBakingThread implements Runnable {

    public static final Thread THREAD = new Thread(new ModelBakingThread());

    private static final Queue<Runnable> BAKING_QUEUE = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean IS_BAKING = new AtomicBoolean(false);

    public static AtomicBoolean requestBaking(@NonNull BerylliumModel model) {
        AtomicBoolean isBaked = new AtomicBoolean(false);
        BAKING_QUEUE.offer(() -> {
            IS_BAKING.set(true);

            ModelBaker.bake(model);
            isBaked.set(true);
            IS_BAKING.set(false);
        });
        return isBaked;
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

    public static boolean isBaking() {
        return !BAKING_QUEUE.isEmpty() || IS_BAKING.get();
    }

    @Override
    public void run() {
        while (true) {
            if (BAKING_QUEUE.isEmpty()) {
                try {
                    Thread.sleep(100);
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
