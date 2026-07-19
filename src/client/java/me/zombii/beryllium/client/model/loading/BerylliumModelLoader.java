package me.zombii.beryllium.client.model.loading;

import com.badlogic.gdx.math.Vector3;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.util.constants.Direction;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.exceptions.*;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.parts.CubePart;
import me.zombii.beryllium.client.model.parts.PartFace;
import me.zombii.beryllium.client.model.parts.PartGroup;
import me.zombii.beryllium.client.model.parts.TextureEntry;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.*;

public class BerylliumModelLoader {

    protected static final Logger LOGGER = LogManager.getLogger("Beryllium | ModelLoader");

    protected static final ObjectList<BerylliumModel> loadedModels = new ObjectArrayList<>();
    protected static final Object2ObjectMap<String, BerylliumModel> modelMap = new Object2ObjectArrayMap<>();

    public static BerylliumModel getModel(String name) {
        return modelMap.get(name);
    }

    public static boolean isRegistered(String name) {
        return modelMap.containsKey(name);
    }

    public static boolean isRegistered(BerylliumModel model) {
        return modelMap.containsKey(model.getName()) && modelMap.get(model.getName()) == model;
    }

    public static List<BerylliumModel> getModels() {
        return ObjectLists.unmodifiable(loadedModels);
    }

    public static Map<String, BerylliumModel> getModelMap() {
        return Object2ObjectMaps.unmodifiable(modelMap);
    }

    public static BerylliumModel loadBerylliumModel(String filePathId, RawAssetLoader.RawFileHandle handle) {
        String json = handle.getString();
        return loadBerylliumModel(filePathId, json);
    }

