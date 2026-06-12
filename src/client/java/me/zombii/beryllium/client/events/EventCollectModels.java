package me.zombii.beryllium.client.events;

import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import net.neoforged.bus.api.Event;

import java.util.List;

public class EventCollectModels extends Event {

    private final List<BerylliumModel> models;

    public EventCollectModels(List<BerylliumModel> models) {
        this.models = models;
    }

    public void registerForBaking(BerylliumModel model) {
        this.models.add(model);
    }

}
