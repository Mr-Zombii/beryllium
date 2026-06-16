package me.zombii.beryllium.client.rendering.layers;

import com.badlogic.gdx.Gdx;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.GenericRegistry;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.IRegistry;
import dev.puzzleshq.puzzleloader.cosmic.game.GameRegistries;
import finalforeach.cosmicreach.util.Identifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.zombii.beryllium.client.events.EventCollectRenderLayers;
import me.zombii.beryllium.common.BerylliumCommon;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RenderLayers {

    public static final IRegistry<RenderLayer> LAYER_REGISTRY = new GenericRegistry<>(Identifier.of(BerylliumCommon.NAMESPACE, "RENDER_LAYERS"));
    private static final Logger LOGGER = LogManager.getLogger("Beryllium | RenderLayers");

    public static void collectAndCompile() {
        ObjectList<RenderLayer> collectedRenderLayers = new ObjectArrayList<>();
        EventCollectRenderLayers collectRenderLayersEvent = new EventCollectRenderLayers(collectedRenderLayers);
        GameRegistries.COSMIC_EVENT_BUS.post(collectRenderLayersEvent);

        boolean debugMode = BerylliumConfig.INSTANCE.debugMode;

        if (debugMode)
            LOGGER.log(Level.INFO, "Collected {} RenderLayer(s) for registration", collectedRenderLayers.size());

        for (RenderLayer renderLayer : collectedRenderLayers) {
            LAYER_REGISTRY.store(renderLayer.getId(), renderLayer);
            Gdx.app.postRunnable(() -> {
                if (debugMode)
                    LOGGER.log(Level.INFO,
                            "Compiling Render Layer \"{}\", RenderOrder: {}, Uses DepthBuffer: {}",
                            renderLayer.getId(), renderLayer.getSortOrder(), renderLayer.usesDepthBuffer()
                    );
                renderLayer.getProgram().fetchAndCompile();
            });
        }
        Gdx.app.postRunnable(() -> {
            if (debugMode)
                LOGGER.log(Level.INFO, "Compiled all {} render layers, freezing layer registry.", collectedRenderLayers.size());
        });

        LAYER_REGISTRY.freeze();
    }

}