    public static BerylliumModel loadBerylliumModel(String filePathId, String json) {
        boolean debugMode = BerylliumConfig.INSTANCE.debugMode;

        JsonValue value = JsonValue.readHjson(json);
        if (!value.isObject()) throw new ModelException(filePathId, "Expected a json object as input, got type \"" + value.getType() + "\" instead");
        JsonObject object = value.asObject();

        if (object.get("id") == null) throw new MissingJsonFieldException(filePathId, "string", "id");
        if (!object.get("id").isString()) throw new InvalidJsonTypeException(filePathId, "id", "string", object.get("id").getType().name());
        String id = object.get("id").asString();

        if (modelMap.containsKey(id)) {
            return modelMap.get(id);
        }

        BerylliumModel parentModel = null;
        if (object.get("parent-id") != null) {
            if (!object.get("parent-id").isString()) throw new InvalidJsonTypeException(filePathId, "parent-id", "string", object.get("parent-id").getType().name());
            String parentId = object.get("parent-id").asString();
            String parentPath = parentId + ".json";

            parentModel = BerylliumModelLoader.getModel(parentId);
            if (parentModel == null) {
                parentModel = BerylliumModelLoader.loadBerylliumModel(
                        parentId, IndependentAssetLoader.loadAsset(Identifier.of(parentPath))
                );
            }
            if (parentModel == null) throw new ModelException(filePathId, "Could not find the parent model \"" + parentId + "\"");
        }

        BerylliumModel model = new BerylliumModel(id);

        loadTextures(filePathId, object, model, parentModel);

        if (object.get("groups") != null) {
            if (!object.get("groups").isObject())
                throw new InvalidJsonTypeException(filePathId, "groups", "Object", object.get("groups").getType().name());
            JsonObject groups = object.get("groups").asObject();

            for (JsonObject.Member group : groups) {
                PartGroup partGroup = model.getOrCreateGroup(group.getName());
                JsonValue groupValue = group.getValue();
                if (!groupValue.isObject())
                    throw new InvalidJsonTypeException(filePathId, group.getName(), "Object", groupValue.getType().name());
                JsonObject groupObject = groupValue.asObject();

                JsonValue parentNameValue = groupObject.get("parentName");
                if (parentNameValue != null) {
                    if (!parentNameValue.isString())
                        throw new InvalidJsonTypeException(filePathId, "parentName", "string", parentNameValue.getType().name());
                    partGroup.setParentName(parentNameValue.asString());
                }

                partGroup.setRotation(getJsonVector3(filePathId, groupObject, "rotation", Vector3.Zero));
                partGroup.setPivot(getJsonVector3(filePathId, groupObject, "pivot", Vector3.Zero));

                JsonValue parts = groupObject.get("parts");
                if (!parts.isArray())
                    throw new InvalidJsonTypeException(filePathId, "parts", "Array", parts.getType().name());
                JsonArray array = parts.asArray();

                for (JsonValue jsonValue : array.values()) {
                    if (!jsonValue.isObject())
                        throw new InvalidJsonArrayTypeException(filePathId, "parts", "Object", jsonValue.getType().name());
                    JsonObject partObject = jsonValue.asObject();

                    Vector3 pos = getJsonVector3(filePathId, partObject, "pos", null);
                    if (pos == null) throw new MissingJsonFieldException(filePathId, "array", "pos");

                    Vector3 size = getJsonVector3(filePathId, partObject, "size", null);
                    if (size == null) throw new MissingJsonFieldException(filePathId, "array", "size");

                    CubePart part = partGroup.newPart(pos, size);

                    part.setPivot(getJsonVector3(filePathId, partObject, "pivot", Vector3.Zero));
                    part.setRotation(getJsonVector3(filePathId, partObject, "rotation", Vector3.Zero));
                    part.setCanCollide(partObject.getBoolean("canCollide", true));

                    if (partObject.get("faces") == null) throw new MissingJsonObjectException(filePathId, "faces");
                    if (!partObject.get("faces").isObject())
                        throw new InvalidJsonTypeException(filePathId, "faces", "Object", partObject.get("faces").getType().name());
                    JsonObject faces = partObject.get("faces").asObject();

                    PartFace[] partFaces = part.getFaces();
                    Arrays.fill(partFaces, null);

                    for (JsonObject.Member member : faces) {
                        JsonValue faceValue = member.getValue();
                        if (!faceValue.isObject())
                            throw new InvalidJsonTypeException(filePathId, group.getName(), "Object", faceValue.getType().name());
                        JsonObject faceObject = faceValue.asObject();

                        Direction direction = switch (member.getName()) {
                            case "NegX" -> Direction.NEG_X;
                            case "PosX" -> Direction.POS_X;
                            case "NegY" -> Direction.NEG_Y;
                            case "PosY" -> Direction.POS_Y;
                            case "NegZ" -> Direction.NEG_Z;
                            case "PosZ" -> Direction.POS_Z;
                            default ->
                                    throw new ModelException(filePathId, "Unexpected face direction \"" + member.getName() + "\"");
                        };

                        PartFace partFace = partFaces[direction.ordinal()] = new PartFace(direction);

                        String texture = getJsonString(filePathId, faceObject, "texture", null);
                        if (texture == null) throw new MissingJsonFieldException(filePathId, "string", "texture");
                        partFace.setTextureID(texture);

                        int uvRotation = faceObject.getInt("uvRotation", 0);
                        partFace.setUVRotation(uvRotation);

                        partFace.setTintIndex(getJsonInt(filePathId, faceObject, "tintIndex", partFace.getTintIndex()));

                        partFace.setAO(getJsonBoolean(filePathId, faceObject, "ambientOcclusion", partFace.usesAO()));

                        partFace.setCulled(getJsonBoolean(filePathId, faceObject, "cullFace", partFace.isCulled()));

                        JsonValue uvValue = faceObject.get("uv");
                        if (uvValue == null) throw new MissingJsonFieldException(filePathId, "array", "uv");
                        ;

                        if (!uvValue.isArray())
                            throw new InvalidJsonTypeException(filePathId, "uv", "Array", uvValue.getType().name());
                        JsonArray uvArray = uvValue.asArray();

                        if (uvArray.size() != 4)
                            throw new InvalidJsonArraySizeException(filePathId, "uv", 4, uvArray.size());

                        JsonValue one = uvArray.get(0);
                        if (!one.isNumber())
                            throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", one.getType().name());
                        JsonValue two = uvArray.get(1);
                        if (!two.isNumber())
                            throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", two.getType().name());
                        JsonValue three = uvArray.get(2);
                        if (!three.isNumber())
                            throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", three.getType().name());
                        JsonValue four = uvArray.get(2);
                        if (!four.isNumber())
                            throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", four.getType().name());

                        float[] uvs = partFace.getUV();
                        uvs[0] = one.asFloat();
                        uvs[1] = two.asFloat();
                        uvs[2] = three.asFloat();
                        uvs[3] = four.asFloat();

                    }
                }

            }
        }

        if (parentModel != null) {
            for (Map.Entry<String, PartGroup> entry : parentModel.getGroupMap().entrySet()) {
                if (model.getGroupMap().containsKey(entry.getKey())) {
                    continue;
                }

                PartGroup parentPartGroup = entry.getValue();

                PartGroup partGroup = model.getOrCreateGroup(entry.getKey());
                partGroup.setRotation(parentPartGroup.getRotation());
                partGroup.setPivot(parentPartGroup.getPivot());
                parentPartGroup.setParentName(parentPartGroup.getParentName());

                for (CubePart part : parentPartGroup) {
                    CubePart newPart = partGroup.newPart(part.getPos(), part.getSize())
                            .setPivot(part.getPivot())
                            .setRotation(part.getRotation())
                            .setInflate(part.getInflate())
                            .setCanCollide(part.canCollide());

                    PartFace[] oldFaces = part.getFaces();
                    PartFace[] newFaces = newPart.getFaces();
                    for (int i = 0; i < 6; i++) {
                        if (oldFaces[i] == null) {
                            newFaces[i] = null;
                            continue;
                        }
                        PartFace oldFace = oldFaces[i];
                        PartFace newFace = new PartFace(oldFace);
                        newFaces[i] = newFace;
                    }
                }

            }
        }

        if (model.getGroup("root") == null) throw new ModelException(filePathId, "root group required");


        if (object.get("attributes") != null) {
            if (!object.get("attributes").isObject()) throw new InvalidJsonTypeException(filePathId, "attributes", "Object", object.get("attributes").getType().name());
            JsonObject attributes = object.get("attributes").asObject();

            model.setTransparent(getJsonBoolean(filePathId, attributes, "isTransparent", false));

            String renderLayerId = getJsonString(filePathId, attributes, "renderLayerId", null);
            model.setRenderLayer(renderLayerId != null ? Identifier.of(renderLayerId) : null);
        }

        if (debugMode) LOGGER.log(Level.INFO, "Loading Beryllium Block Model \"{}\"", filePathId);
        return register(model);

    }

