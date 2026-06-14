package me.zombii.beryllium.client.rendering.model.loading.baking;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.puzzleshq.puzzleloader.cosmic.game.GameRegistries;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import finalforeach.cosmicreach.util.Identifier;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.BerylliumAtlases;
import me.zombii.beryllium.client.events.EventCollectModels;
import me.zombii.beryllium.client.rendering.model.*;
import me.zombii.beryllium.client.rendering.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.opengl.buffers.TBO;
import me.zombii.beryllium.client.rendering.opengl.textures.PixelMap;
import me.zombii.beryllium.client.rendering.opengl.textures.atlas.GLAtlas;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelBaker {

    private static final Object2ObjectMap<BerylliumModel, BakedBerylliumModel> modelMap = Object2ObjectMaps.synchronize(new Object2ObjectArrayMap<>());

    private static final Logger LOGGER = LogManager.getLogger("Beryllium | ModelBaker");

    public static void bakeTextures(Map<String, TextureEntry> textureMap) {
        GLAtlas albedoAtlas = BerylliumAtlases.ALBEDO_ATLAS;
        GLAtlas emissionAtlas = BerylliumAtlases.EMISSIVE_ATLAS;
        GLAtlas normalAtlas = BerylliumAtlases.NORMAL_ATLAS;
        GLAtlas materialAtlas = BerylliumAtlases.MATERIAL_ATLAS;

        BerylliumConfig config = BerylliumConfig.getOrLoad();

        for (TextureEntry value : textureMap.values()) {
            Identifier albedoTexturePath = value.getAlbedoTexturePath();

            BufferedImage albedoImage = IndependentAssetLoader.loadResource(albedoTexturePath, BufferedImage.class);
            albedoAtlas.add(albedoTexturePath.toString(), PixelMap.fromBufferedImage(albedoImage)).draw();

            if (config.enableEmissiveAtlas) {
                Identifier emissiveTexturePath = value.getEmissiveTexturePath();

                BufferedImage emissiveImage = IndependentAssetLoader.loadResource(emissiveTexturePath, BufferedImage.class);
                emissionAtlas.add(emissiveTexturePath.toString(), PixelMap.fromBufferedImage(emissiveImage)).draw();
            }

            if (config.enableNormalAtlas) {
                Identifier normalTexturePath = value.getNormalMapTexturePath();

                BufferedImage normalImage = IndependentAssetLoader.loadResource(normalTexturePath, BufferedImage.class);
                normalAtlas.add(normalTexturePath.toString(), PixelMap.fromBufferedImage(normalImage)).draw();
            }

            if (config.enableMaterialAtlas) {
                Identifier aoTexturePath = value.getAoMapTexturePath();
                Identifier metalnessTexturePath = value.getNormalMapTexturePath();
                Identifier roughnessTexturePath = value.getNormalMapTexturePath();
                Identifier depthTexturePath = value.getDepthMapTexturePath();

                BufferedImage aoImage = IndependentAssetLoader.loadResource(aoTexturePath, BufferedImage.class);
                BufferedImage depthImage = IndependentAssetLoader.loadResource(depthTexturePath, BufferedImage.class);
                BufferedImage metalnessImage = IndependentAssetLoader.loadResource(metalnessTexturePath, BufferedImage.class);
                BufferedImage roughnessImage = IndependentAssetLoader.loadResource(roughnessTexturePath, BufferedImage.class);

                int widths = aoImage.getWidth() + depthImage.getWidth() + metalnessImage.getWidth() + roughnessImage.getWidth();
                int heights = aoImage.getHeight() + depthImage.getHeight() + metalnessImage.getHeight() + roughnessImage.getHeight();

                if ((widths / 4 != aoImage.getWidth()) || (heights / 4 != aoImage.getHeight())) {
                    throw new RuntimeException("Dimensions for aoImage, depthImage, metalnessImage, roughnessImage do not match each other!");
                }

                PixelMap materialMap = new PixelMap(aoImage.getWidth(), aoImage.getHeight());
                for (int x = 0; x < materialMap.getWidth(); x++) {
                    for (int y = 0; y < materialMap.getHeight(); y++) {
                        int red = (aoImage.getRGB(x, y) >> 16) & 0xff;
                        int green = (depthImage.getRGB(x, y) >> 16) & 0xff;
                        int blue = (metalnessImage.getRGB(x, y) >> 16) & 0xff;
                        int alpha = (roughnessImage.getRGB(x, y) >> 16) & 0xff;

                        int pixelColor = (alpha << 24) | (red << 16) | (green << 8) | blue;

                        materialMap.setPixel(x, y, pixelColor);
                    }
                }

                materialAtlas.add(value.getAtlasMaterialID(), materialMap).draw();
            }
        }
    }

    private static final Matrix4 matRot = new Matrix4();
    private static final Matrix4 matTrns = new Matrix4();

    public static void requestAtlasUpdate() {
        BerylliumConfig config = BerylliumConfig.getOrLoad();

        ModelBakingThread.postRunnable(() -> {
            if (config.debugMode) LOGGER.log(Level.INFO, "Finished Baking Models");

            if (config.debugMode) LOGGER.log(Level.INFO, "Reordering \"{}\"'s objects", BerylliumAtlases.ALBEDO_ATLAS.getID());
            BerylliumAtlases.ALBEDO_ATLAS.reorder();
            if (config.debugMode) LOGGER.log(Level.INFO, "Redrawing \"{}\"'s objects", BerylliumAtlases.ALBEDO_ATLAS.getID());
            BerylliumAtlases.ALBEDO_ATLAS.redrawSubtextures();
            if (config.enableEmissiveAtlas) {
                if (config.debugMode) LOGGER.log(Level.INFO, "Reordering \"{}\"'s objects", BerylliumAtlases.EMISSIVE_ATLAS.getID());
                BerylliumAtlases.EMISSIVE_ATLAS.reorder();
                if (config.debugMode) LOGGER.log(Level.INFO, "Redrawing \"{}\"'s objects", BerylliumAtlases.EMISSIVE_ATLAS.getID());
                BerylliumAtlases.EMISSIVE_ATLAS.redrawSubtextures();
            }
            if (config.enableNormalAtlas) {
                if (config.debugMode) LOGGER.log(Level.INFO, "Reordering \"{}\"'s objects", BerylliumAtlases.NORMAL_ATLAS.getID());
                BerylliumAtlases.NORMAL_ATLAS.reorder();
                if (config.debugMode) LOGGER.log(Level.INFO, "Redrawing \"{}\"'s objects", BerylliumAtlases.NORMAL_ATLAS.getID());
                BerylliumAtlases.NORMAL_ATLAS.redrawSubtextures();
            }
            if (config.enableMaterialAtlas) {
                if (config.debugMode) LOGGER.log(Level.INFO, "Reordering \"{}\"'s objects", BerylliumAtlases.MATERIAL_ATLAS.getID());
                BerylliumAtlases.MATERIAL_ATLAS.reorder();
                if (config.debugMode) LOGGER.log(Level.INFO, "Redrawing \"{}\"'s objects", BerylliumAtlases.MATERIAL_ATLAS.getID());
                BerylliumAtlases.MATERIAL_ATLAS.redrawSubtextures();
            }

            Gdx.app.postRunnable(() -> {
                BerylliumAtlases.AlbedoUVBuffer = createOrUpdateTBO(
                        config,
                        BerylliumAtlases.ALBEDO_ATLAS,
                        BerylliumAtlases.AlbedoUVBuffer
                );

                if (config.enableEmissiveAtlas) {
                    BerylliumAtlases.EmissiveUVBuffer = createOrUpdateTBO(
                            config,
                            BerylliumAtlases.EMISSIVE_ATLAS,
                            BerylliumAtlases.EmissiveUVBuffer
                    );
                }

                if (config.enableNormalAtlas) {
                    BerylliumAtlases.NormalUVBuffer = createOrUpdateTBO(
                            config,
                            BerylliumAtlases.NORMAL_ATLAS,
                            BerylliumAtlases.NormalUVBuffer
                    );
                }

                if (config.enableMaterialAtlas) {
                    BerylliumAtlases.MaterialUVBuffer = createOrUpdateTBO(
                            config,
                            BerylliumAtlases.MATERIAL_ATLAS,
                            BerylliumAtlases.MaterialUVBuffer
                    );
                }

                try {
                    BerylliumAtlases.ALBEDO_ATLAS.toFile(new File("ambient.png"));
                    if (config.enableEmissiveAtlas) BerylliumAtlases.EMISSIVE_ATLAS.toFile(new File("emissive.png"));
                    if (config.enableNormalAtlas) BerylliumAtlases.NORMAL_ATLAS.toFile(new File("normal.png"));
                    if (config.enableMaterialAtlas) BerylliumAtlases.MATERIAL_ATLAS.toFile(new File("material.png"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (config.debugMode)
                    LOGGER.log(Level.INFO, "Uploading atlases");

                BerylliumAtlases.ALBEDO_ATLAS.upload();
                if (config.enableEmissiveAtlas) BerylliumAtlases.EMISSIVE_ATLAS.upload();
                if (config.enableNormalAtlas) BerylliumAtlases.NORMAL_ATLAS.upload();
                if (config.enableMaterialAtlas) BerylliumAtlases.MATERIAL_ATLAS.upload();
            });
        });
    }

    public static void requestFaceBufferUpdate() {
        BerylliumConfig config = BerylliumConfig.getOrLoad();

        ModelBakingThread.postRunnable(() -> {
            Gdx.app.postRunnable(() -> {
                BerylliumAtlases.PerFaceUVBuffer = createOrUpdateFaceUVTBO(
                        config,
                        BerylliumAtlases.PerFaceUVBuffer
                );
            });
        });
    }

    private static final int elementSize = 4 * 4;
    private static final int atlasElementSize = 4 * 4;
    private static final float[] uvs = new float[4];

    private static TBO createOrUpdateTBO(BerylliumConfig config, GLAtlas atlas, TBO tbo) {
        if (tbo == null) {
            if (config.debugMode) LOGGER.log(Level.INFO, "Creating \"{}\"'s TBO", atlas.getID());
            tbo = new TBO(atlas.getSubtextureCount() * atlasElementSize, GL30.GL_RG16UI, true);
        }
        if (config.debugMode) LOGGER.log(Level.INFO, "Updating \"{}\"'s TBO", atlas.getID());
        uploadData(atlas, tbo);
        return tbo;
    }

    private static void uploadData(GLAtlas atlas, TBO tbo) {
        List<GLAtlas.SubTexture> texs = atlas.getSubTextures();
        int texCount = texs.size();
        int tboSize = texCount * atlasElementSize;

        if (tboSize > tbo.getSize()) {
            tbo.resize(tboSize);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(tboSize).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < texCount; i++) {
                GLAtlas.SubTexture tex = texs.get(i);
                buffer.putShort((short) tex.getX());
                buffer.putShort((short) tex.getY());
                tex.setTBOIndex(i);
            }
            buffer.flip();

            tbo.write(0, buffer);
        }

    }

    private static TBO createOrUpdateFaceUVTBO(BerylliumConfig config, TBO tbo) {
        if (tbo == null) {
            if (config.debugMode) LOGGER.log(Level.INFO, "Creating FaceUVBuffer's TBO");
            tbo = new TBO(NEXT_INDEX.get() * elementSize, GL30.GL_RGBA16UI, true);
        }
        if (config.debugMode) LOGGER.log(Level.INFO, "Updating FaceUVBuffer's TBO");
        uploadFaceUVData(tbo);
        return tbo;
    }

    private static void uploadFaceUVData(TBO tbo) {
        int elementCount = NEXT_INDEX.get();
        int tboSize = elementCount * elementSize;

        if (tboSize > tbo.getSize()) {
            tbo.resize(tboSize);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer buffer = stack.malloc(tboSize).order(ByteOrder.LITTLE_ENDIAN);

            for (int i = 0; i < elementCount; i++) {
                int[] data = UV_STACK.get(i);

                buffer.putShort((short) data[0]);
                buffer.putShort((short) data[1]);
                buffer.putShort((short) data[2]);
                buffer.putShort((short) data[3]);
            }
            buffer.flip();

            tbo.write(0, buffer);
        }

    }

    public static void requestModelToBake(BerylliumModel model) {
        ModelBakingThread.requestBaking(model);
        requestAtlasUpdate();
        requestFaceBufferUpdate();
    }

    public static void requestModelsToBake(List<BerylliumModel> models) {
        for (BerylliumModel model : models) {
            ModelBakingThread.requestBaking(model);
        }
        requestAtlasUpdate();
        requestFaceBufferUpdate();
    }

    public static void requestAllModelsToBake() {
        requestModelsToBake(BerylliumModelLoader.getModels());
        requestAtlasUpdate();
        requestFaceBufferUpdate();
    }

    public static void bakeGroups(
            BakedBerylliumModel berylliumModel,
            ObjectList<VertexGroup> vertexGroups,
            Object2ObjectMap<String, VertexGroup> groupMap,
            List<PartGroup> groups
    ) {
        for (PartGroup group : groups) {
            VertexGroup vertexGroup = new VertexGroup(berylliumModel, group.getName(), group.getParentName());
            vertexGroups.add(vertexGroup);
            groupMap.put(group.getName(), vertexGroup);

            vertexGroup.setEnabled(group.isEnabled());
            vertexGroup.setPivot(group.getPivot());
            vertexGroup.setRotation(group.getRotation());

            for (Part part : group)
                bakePart(
                        part,
                        vertexGroup
                );
        }
    }

    private static final float sixteenth = 1/16f;

    public static void bakePart(
            Part part,
            VertexGroup group
    ) {
        for (PartFace face : part.getFaces()) {
            if (face == null) continue;

            int direction = face.getDirection();

            Vector3 rotation = part.getRotation();
            Vector3 pivot = part.getPivot();
            Vector3 size = part.getSize();
            float scale = part.getScale();

//            matRot.idt();
//            matRot.translate(pivot);
//            matRot.rotate(Vector3.Z, rotation.z);
//            matRot.rotate(Vector3.Y, rotation.y);
//            matRot.rotate(Vector3.X, rotation.x);
//            matRot.translate(-pivot.x, -pivot.y, -pivot.z);

            Vector3 pos = part.getPos();

            matTrns.idt();
            matTrns.translate(
                    pos.x * sixteenth,
                    pos.y * sixteenth,
                    pos.z * sixteenth
            );
            matTrns.translate(
                    (size.x * sixteenth) / 2f,
                    (size.y * sixteenth) / 2f,
                    (size.z * sixteenth) / 2f
            );
            matTrns.scl(size.x + scale, size.y + scale, size.z + scale);
            matTrns.scl(sixteenth);

//            matTrns.mul(matRot);

            BaseQuad quad = BaseQuad.FACES[direction];

            group.getFacesByDirection(face.isCulled() ? direction : -1)
                    .add(BakedFace.bake(
                            group.getModel(),
                            quad, face,
                            matTrns
                    ));
        }
    }

    public static void bake(BerylliumModel model) {
        ObjectList<VertexGroup> groups = new ObjectArrayList<>();
        Object2ObjectMap<String, VertexGroup> groupMap = new Object2ObjectArrayMap<>();

        BakedBerylliumModel bakedModel = new BakedBerylliumModel(
                model, groups, groupMap
        );

        bakeTextures(model.getTextureMap());
        bakeGroups(
                bakedModel,
                groups, groupMap,
                model.getGroups()
        );
        if (BerylliumConfig.getOrLoad().debugMode) {
            int groupCount = groups.size();
            int quadCount = 0;
            for (VertexGroup vertexGroup : groups) {
                for (int i = -1; i < 6; i++) {
                    quadCount += vertexGroup.getFacesByDirection(i).size();
                }
            }
            LOGGER.log(Level.INFO,
                    "Baked Model \"{}\", {} Group(s), {} Quad(s), {} Triangle(s), {}",
                    model.getID(), groupCount, quadCount, quadCount * 2, quadCount != 1 ? "Vertices" : "Vertex"
            );
        }
        modelMap.put(model, bakedModel);
    }

    public static void collectAndBake() {
        ObjectList<BerylliumModel> collectedModels = new ObjectArrayList<>();
        EventCollectModels collectModelsEvent = new EventCollectModels(collectedModels);
        GameRegistries.COSMIC_EVENT_BUS.post(collectModelsEvent);

        if (BerylliumConfig.getOrLoad().debugMode)
            LOGGER.log(Level.INFO, "Collected {} models for baking", collectedModels.size());

        for (BerylliumModel collectedModel : collectedModels) {
            if (!BerylliumModelLoader.isRegistered(collectedModel)) {
                BerylliumModelLoader.register(collectedModel);
            }
        }

        ModelBaker.requestModelsToBake(collectedModels);
    }

    public static BakedBerylliumModel get(BerylliumModel model) {
        return modelMap.get(model);
    }

    public static Map<BerylliumModel, BakedBerylliumModel> getModelMap() {
        return Object2ObjectMaps.unmodifiable(modelMap);
    }

    private static final ObjectList<int[]> UV_STACK = new ObjectArrayList<>();
    private static final Int2IntMap UV_TABLE = new Int2IntArrayMap();
    private static final AtomicInteger NEXT_INDEX = new AtomicInteger(0);

    public static int getOrMakePerFaceIdx(int[] uvs) {
        int hash = Objects.hash(uvs[0], uvs[1], uvs[2], uvs[3]);
        if (UV_TABLE.containsKey(hash)) {
            return UV_TABLE.get(hash);
        }
        int idx = NEXT_INDEX.getAndIncrement();
        UV_STACK.add(uvs);
        UV_TABLE.put(hash, idx);

        return idx;
    }
}
