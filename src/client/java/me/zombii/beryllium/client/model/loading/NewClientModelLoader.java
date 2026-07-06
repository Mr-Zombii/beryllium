package me.zombii.beryllium.client.model.loading;

import com.badlogic.gdx.utils.JsonWriter;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.generation.model.BlockModelGenerator;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.ISidedModelLoader;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import finalforeach.cosmicreach.rendering.blockmodels.BlockModel;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.model.vanilla.BasicBlockModel;
import me.zombii.beryllium.common.BerylliumCommon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NewClientModelLoader implements ISidedModelLoader {

    public static final float[] DEFAULT_ROTATION = new float[3];

    public static final Map<String, BlockModel> CACHE = new ConcurrentHashMap<>();


    static {
        BerylliumCommon.SETTINGS.outputType = JsonWriter.OutputType.json;
    }

    private static String fixGdxJson(String json) {
        return BerylliumCommon.READER.parse(json).prettyPrint(BerylliumCommon.SETTINGS);
    }

    @Override
    public boolean hasModel(String modelName, float[] rotation) {
        return BerylliumModelLoader.isRegistered(modelName);
    }

    @Override
    public void loadModel(BlockModelGenerator modelGenerator, boolean coverAllRotations) {
        loadModel(modelGenerator, coverAllRotations, false);
    }

    private final float[] rotTemp = new float[3];

    @Override
    public void loadModel(BlockModelGenerator modelGenerator, boolean coverAllRotations, boolean override) {
        String modelName = modelGenerator.getName();

        if (!override && CACHE.containsKey(modelName)) return;
        if (override) BerylliumModelLoader.unregister(modelName);

        String modelJson = modelGenerator.toJson().toString();
        BerylliumModelLoader.loadVanillaBlockModel(modelName, modelJson);

        if (coverAllRotations) {
            for (int x = 0; x < 360; x += 90) {
                for (int y = 0; y < 360; y += 90) {
                    for (int z = 0; z < 360; z += 90) {
                        this.rotTemp[0] = x;
                        this.rotTemp[1] = y;
                        this.rotTemp[2] = z;
                        CACHE.put(modelName, fromString(modelName, rotTemp, modelJson, override));
                    }
                }
            }
        } else {
            CACHE.put(modelName, fromString(modelName, DEFAULT_ROTATION, modelJson, override));
        }
    }

    @Override
    public BlockModel loadModel(BlockModelGenerator modelGenerator, float[] rotation) {
        return loadModel(modelGenerator, rotation, false);
    }

    @Override
    public BlockModel loadModel(BlockModelGenerator modelGenerator, float[] rotation, boolean override) {
        String modelName = modelGenerator.getName();

        if (!override && CACHE.containsKey(modelName)) return CACHE.get(modelName);
        if (override) BerylliumModelLoader.unregister(modelName);

        String modelJson = modelGenerator.toJson().toString();
        BerylliumModelLoader.loadVanillaBlockModel(modelName, modelJson);
        BlockModel model = fromString(modelName, rotation, modelJson, override);
        CACHE.put(modelName, model);
        return model;
    }

    @Override
    public BlockModel loadModel(String modelName, float[] rotation, boolean override) {
        if (!override && CACHE.containsKey(modelName)) return CACHE.get(modelName);
        if (override) BerylliumModelLoader.unregister(modelName);

        if (!modelName.contains(".json")) {

            String filePath = modelName + ".json";
            RawAssetLoader.RawFileHandle modelFileHandle = IndependentAssetLoader.loadAsset(Identifier.of(filePath));
            BerylliumModelLoader.loadBerylliumModel(filePath, modelFileHandle);

            //TODO make this not just a cube ( help needed)
            RawAssetLoader.RawFileHandle fileHandle = IndependentAssetLoader.loadAsset(Identifier.of("base:models/blocks/cube.json"));
            BlockModel model = fromString("beryllium:cube", new float[]{0.0F, 0.0F, 0.0F}, fileHandle.getString(), override);
            CACHE.put(modelName, model);
            return model;
        }

        String json = IndependentAssetLoader.loadAsset(Identifier.of(modelName)).getString();

        BerylliumModelLoader.loadVanillaBlockModel(modelName, json);
        BlockModel model = fromString(modelName, rotation, json, override);
        CACHE.put(modelName, model);
        return model;
    }

    @Override
    public BlockModel loadModel(String modelName, String modelJson, float[] rotation, boolean override) {
        if (!override && CACHE.containsKey(modelName)) return CACHE.get(modelName);
        if (override) BerylliumModelLoader.unregister(modelName);

        String json = fixGdxJson(modelJson);
        BerylliumModelLoader.loadVanillaBlockModel(modelName, json);
        BlockModel model = fromString(modelName, rotation, json, override);
        CACHE.put(modelName, model);
        return model;
    }

    private static BlockModel fromString(String modelName, float[] rotation, String modelJson, boolean override) {
//        try {
//            return (BlockModel) ReflectionUtil.getMethod(DummyBlockModel.class, "getInstanceFromJsonStr", new Class[]{String.class, String.class, float[].class})
//                    .invoke(null, modelName, modelJson, DEFAULT_ROTATION);
//        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//        try {
//            BlockModelJson model = (BlockModelJson) ReflectionUtil.getMethod(BlockModelJson.class, "fromJson", new Class[]{String.class, int.class, int.class, int.class})
//                    .invoke(null, modelJson, 0, 0, 0);
//            return model;
//        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
        return BasicBlockModel.fromJson(modelName, rotation, modelJson, override);
    }
}
