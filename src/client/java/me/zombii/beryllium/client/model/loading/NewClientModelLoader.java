package me.zombii.beryllium.client.model.loading;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.generation.model.BlockModelGenerator;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.ISidedModelLoader;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import dev.puzzleshq.puzzleloader.loader.util.ReflectionUtil;
import finalforeach.cosmicreach.rendering.blockmodels.BlockModel;
import finalforeach.cosmicreach.rendering.blockmodels.BlockModelJson;
import finalforeach.cosmicreach.util.Identifier;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NewClientModelLoader implements ISidedModelLoader {

    public static final Json JSON = new Json();
    private static final JsonReader READER = new JsonReader();
    public static final JsonValue.PrettyPrintSettings SETTINGS = new JsonValue.PrettyPrintSettings();
    public static final float[] DEFAULT_ROTATION = new float[3];

    public static final Map<String, BlockModel> CACHE = new ConcurrentHashMap<>();


    static {
        SETTINGS.outputType = JsonWriter.OutputType.json;
    }

    private static String fixGdxJson(String json) {
        return READER.parse(json).prettyPrint(SETTINGS);
    }

    @Override
    public boolean hasModel(String modelName, float[] rotation) {
        return BerylliumModelLoader.isRegistered(modelName);
    }

    @Override
    public void loadModel(BlockModelGenerator modelGenerator, boolean coverAllRotations) {
        loadModel(modelGenerator, coverAllRotations, false);
    }

    @Override
    public void loadModel(BlockModelGenerator modelGenerator, boolean coverAllRotations, boolean override) {
        if (!override && CACHE.containsKey(modelGenerator.getName())) return;
        if (override) BerylliumModelLoader.unregister(modelGenerator.getName());

        String modelName = modelGenerator.getName();
        String modelJson = modelGenerator.toJson().toString();
        BerylliumModelLoader.loadVanillaBlockModel(modelName, modelJson);
        CACHE.put(modelName, fromString(modelName, modelJson));
    }

    @Override
    public BlockModel loadModel(BlockModelGenerator modelGenerator, float[] rotation) {
        return loadModel(modelGenerator, rotation, false);
    }

    @Override
    public BlockModel loadModel(BlockModelGenerator modelGenerator, float[] rotation, boolean override) {
        if (!override && CACHE.containsKey(modelGenerator.getName())) return CACHE.get(modelGenerator.getName());
        if (override) BerylliumModelLoader.unregister(modelGenerator.getName());

        String modelName = modelGenerator.getName();
        String modelJson = modelGenerator.toJson().toString();
        BerylliumModelLoader.loadVanillaBlockModel(modelName, modelJson);
        BlockModel model = fromString(modelName, modelJson);
        CACHE.put(modelName, model);
        return model;
    }

    @Override
    public BlockModel loadModel(String modelName, float[] rotation, boolean override) {
        if (!override && CACHE.containsKey(modelName)) return CACHE.get(modelName);
        if (override) BerylliumModelLoader.unregister(modelName);

        String json = IndependentAssetLoader.loadAsset(Identifier.of(modelName)).getString();

        BerylliumModelLoader.loadVanillaBlockModel(modelName, json);
        BlockModel model = fromString(modelName, json);
        CACHE.put(modelName, model);
        return model;
    }

    @Override
    public BlockModel loadModel(String modelName, String modelJson, float[] rotation, boolean override) {
        if (!override && CACHE.containsKey(modelName)) return CACHE.get(modelName);
        if (override) BerylliumModelLoader.unregister(modelName);

        String json = fixGdxJson(modelJson);
        BerylliumModelLoader.loadVanillaBlockModel(modelName, json);
        BlockModel model = fromString(modelName, json);
        CACHE.put(modelName, model);
        return model;
    }

    private static BlockModel fromString(String modelName, String modelJson) {
//        try {
//            return (BlockModel) ReflectionUtil.getMethod(DummyBlockModel.class, "getInstanceFromJsonStr", new Class[]{String.class, String.class, float[].class})
//                    .invoke(null, modelName, modelJson, DEFAULT_ROTATION);
//        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
        try {
            BlockModelJson model = (BlockModelJson) ReflectionUtil.getMethod(BlockModelJson.class, "fromJson", new Class[]{String.class, int.class, int.class, int.class})
                    .invoke(null, modelJson, 0, 0, 0);
            return model;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
