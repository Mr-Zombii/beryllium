package me.zombii.beryllium.client;

import dev.puzzleshq.puzzleloader.cosmic.core.registries.GenericRegistry;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.IRegistry;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import me.zombii.beryllium.client.rendering.model.loading.baking.BakedBerylliumModel;
import me.zombii.beryllium.common.BerylliumCommon;

public class BerylliumClientRegistries {

    public static IRegistry<RenderLayer> RENDER_LAYER_REGISTRY = registry("RENDER_LAYERS");
    public static IRegistry<BerylliumModel> MODEL_REGISTRY = registry("MODELS");
    public static IRegistry<BakedBerylliumModel> BAKED_MODEL_REGISTRY = registry("BAKED_MODELS");

    private static <T> IRegistry<T> registry(String name) {
        return new GenericRegistry<>(Identifier.of(BerylliumCommon.NAMESPACE, name));
    }

}
