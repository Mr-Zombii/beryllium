package me.zombii.beryllium.client.rendering.model.loading;

import com.badlogic.gdx.files.FileHandle;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.util.constants.Direction;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.rendering.model.*;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BerylliumModelLoader {

    private static final Logger LOGGER = LogManager.getLogger("Beryllium | ModelLoader");

    private static final ObjectList<BerylliumModel> loadedModels = new ObjectArrayList<>();
    private static final Object2ObjectMap<Identifier, BerylliumModel> modelMap = new Object2ObjectArrayMap<>();

    public static BerylliumModel getModel(Identifier name) {
        return modelMap.get(name);
    }

    public static boolean isRegistered(Identifier name) {
        return modelMap.containsKey(name);
    }

    public static boolean isRegistered(BerylliumModel model) {
        return modelMap.containsKey(model.getID()) && modelMap.get(model.getID()) == model;
    }

    public static List<BerylliumModel> getModels() {
        return ObjectLists.unmodifiable(loadedModels);
    }

    public static Map<Identifier, BerylliumModel> getModelMap() {
        return Object2ObjectMaps.unmodifiable(modelMap);
    }

    public static BerylliumModel loadVanillaBlockModel(Identifier modelID, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = fis.readAllBytes();
        fis.close();

        String json = new String(bytes);
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(Identifier modelID, FileHandle handle) throws IOException {
        String json = handle.readString();
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(Identifier modelID) {
        String json = IndependentAssetLoader.loadAsset(modelID).getString();
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(Identifier modelID, RawAssetLoader.RawFileHandle handle) throws IOException {
        String json = handle.getString();
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(Identifier name, String json) {
        if (modelMap.containsKey(name)) {
            return modelMap.get(name);
        }

        boolean debugMode = BerylliumConfig.getOrLoad().debugMode;

        JsonValue value = JsonValue.readHjson(json);
        if (!value.isObject()) throw new IllegalArgumentException("Expected a json object as input, got a \"" + value.getType() + "\" for model \"" + name + "\"");
        BerylliumModel model = new BerylliumModel(name);

        JsonObject object = value.asObject();
        if (object.isEmpty()) {
            if (debugMode) LOGGER.log(Level.INFO, "Loading Empty Vanilla Block Model \"{}\"", name);
            return register(model);
        }

        String parentModelName = object.getString("parent", null);
        BerylliumModel foundParentModel = null;
        if (parentModelName != null) {
            BerylliumModel parentModel = BerylliumModelLoader.getModel(Identifier.of(parentModelName.trim()));
            if (parentModel == null) {
                try {
                    parentModel = BerylliumModelLoader.loadVanillaBlockModel(Identifier.of(parentModelName.trim()), IndependentAssetLoader.loadAsset(Identifier.of(parentModelName.trim())));
                } catch (IOException ignore) {}
            }
            if (parentModel == null) throw new IllegalStateException("Could not find the parent model \"" + parentModelName + "\" for model \"" + name + "\"");
            foundParentModel = parentModel;
        }

        JsonValue textureValues = object.get("textures");
        if (textureValues != null) {
            if (!textureValues.isObject())
                throw new IllegalArgumentException(
                        "Expected json object expected for the texture dict in model \"" + name + "\", got type \"" + textureValues.getType() + "\""
                );

            JsonObject textures = textureValues.asObject();
            for (JsonObject.Member texture : textures) {
                TextureEntry entry = model.createTexture(texture.getName());
                JsonValue textureValue = texture.getValue();

                if (!textureValue.isObject())
                    throw new IllegalArgumentException(
                            "Expected json object for texture \"" + texture.getName() + "\", got type \"" + textureValue.getType() + "\" in model \"" + name + "\""
                    );

                JsonObject textureObject = textureValue.asObject();

                String albedoTexture = textureObject.getString("fileName", null);
                if (albedoTexture != null) {
                    entry.setAlbedoTexturePath(Identifier.of(albedoTexture));
                }
                String emissiveTexture = textureObject.getString("emissivefileName", null);
                if (emissiveTexture != null) {
                    entry.setEmissiveTexturePath(Identifier.of(emissiveTexture));
                }
                String aoMapTexture = textureObject.getString("aoMapFileName", null);
                if (aoMapTexture != null) {
                    entry.setAoMapTexturePath(Identifier.of(aoMapTexture));
                }
                String normalTexture = textureObject.getString("normalMapFileName", null);
                if (normalTexture != null) {
                    entry.setNormalMapTexturePath(Identifier.of(normalTexture));
                }
                String roughnessMapTexture = textureObject.getString("roughnessMapFileName", null);
                if (roughnessMapTexture != null) {
                    entry.setRoughnessMapTexturePath(Identifier.of(roughnessMapTexture));
                }
                String metalnessMapTexture = textureObject.getString("metalnessMapFileName", null);
                if (metalnessMapTexture != null) {
                    entry.setMetalnessMapTexturePath(Identifier.of(metalnessMapTexture));
                }
                String depthMapTexture = textureObject.getString("depthMapFileName", null);
                if (depthMapTexture != null) {
                    entry.setDepthMapTexturePath(Identifier.of(depthMapTexture));
                }
            }
        } else {
            if (foundParentModel != null) {
                for (TextureEntry entry : foundParentModel.getTextureMap().values()) {
                    TextureEntry newEntry = model.createTexture(entry.getName());
                    newEntry.setAlbedoTexturePath(entry.getAlbedoTexturePath());
                    newEntry.setEmissiveTexturePath(entry.getEmissiveTexturePath());
                    newEntry.setAoMapTexturePath(entry.getAoMapTexturePath());
                    newEntry.setNormalMapTexturePath(entry.getNormalMapTexturePath());
                    newEntry.setMetalnessMapTexturePath(entry.getMetalnessMapTexturePath());
                    newEntry.setRoughnessMapTexturePath(entry.getRoughnessMapTexturePath());
                    newEntry.setDepthMapTexturePath(entry.getDepthMapTexturePath());
                }
            }
        }

        JsonValue cuboidsValue = object.get("cuboids");
        if (cuboidsValue != null) {
            if (!cuboidsValue.isArray())
                throw new IllegalArgumentException("Expected json array the cuboids list in model \"" + name + "\", not type \"" + cuboidsValue.getType() + "\"");

            PartGroup rootGroup = model.getOrCreateGroup("root");

            JsonArray cuboids = cuboidsValue.asArray();
            cuboids.forEach(cuboid -> {
                if (!cuboid.isObject()) throw new IllegalArgumentException("Expected cuboid to be a json object, got type \"" + cuboid.getType() + "\"");
                JsonObject cuboidObject = cuboid.asObject();

                JsonValue localBoundsValue = cuboidObject.get("localBounds");
                if (localBoundsValue == null || !localBoundsValue.isArray())
                    throw new IllegalArgumentException(
                            "Expected local bounds to be a json array not " + (localBoundsValue == null ?
                                    "null" :
                                    " type \"" + localBoundsValue.getType() + "\""
                            ) + " in model \"" + name + "\""
                    );
                JsonArray localBounds = localBoundsValue.asArray();
                if (localBounds.size() != 6) throw new IllegalArgumentException("Expected local bounds to be six numbers in length in model \"" + name + "\"");
                Part part = rootGroup.newPart(
                        localBounds.get(0).asFloat(),
                        localBounds.get(1).asFloat(),
                        localBounds.get(2).asFloat(),
                        localBounds.get(0).asFloat() - localBounds.get(0).asFloat(),
                        localBounds.get(1).asFloat() - localBounds.get(1).asFloat(),
                        localBounds.get(2).asFloat() - localBounds.get(2).asFloat()
                );
                JsonValue faceValues = cuboidObject.get("faces");
                if (faceValues == null) return;
                if (!faceValues.isObject()) throw new IllegalArgumentException(
                        "Expected faces on cuboids to be a json object, not type \"" + faceValues.getType() + "\" in model \"" + name + "\""
                );
                JsonObject facesObject = faceValues.asObject();
                PartFace[] faces = part.getFaces();
                Arrays.fill(faces, null);
                for (JsonObject.Member member : facesObject) {
                    Direction direction = switch (member.getName()) {
                        case "localNegX" -> Direction.NEG_X;
                        case "localPosX" -> Direction.POS_X;
                        case "localNegY" -> Direction.NEG_Y;
                        case "localPosY" -> Direction.POS_Y;
                        case "localNegZ" -> Direction.NEG_Z;
                        case "localPosZ" -> Direction.POS_Z;
                        default -> throw new IllegalArgumentException("Unexpected face direction \"" + member.getName() + "\" in model \"" + name + "\"");
                    };

                    if (!member.getValue().isObject())
                        throw new IllegalArgumentException("Expected face \"" + direction + "\" to be a json object, not type \"" + member.getValue().getType() + "\" in model \"" + name + "\"");

                    JsonObject faceObject = member.getValue().asObject();

                    PartFace face = faces[direction.ordinal()] = new PartFace(direction);
                    face.setCulled(faceObject.getBoolean("cullFace", true));
                    face.setAO(faceObject.getBoolean("ambientocclusion", true));
                    face.setTextureID(faceObject.getString("texture", null));
                    face.setUVRotation(faceObject.getInt("uvRotation", 0));

                    JsonValue uvValues = faceObject.get("uv");
                    if (uvValues == null || !uvValues.isArray()) throw new IllegalArgumentException(
                            "Expected uvs in face \"" + direction + "\" to be a json array, not " + (uvValues == null ? "\"null\"" : ("type \"" + uvValues.getType() + "\"")) + " in model \"" + name + "\""
                    );
                    JsonArray uvsArray = uvValues.asArray();
                    if (uvsArray.size() != 4) throw new IllegalArgumentException(
                            "Expected uv array in face \""  + direction + "\" to be four numbers in length in model \"" + name + "\""
                    );

                    float[] uvs = face.getUV();
                    uvs[0] = uvsArray.get(0).asFloat();
                    uvs[1] = uvsArray.get(1).asFloat();
                    uvs[2] = uvsArray.get(2).asFloat();
                    uvs[3] = uvsArray.get(3).asFloat();
                }
            });
        } else {
            if (foundParentModel != null) {
                for (PartGroup group : foundParentModel) {
                    PartGroup newGroup = model.getOrCreateGroup(group.getName());
                    newGroup.setPivot(group.getPivot());
                    newGroup.setRotation(group.getRotation());
                    newGroup.setParentName(group.getParentName());
                    for (Part part : group) {
                        Part newPart = newGroup.newPart(part.getPos(), part.getSize())
                                .setPivot(part.getPivot())
                                .setRotation(part.getRotation())
                                .setScale(part.getScale());
                        PartFace[] oldFaces = part.getFaces();
                        PartFace[] newFaces = newPart.getFaces();
                        for (int i = 0; i < 6; i++) {
                            if (oldFaces[i] == null) {
                                newFaces[i] = null;
                                continue;
                            }
                            PartFace oldFace = oldFaces[i];
                            PartFace newFace = new PartFace(oldFace.getDirection());
                            newFace.setAO(oldFace.usesAO())
                                    .setCulled(oldFace.isCulled())
                                    .setTextureID(oldFace.getTextureID())
                                    .setTintIndex(oldFace.getTintIndex())
                                    .setUVRotation(oldFace.getUVRotation())
                            ;
                            newFaces[i] = newFace;
                        }
                    }
                }
            }
        }
        if (debugMode) LOGGER.log(Level.INFO, "Loading Vanilla Block Model \"{}\"", name);
        return register(model);
    }

    public static BerylliumModel register(BerylliumModel model) {
        return register(model, false);
    }

    public static BerylliumModel register(BerylliumModel newModel, boolean overwrite) {
        boolean debugMode = BerylliumConfig.getOrLoad().debugMode;

        int partCount = 0;
        if (debugMode) {
            for (PartGroup partGroup : newModel) {
                partCount += partGroup.getParts().size();
            }
        }

        Identifier modelName = newModel.getID();
        if (modelMap.containsKey(modelName)) {
            if (!overwrite) throw new IllegalArgumentException("Tried to re-register a model that was already loaded!");

            BerylliumModel model = modelMap.get(modelName);
            loadedModels.remove(model);
            modelMap.remove(modelName, model);

            if (debugMode)
                LOGGER.log(
                        Level.INFO, "Re-Registering Model \"{}\", {} Texture(s), {} Group(s) {} Part(s)",
                        modelName, newModel.getTextureMap().size(), newModel.getGroups().size(), partCount
                );
        } else {
            if (debugMode)
                LOGGER.log(
                        Level.INFO, "Registering Model \"{}\", {} Texture(s), {} Group(s) {} Part(s)",
                        modelName, newModel.getTextureMap().size(), newModel.getGroups().size(), partCount
                );
        }
        modelMap.put(modelName, newModel);
        loadedModels.add(newModel);
        return newModel;
    }
}