    public static int getJsonInt(String filePathId, JsonObject object, String name, int defaultValue) {
        JsonValue tintIndexValue = object.get(name);
        if (tintIndexValue != null && !tintIndexValue.isNumber()) {
            throw new InvalidJsonTypeException(filePathId, name, "int", tintIndexValue.getType().name());
        }
        return tintIndexValue != null ? tintIndexValue.asInt() : defaultValue;
    }

    public static boolean getJsonBoolean(String filePathId, JsonObject object, String name, boolean defaultValue) {
        JsonValue tintIndexValue = object.get(name);
        if (tintIndexValue != null && !tintIndexValue.isBoolean()) {
            throw new InvalidJsonTypeException(filePathId, name, "boolean", tintIndexValue.getType().name());
        }
        return tintIndexValue != null ? tintIndexValue.asBoolean() : defaultValue;
    }

    public static String getJsonString(String filePathId, JsonObject object, String name, String defaultValue) {
        JsonValue tintIndexValue = object.get(name);
        if (tintIndexValue != null && !tintIndexValue.isString()) {
            throw new InvalidJsonTypeException(filePathId, name, "string", tintIndexValue.getType().name());
        }
        return tintIndexValue != null ? tintIndexValue.asString() : defaultValue;
    }

    public static Vector3 getJsonVector3(String filePathId, JsonObject object, String name, Vector3 defaultValue) {
        JsonValue value = object.get(name);
        if (value == null) return defaultValue;

        if (!value.isArray()) throw new InvalidJsonTypeException(filePathId, name, "Array", value.getType().name());
        JsonArray array = value.asArray();

        if (array.size() != 3) throw new InvalidJsonArraySizeException(filePathId, name, 3, array.size());

        JsonValue x = array.get(0);
        if (!x.isNumber()) throw new InvalidJsonArrayTypeException(filePathId, name, "number", x.getType().name());
        JsonValue y = array.get(1);
        if (!y.isNumber()) throw new InvalidJsonArrayTypeException(filePathId, name, "number", y.getType().name());
        JsonValue z = array.get(2);
        if (!z.isNumber()) throw new InvalidJsonArrayTypeException(filePathId, name, "number", z.getType().name());

        Vector3 vector3 = new Vector3();

        vector3.set(x.asFloat(), y.asFloat(), z.asFloat());

        return vector3;
    }

