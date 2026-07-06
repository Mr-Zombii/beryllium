package me.zombii.beryllium.client.events;


import com.badlogic.gdx.utils.Queue;
import net.neoforged.bus.api.Event;

public class EventDebugBlockLoadingQueue extends Event {
    private final Queue<Runnable> loadingQueue;

    public EventDebugBlockLoadingQueue(Queue<Runnable> loadingQueue) {
        this.loadingQueue = loadingQueue;
    }

    public void registerToQueue(Runnable runnable) {
        this.loadingQueue.addLast(runnable);
    }
}
