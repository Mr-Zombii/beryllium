package me.zombii.beryllium.client;

import com.badlogic.gdx.Gdx;
import dev.puzzleshq.puzzleloader.cosmic.game.GameRegistries;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.ISidedModelLoader;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import dev.puzzleshq.puzzleloader.loader.launch.Piece;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.client.ClientModInit;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.client.ClientPostModInit;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.singletons.GameSingletons;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.util.assets.GameAssetLoader;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.zombii.beryllium.client.events.EventCollectModels;
import me.zombii.beryllium.client.events.EventCollectRenderLayers;
import me.zombii.beryllium.client.events.EventDebugBlockLoadingQueue;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.baking.ModelBakingThread;
import me.zombii.beryllium.client.model.loading.NewBlockModelInstantiator;
import me.zombii.beryllium.client.model.loading.NewClientModelLoader;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.world.BerylliumMeshingThread;
import me.zombii.beryllium.client.rendering.world.BerylliumZoneRenderer;
import me.zombii.beryllium.common.BerylliumCommon;
import me.zombii.beryllium.common.BerylliumConfig;
import net.neoforged.bus.api.SubscribeEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BerylliumClient implements ClientModInit, ClientPostModInit {

    public static final int ATLAS_SIZE = 1024;

    public static final Identifier MISSING_TEXTURE_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/missing/missing-texture.png");
    public static final Identifier MISSING_TEXTURE_EMISSIVE_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/missing/missing-texture-emissive.png");

    public static final Identifier DEFAULT_TEXTURE_EMISSIVE_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/default/default-texture-emissive.png");
    public static final Identifier DEFAULT_TEXTURE_NORMAL_MAP_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/default/default-texture-normal-map.png");
    public static final Identifier DEFAULT_TEXTURE_AO_MAP_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/default/default-texture-ao-map.png");
    public static final Identifier DEFAULT_TEXTURE_DEPTH_MAP_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/default/default-texture-depth-map.png");
    public static final Identifier DEFAULT_TEXTURE_ROUGHNESS_MAP_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/default/default-texture-roughness-map.png");
    public static final Identifier DEFAULT_TEXTURE_METALNESS_MAP_PATH = Identifier.of(BerylliumCommon.NAMESPACE, "textures/default/default-texture-metalness-map.png");
    public static final ObjectList<BerylliumModel> INTERCEPTED_MODELS = new ObjectArrayList<>();

    public BerylliumClient() {
        GameRegistries.COSMIC_EVENT_BUS.register(this);
    }

    @Override
    public void onClientInit() {
        if (BerylliumConfig.INSTANCE.enableBerylliumRendering) {
            ISidedModelLoader.CONTEXTUAL_INSTANCE.set(new NewClientModelLoader());

            BerylliumAtlases.initAtlases();
            ModelBakingThread.start();

            IndependentAssetLoader.registerLoadingMethod(BufferedImage.class, (handle) -> {
                try {
                    InputStream stream = handle.read();
                    BufferedImage image = ImageIO.read(stream);
                    stream.close();
                    return image;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Gdx.app.postRunnable(RenderLayers::collectAndCompile);
        }
    }

    @SubscribeEvent
    public void onEvent(EventDebugBlockLoadingQueue event) {
        if (BerylliumConfig.INSTANCE.enableDebugBlock) {
            event.registerToQueue(() -> {
                Block.loadBlock(GameAssetLoader.loadAsset(Identifier.of(BerylliumCommon.NAMESPACE, "blocks/debug.json")));
            });
        }
    }

    @SubscribeEvent
    public void onEvent(EventCollectModels event) {
        for (URL source : Piece.classLoader.getURLs()) {
            System.out.println("Found: " + source.getFile());
        }

//        GameAssetLoader.forEachAsset("models/blocks", ".json", (p, f) -> {
//            try {
//                event.registerForBaking(BerylliumModelLoader.loadVanillaBlockModel(p.trim(), f));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });

        for (BerylliumModel interceptedModel : INTERCEPTED_MODELS) {
            event.registerForBaking(interceptedModel);
        }
    }

    @SubscribeEvent
    public void onEvent(EventCollectRenderLayers event) {
        event.registerRenderLayer(new RenderLayer(
                Identifier.of(BerylliumCommon.NAMESPACE, "shaders/block-shader.vert"),
                Identifier.of(BerylliumCommon.NAMESPACE, "shaders/block-shader.frag"),
                Identifier.of(BerylliumCommon.NAMESPACE, "opaque-block-render-layer"),
                true, 0
        ));
        event.registerRenderLayer(new RenderLayer(
                Identifier.of(BerylliumCommon.NAMESPACE, "shaders/block-shader.vert"),
                Identifier.of(BerylliumCommon.NAMESPACE, "shaders/block-shader.frag"),
                Identifier.of(BerylliumCommon.NAMESPACE, "translucent-block-render-layer"),
                true, 1
        ));
    }

    @Override
    public void onClientPostInit() {
        if (BerylliumConfig.INSTANCE.enableBerylliumRendering) {
            BerylliumMeshingThread.THREAD.start();
            GameSingletons.zoneRenderer = new BerylliumZoneRenderer();
//            GameSingletons.zoneRenderer = new NewZoneRenderer();
//            GameSingletons.meshGenThread = new MeshGenThread();
            GameSingletons.blockModelInstantiator = new NewBlockModelInstantiator();
        };
//        register(Block.getById("base:grass"), (state, pos, tintIdx) -> {
//            int r = (int) ((pos.localX / 16f) * 255);
//            int g = (int) ((pos.localY / 16f) * 255);
//            int b = (int) ((pos.localZ / 16f) * 255);
//            return argb8888ToRgb565(new Color(r, g, b).getRGB());
//        });
    }
}