    public static void loadTextures(String filePathId, JsonObject object, BerylliumModel model, BerylliumModel parentModel) {
        if (object.get("textures") == null) throw new MissingJsonObjectException(filePathId, "textures");
        if (!object.get("textures").isObject()) throw new InvalidJsonTypeException(filePathId, "textures", "Object", object.get("textures").getType().name());
        JsonObject textures = object.get("textures").asObject();

        for (JsonObject.Member texture : textures) {
            TextureEntry entry = model.createTexture(texture.getName());
            JsonValue textureValue = texture.getValue();

            if (textureValue.isString()){
                entry.setAlbedoTexturePath(Identifier.of(textureValue.asString()))
                        .setEmissiveTexturePath(null).setAoMapTexturePath(null).setNormalMapTexturePath(null)
                        .setRoughnessMapTexturePath(null).setMetalnessMapTexturePath(null).setDepthMapTexturePath(null);
                continue;
            }

            if (!textureValue.isObject()) throw new InvalidJsonTypeException(filePathId, texture.getName(), "Object or String", textureValue.getType().name());

            JsonObject textureObject = textureValue.asObject();

            String albedoTexture = textureObject.getString("texture", null);
            if (albedoTexture != null) {
                entry.setAlbedoTexturePath(Identifier.of(albedoTexture));
            }
            String emissiveTexture = textureObject.getString("emissive", null);
            if (emissiveTexture != null) {
                entry.setEmissiveTexturePath(Identifier.of(emissiveTexture));
            }
            String aoMapTexture = textureObject.getString("ao-map", null);
            if (aoMapTexture != null) {
                entry.setAoMapTexturePath(Identifier.of(aoMapTexture));
            }
            String normalTexture = textureObject.getString("normal-map", null);
            if (normalTexture != null) {
                entry.setNormalMapTexturePath(Identifier.of(normalTexture));
            }
            String roughnessMapTexture = textureObject.getString("roughness-map", null);
            if (roughnessMapTexture != null) {
                entry.setRoughnessMapTexturePath(Identifier.of(roughnessMapTexture));
            }
            String metalnessMapTexture = textureObject.getString("metalness-map", null);
            if (metalnessMapTexture != null) {
                entry.setMetalnessMapTexturePath(Identifier.of(metalnessMapTexture));
            }
            String depthMapTexture = textureObject.getString("depth-map", null);
            if (depthMapTexture != null) {
                entry.setDepthMapTexturePath(Identifier.of(depthMapTexture));
            }

        }

        if (parentModel != null) {
            for (Map.Entry<String, TextureEntry> entry : parentModel.getTextureMap().entrySet()) {
                if (model.getTextureMap().containsKey(entry.getKey())) {
                    continue;
                }
                TextureEntry textureEntry = entry.getValue();
                TextureEntry newEntry = model.createTexture(entry.getKey());

                newEntry.setAlbedoTexturePath(textureEntry.getAlbedoTexturePath());
                newEntry.setEmissiveTexturePath(textureEntry.getEmissiveTexturePath());
                newEntry.setAoMapTexturePath(textureEntry.getAoMapTexturePath());
                newEntry.setNormalMapTexturePath(textureEntry.getNormalMapTexturePath());
                newEntry.setMetalnessMapTexturePath(textureEntry.getMetalnessMapTexturePath());
                newEntry.setRoughnessMapTexturePath(textureEntry.getRoughnessMapTexturePath());
                newEntry.setDepthMapTexturePath(textureEntry.getDepthMapTexturePath());

            }
        }
    }

    public static BerylliumModel register(BerylliumModel model) {
        return register(model, false);
    }

    public static BerylliumModel register(BerylliumModel newModel, boolean overwrite) {
        boolean debugMode = BerylliumConfig.INSTANCE.debugMode;

        int partCount = 0;
        if (debugMode) {
            for (PartGroup partGroup : newModel) {
                partCount += partGroup.getParts().size();
            }
        }

        String modelName = newModel.getName();
        if (modelMap.containsKey(modelName)) {
            if (!overwrite) throw new ModelException(newModel, "Tried to re-register a model that was already loaded!");

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

    public static void unregister(String modelID) {
        if (isRegistered(modelID)) {
            BerylliumModel model = modelMap.get(modelID);
            loadedModels.remove(model);
            modelMap.remove(modelID, model);
        }
    }

}
