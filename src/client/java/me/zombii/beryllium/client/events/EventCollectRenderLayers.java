package me.zombii.beryllium.client.events;

import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import net.neoforged.bus.api.Event;

import java.util.List;

public class EventCollectRenderLayers extends Event {

    private final List<RenderLayer> layers;

    public EventCollectRenderLayers(List<RenderLayer> models) {
        this.layers = models;
    }

    public void registerRenderLayer(RenderLayer renderLayer) {
        this.layers.add(renderLayer);
    }

}
