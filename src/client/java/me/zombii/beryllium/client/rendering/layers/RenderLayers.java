package me.zombii.beryllium.client.rendering.layers;

import com.badlogic.gdx.Gdx;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.GenericRegistry;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.IRegistry;
import dev.puzzleshq.puzzleloader.cosmic.game.GameRegistries;
import finalforeach.cosmicreach.util.Identifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.zombii.beryllium.client.BerylliumClientRegistries;
import me.zombii.beryllium.client.events.EventCollectRenderLayers;
import me.zombii.beryllium.common.BerylliumCommon;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Comparator;

public class RenderLayers {

    public static RenderLayer[] LAYER_ORDER;
    private static final Logger LOGGER = LogManager.getLogger("Beryllium | RenderLayers");

    public static void collectAndCompile() {
        ObjectList<RenderLayer> collectedRenderLayers = new ObjectArrayList<>();
        EventCollectRenderLayers collectRenderLayersEvent = new EventCollectRenderLayers(collectedRenderLayers);
        GameRegistries.COSMIC_EVENT_BUS.post(collectRenderLayersEvent);

        boolean debugMode = BerylliumConfig.INSTANCE.debugMode;

        if (debugMode)
            LOGGER.log(Level.INFO, "Collected {} RenderLayer(s) for registration", collectedRenderLayers.size());

        for (RenderLayer renderLayer : collectedRenderLayers) {
            BerylliumClientRegistries.RENDER_LAYERS.store(renderLayer.getId(), renderLayer);
            Gdx.app.postRunnable(() -> {
                if (debugMode)
                    LOGGER.log(Level.INFO,
                            "Compiling Render Layer \"{}\", RenderOrder: {}, Uses DepthBuffer: {}",
                            renderLayer.getId(), renderLayer.getSortOrder(), renderLayer.usesDepthBuffer()
                    );
                renderLayer.getProgram().fetchAndCompile();
            });
        }
        LAYER_ORDER = collectedRenderLayers.toArray(new RenderLayer[0]);
        Arrays.sort(LAYER_ORDER, Comparator.comparingInt(RenderLayer::getSortOrder));

        Gdx.app.postRunnable(() -> {
            if (debugMode)
                LOGGER.log(Level.INFO, "Compiled all {} render layers, freezing layer registry.", collectedRenderLayers.size());
        });

        BerylliumClientRegistries.RENDER_LAYERS.freeze();
    }

}
