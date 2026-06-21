package me.zombii.beryllium.client;

import dev.puzzleshq.puzzleloader.cosmic.core.registries.GenericRegistry;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.IRegistry;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.common.BerylliumCommon;

public class BerylliumClientRegistries {

    public static IRegistry<RenderLayer> RENDER_LAYERS = registry("RENDER_LAYERS");
    
    private static <T> IRegistry<T> registry(String name) {
        return new GenericRegistry<>(Identifier.of(BerylliumCommon.NAMESPACE, name));
    }

}
