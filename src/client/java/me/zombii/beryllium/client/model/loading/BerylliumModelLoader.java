package me.zombii.beryllium.client.model.loading;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.util.constants.Direction;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.exceptions.*;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.parts.Part;
import me.zombii.beryllium.client.model.parts.PartFace;
import me.zombii.beryllium.client.model.parts.PartGroup;
import me.zombii.beryllium.client.model.parts.TextureEntry;
import me.zombii.beryllium.common.BerylliumCommon;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class BerylliumModelLoader {

    private static final Logger LOGGER = LogManager.getLogger("Beryllium | ModelLoader");

    private static final ObjectList<BerylliumModel> loadedModels = new ObjectArrayList<>();
    private static final Object2ObjectMap<String, BerylliumModel> modelMap = new Object2ObjectArrayMap<>();

    private static final Object2ObjectMap<String, String> modelIdToPath = new Object2ObjectArrayMap<>();
    public static final List<String> blockIdsToLoad = new ArrayList<>();

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
    public static BerylliumModel loadVanillaBlockModel(String modelID, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = fis.readAllBytes();
        fis.close();

        String json = new String(bytes);
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(String modelID, FileHandle handle) throws IOException {
        String json = handle.readString();
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(String modelID) {
        String json = IndependentAssetLoader.loadAsset(Identifier.of(modelID)).getString();
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(String modelID, RawAssetLoader.RawFileHandle handle) throws IOException {
        String json = handle.getString();
        return loadVanillaBlockModel(modelID, json);
    }

    public static BerylliumModel loadVanillaBlockModel(String name, String json) {
        if (modelMap.containsKey(name)) {
            return modelMap.get(name);
        }

        boolean debugMode = BerylliumConfig.INSTANCE.debugMode;

        JsonValue value = JsonValue.readHjson(json);
        if (!value.isObject()) throw new ModelException(name, "Expected a json object as input, got type \"" + value.getType() + "\" instead");
        BerylliumModel model = new BerylliumModel(name);

        if (name.contains("water"))
            model.setRenderLayer(Identifier.of(BerylliumCommon.NAMESPACE, "translucent-block-render-layer"));

        JsonObject object = value.asObject();
        if (object.isEmpty()) {
            if (debugMode) LOGGER.log(Level.INFO, "Loading Empty Vanilla Block Model \"{}\"", name);
            return register(model);
        }

        String parentModelName = object.getString("parent", null);
        BerylliumModel foundParentModel = null;
        if (parentModelName != null) {
            BerylliumModel parentModel = BerylliumModelLoader.getModel(parentModelName.trim());
            if (parentModel == null) {
                try {
                    parentModel = BerylliumModelLoader.loadVanillaBlockModel(parentModelName.trim(), IndependentAssetLoader.loadAsset(Identifier.of(parentModelName.trim())));
                } catch (IOException ignore) {}
            }
            if (parentModel == null) throw new ModelException(name, "Could not find the parent model \"" + parentModelName + "\"");
            foundParentModel = parentModel;
        }

        JsonValue textureValues = object.get("textures");
        if (textureValues != null) {
            if (!textureValues.isObject())
                throw new ModelException(name,
                        "Expected json object for texture dict, got type \"" + textureValues.getType() + "\" instead"
                );

            JsonObject textures = textureValues.asObject();
            for (JsonObject.Member texture : textures) {
                TextureEntry entry = model.createTexture(texture.getName());
                JsonValue textureValue = texture.getValue();

                if (!textureValue.isObject())
                    throw new ModelException(name,
                            "Expected json object for texture \"" + texture.getName() + "\", got type \"" + textureValue.getType() + "\" instead"
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
                throw new ModelException(name, "Expected cuboids to be a json array, got type \"" + cuboidsValue.getType() + "\" instead");

            PartGroup rootGroup = model.getOrCreateGroup("root");

            JsonArray cuboids = cuboidsValue.asArray();
            cuboids.forEach(cuboid -> {
                if (!cuboid.isObject()) throw new ModelException(name, "Expected cuboid to be a json object, got type \"" + cuboid.getType() + "\" instead");
                JsonObject cuboidObject = cuboid.asObject();

                JsonValue localBoundsValue = cuboidObject.get("localBounds");
                if (localBoundsValue == null || !localBoundsValue.isArray())
                    throw new ModelException(name,
                            "Expected local bounds to be a json array, got " + (localBoundsValue == null ? "null" : "type \"" + localBoundsValue.getType() + "\""
                            ) + "instead"
                    );
                JsonArray localBounds = localBoundsValue.asArray();
                if (localBounds.size() != 6) throw new ModelException(name, "Expected local bounds to be six numbers in length");
                Part part = rootGroup.newPart(
                        localBounds.get(0).asFloat(),
                        localBounds.get(1).asFloat(),
                        localBounds.get(2).asFloat(),
                        localBounds.get(3).asFloat() - localBounds.get(0).asFloat(),
                        localBounds.get(4).asFloat() - localBounds.get(1).asFloat(),
                        localBounds.get(5).asFloat() - localBounds.get(2).asFloat()
                ).setPivot(8, 8, 8);
                part.setScale(cuboidObject.getFloat("inflate", 0));
                JsonValue faceValues = cuboidObject.get("faces");
                if (faceValues == null) return;
                if (!faceValues.isObject()) throw new ModelException(name,
                        "Expected faces on cuboids to be a json object, got type \"" + faceValues.getType() + "\" instead"
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
                        default -> throw new ModelException(name, "Unexpected face direction \"" + member.getName() + "\"");
                    };

                    if (!member.getValue().isObject())
                        throw new ModelException(name, "Expected face \"" + direction + "\" to be a json object, got type \"" + member.getValue().getType() + "\" instead");

                    JsonObject faceObject = member.getValue().asObject();

                    PartFace face = faces[direction.ordinal()] = new PartFace(direction);
                    face.setCulled(faceObject.getBoolean("cullFace", true));
                    face.setAO(faceObject.getBoolean("ambientocclusion", true));
                    face.setTextureID(faceObject.getString("texture", null));
                    face.setUVRotation(faceObject.getInt("uvRotation", 0));

                    JsonValue uvValues = faceObject.get("uv");
                    if (uvValues == null || !uvValues.isArray()) throw new ModelException(name,
                            "Expected uvs in face \"" + direction + "\" to be a json array, got " + (uvValues == null ? "\"null\"" : ("type \"" + uvValues.getType() + "\" instead"))
                    );
                    JsonArray uvsArray = uvValues.asArray();
                    if (uvsArray.size() != 4) throw new ModelException(name,
                            "Expected uv array in face \""  + direction + "\" to be four numbers in length"
                    );

                    // TODO: make UVs dependent on texture size, at least for vanilla models, for parity with vanilla behaviour
                    int[] uvs = face.getUV();
                    uvs[0] = uvsArray.get(0).asInt();
                    uvs[1] = uvsArray.get(1).asInt();
                    uvs[2] = uvsArray.get(2).asInt();
                    uvs[3] = uvsArray.get(3).asInt();
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
                            PartFace newFace = new PartFace(oldFace);
                            newFaces[i] = newFace;
                        }
                    }
                }
            }
        }
        if (object.get("isTransparent") != null) {
            model.setTransparent(object.getBoolean("isTransparent", false));
        } else if (foundParentModel != null) {
            model.setTransparent(foundParentModel.isTransparent());
        }

        JsonValue planesValue = object.get("planes");
        if (planesValue != null){
            if (!planesValue.isArray())
                throw new ModelException(name, "Expected planes to be a json array, got type \"" + planesValue.getType() + "\" instead");

            PartGroup rootGroup = model.getOrCreateGroup("root");
            JsonArray planes = planesValue.asArray();
            planes.forEach(plane -> {
                if (!plane.isObject())
                    throw new ModelException(name, "Expected plane to be a json object, got type \"" + plane.getType() + "\" instead");
                JsonObject planeObject = plane.asObject();

                JsonValue verticesValue = planeObject.get("vertices");
                if (verticesValue == null || !verticesValue.isArray())
                    throw new ModelException(name,
                            "Expected vertices to be a json array, got " + (verticesValue == null ? "null" : "type \"" + verticesValue.getType() + "\""
                            ) + "instead"
                    );

                JsonArray vertices = verticesValue.asArray();
                if (vertices.size() != 12)
                    throw new ModelException(name, "Expected vertices to be twelve numbers in length");


                Vector3 v1 = new Vector3(vertices.get(0).asFloat(), vertices.get(1).asFloat(), vertices.get(2).asFloat());
                Vector3 v2 = new Vector3(vertices.get(3).asFloat(), vertices.get(4).asFloat(), vertices.get(5).asFloat());
                Vector3 v3 = new Vector3(vertices.get(6).asFloat(), vertices.get(7).asFloat(), vertices.get(8).asFloat());
                Vector3 v4 = new Vector3(vertices.get(9).asFloat(), vertices.get(10).asFloat(), vertices.get(11).asFloat());

                Plane checkPlane = new Plane(v1, v2, v4);
                if (checkPlane.distance(v3) > 0.01)
                    throw new ModelException(name, "Plane vertices aren't on a single plane");

                float length = v1.dst(v2);
                float width = v1.dst(v4);
                Vector3 center = v1.cpy().add(v3).scl(0.5f);
                Vector3 partPos = center.cpy();
                partPos.x -= length / 2;
                partPos.y -= width / 2;

                Vector3 xAxis = v2.cpy().sub(v1).nor();
                Vector3 zAxis = v4.cpy().sub(v1).nor();
                Vector3 yAxis = xAxis.cpy().crs(zAxis).scl(-1);
                System.out.println("X axis: " + xAxis + ", Y axis: " + yAxis + ", Z axis:" + zAxis);

                Quaternion rotQuaternion = new Quaternion();
                rotQuaternion.setFromAxes(xAxis.x, xAxis.y, xAxis.z, yAxis.x, yAxis.y, yAxis.z, zAxis.x, zAxis.y, zAxis.z);
                Vector3 eulerAngles = new Vector3();
                eulerAngles.z = rotQuaternion.getAngleAround(Vector3.Z);
                rotQuaternion.mul(new Quaternion(Vector3.Z, -eulerAngles.z));
                eulerAngles.y = rotQuaternion.getAngleAround(Vector3.Y);
                rotQuaternion.mul(new Quaternion(Vector3.Y, -eulerAngles.y));
                eulerAngles.x = rotQuaternion.getAngleAround(Vector3.X);
                System.out.println("Euler angles: " + eulerAngles);

                eulerAngles.x %= 360;
                eulerAngles.y %= 360;
                eulerAngles.z %= 360;
                if (eulerAngles.x > 180){
                    eulerAngles.x %= 180;
                    eulerAngles.x -= 90;
                }
                if (eulerAngles.y > 180){
                    eulerAngles.y %= 180;
                    eulerAngles.y -= 90;
                }
                if (eulerAngles.z > 180){
                    eulerAngles.z %= 180;
                    eulerAngles.z -= 90;
                }
                eulerAngles.scl(-1);
                System.out.println("Corrected Euler angles: " + eulerAngles);

                Part part = rootGroup.newPart(
                        partPos.x,
                        partPos.y,
                        partPos.z,
                        length,
                        width,
                        0
                ).setPivot(center).setRotation(
                        eulerAngles
                );

                PartFace[] faces = part.getFaces();
                Arrays.fill(faces, null);

                JsonValue uvValues = planeObject.get("uv");
                if (uvValues == null || !uvValues.isArray()) throw new ModelException(name,
                        "Expected plane uvs to be a json array, got " + (uvValues == null ? "\"null\"" : ("type \"" + uvValues.getType() + "\" instead"))
                );
                JsonArray uvsArray = uvValues.asArray();
                if (uvsArray.size() != 8) throw new ModelException(name,
                        "Expected plane uv to be eight numbers in length"
                );

                PartFace topFace = faces[Direction.POS_Z.ordinal()] = new PartFace(Direction.POS_Z);
                topFace.setCulled(planeObject.getBoolean("cullFace", false));
                topFace.setAO(false);
                topFace.setTextureID(planeObject.getString("texture", null));
                topFace.setUVRotation(planeObject.getInt("uvRotation", 0));

                PartFace bottomFace = faces[Direction.NEG_Z.ordinal()] = new PartFace(Direction.NEG_Z);
                bottomFace.setCulled(planeObject.getBoolean("cullFace", false));
                bottomFace.setAO(false);
                bottomFace.setTextureID(planeObject.getString("texture", null));
                bottomFace.setUVRotation(360 - planeObject.getInt("uvRotation", 0));

                // TODO: make UVs dependent on texture size, at least for vanilla models, for parity with vanilla behaviour
                int[] topUvs = topFace.getUV();
                topUvs[0] = uvsArray.get(0).asInt();
                topUvs[1] = uvsArray.get(1).asInt();
                topUvs[2] = uvsArray.get(4).asInt();
                topUvs[3] = uvsArray.get(5).asInt();

                int[] bottomUvs = bottomFace.getUV();
                bottomUvs[0] = uvsArray.get(2).asInt();
                bottomUvs[1] = uvsArray.get(3).asInt();
                bottomUvs[2] = uvsArray.get(6).asInt();
                bottomUvs[3] = uvsArray.get(7).asInt();
            });
        }

        if (debugMode) LOGGER.log(Level.INFO, "Loading Vanilla Block Model \"{}\"", name);
        return register(model);
    }

    public static @Nullable String registerBerylliumBlockModelID(String filePathId, String json) {
        JsonValue value = JsonValue.readHjson(json);
        if (!value.isObject()) throw new ModelException(filePathId, "Expected a json object as input, got type \"" + value.getType() + "\" instead");
        JsonObject object = value.asObject();

        if (object.get("id") == null) return null;
        if (!object.get("id").isString()) throw new InvalidJsonTypeException(filePathId, "id", "string", object.get("id").getType().name());
        String id = object.get("id").asString();

        if (BerylliumConfig.INSTANCE.debugMode) LOGGER.log(Level.INFO, "Registered Block Model ID \"{}\"", id);

        if (!modelIdToPath.containsKey(id)){
            modelIdToPath.put(id, filePathId);
        }
        return id;
    }

    public static void addToLoadingList(String modelID) {
        if (BerylliumConfig.INSTANCE.debugMode) LOGGER.log(Level.INFO, "Added Block Model ID to loading list \"{}\"", modelID);
        blockIdsToLoad.add(modelID);
    }

    public static BerylliumModel loadBerylliumBlockModel(String modelID, RawAssetLoader.RawFileHandle handle) {
        String json = handle.getString();
        return loadBerylliumBlockModel(modelID, json);
    }


    public static BerylliumModel loadBerylliumBlockModel(String filePathId, String json) {
        boolean debugMode = BerylliumConfig.INSTANCE.debugMode;

        JsonValue value = JsonValue.readHjson(json);
        if (!value.isObject()) throw new ModelException(filePathId, "Expected a json object as input, got type \"" + value.getType() + "\" instead");
        JsonObject object = value.asObject();

        if (object.get("id") == null) throw new MissingJsonFieldException(filePathId, "string", "id");
        if (!object.get("id").isString()) throw new InvalidJsonTypeException(filePathId, "id", "string", object.get("id").getType().name());
        String id = object.get("id").asString();
        //TODO make better ( what the fuck did i mean???? )
//        if (!modelIdToPath.containsKey(id)) throw new ModelException(filePathId, "Tried to load model before its ID '" + id + "' was registered");

        if (modelMap.containsKey(id)) {
            return modelMap.get(id);
        }

        BerylliumModel parentModel = null;
        if (object.get("parent-id") != null) {
            if (!object.get("parent-id").isString()) throw new InvalidJsonTypeException(filePathId, "parent-id", "string", object.get("parent-id").getType().name());
            String parentId = object.get("parent-id").asString();
            if (!modelIdToPath.containsKey(parentId)) throw new ModelException(filePathId, "Tried to load model before its parent model ID '" + parentId + "' was registered");

            parentModel = BerylliumModelLoader.getModel(parentId);
            if (parentModel == null) {
                parentModel = BerylliumModelLoader.loadBerylliumBlockModel(
                        modelIdToPath.get(parentId), IndependentAssetLoader.loadAsset(Identifier.of(modelIdToPath.get(parentId)))
                );
            }
            if (parentModel == null) throw new ModelException(filePathId, "Could not find the parent model \"" + parentId + "\"");
        }

        BerylliumModel model = new BerylliumModel(id);

        loadTextures(filePathId, object, model, parentModel);

        if (object.get("groups") == null) throw new MissingJsonObjectException(filePathId, "groups");
        if (!object.get("groups").isObject()) throw new InvalidJsonTypeException(filePathId, "groups", "Object", object.get("groups").getType().name());
        JsonObject groups = object.get("groups").asObject();

        for (JsonObject.Member group : groups) {
            PartGroup partGroup = model.getOrCreateGroup(group.getName());
            JsonValue groupValue = group.getValue();
            if (!groupValue.isObject()) throw new InvalidJsonTypeException(filePathId, group.getName(), "Object", groupValue.getType().name());
            JsonObject groupObject = groupValue.asObject();

            JsonValue parentNameValue = groupObject.get("parentName");
            if (parentNameValue != null) {
                if (!parentNameValue.isString()) throw new InvalidJsonTypeException(filePathId, "parentName", "string", parentNameValue.getType().name());
                partGroup.setParentName(parentNameValue.asString());
            }

            partGroup.setRotation(getJsonVector3(filePathId, groupObject, "rotation", Vector3.Zero));
            partGroup.setPivot(getJsonVector3(filePathId, groupObject, "pivot", Vector3.Zero));

            JsonValue parts = groupObject.get("parts");
            if (!parts.isArray()) throw new InvalidJsonTypeException(filePathId, "parts", "Array", parts.getType().name());
            JsonArray array = parts.asArray();

            for (JsonValue jsonValue : array.values()) {
                if (!jsonValue.isObject()) throw new InvalidJsonArrayTypeException(filePathId, "parts", "Object", jsonValue.getType().name());
                JsonObject partObject = jsonValue.asObject();

                Vector3 pos = getJsonVector3(filePathId, partObject, "pos", null);
                if (pos == null) throw new MissingJsonFieldException(filePathId, "array", "pos");

                Vector3 size = getJsonVector3(filePathId, partObject, "size", null);
                if (size == null) throw new MissingJsonFieldException(filePathId, "array", "size");

                Part part = partGroup.newPart(pos, size);

                part.setPivot(getJsonVector3(filePathId, partObject, "pivot", Vector3.Zero));
                part.setRotation(getJsonVector3(filePathId, partObject, "rotation", Vector3.Zero));

                if (partObject.get("faces") == null) throw new MissingJsonObjectException(filePathId, "faces");
                if (!partObject.get("faces").isObject()) throw new InvalidJsonTypeException(filePathId, "faces", "Object", partObject.get("faces").getType().name());
                JsonObject faces = partObject.get("faces").asObject();

                PartFace[] partFaces = part.getFaces();
                Arrays.fill(partFaces, null);

                for (JsonObject.Member member : faces) {
                    JsonValue faceValue = member.getValue();
                    if (!faceValue.isObject()) throw new InvalidJsonTypeException(filePathId, group.getName(), "Object", faceValue.getType().name());
                    JsonObject faceObject = faceValue.asObject();

                    Direction direction = switch (member.getName()) {
                        case "NegX" -> Direction.NEG_X;
                        case "PosX" -> Direction.POS_X;
                        case "NegY" -> Direction.NEG_Y;
                        case "PosY" -> Direction.POS_Y;
                        case "NegZ" -> Direction.NEG_Z;
                        case "PosZ" -> Direction.POS_Z;
                        default -> throw new ModelException(filePathId, "Unexpected face direction \"" + member.getName() + "\"");
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
                    if (uvValue == null) throw new MissingJsonFieldException(filePathId, "array", "uv");;

                    if (!uvValue.isArray()) throw new InvalidJsonTypeException(filePathId, "uv", "Array", uvValue.getType().name());
                    JsonArray uvArray = uvValue.asArray();

                    if (uvArray.size() != 4) throw new InvalidJsonArraySizeException(filePathId, "uv", 4, uvArray.size());

                    JsonValue one = uvArray.get(0);
                    if (!one.isNumber()) throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", one.getType().name());
                    JsonValue two = uvArray.get(1);
                    if (!two.isNumber()) throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", two.getType().name());
                    JsonValue three = uvArray.get(2);
                    if (!three.isNumber()) throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", three.getType().name());
                    JsonValue four = uvArray.get(2);
                    if (!four.isNumber()) throw new InvalidJsonArrayTypeException(filePathId, "uv", "number", four.getType().name());

                    int[] uvs = partFace.getUV();
                    uvs[0] = one.asInt();
                    uvs[1] = two.asInt();
                    uvs[2] = three.asInt();
                    uvs[3] = four.asInt();

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

                for (Part part : parentPartGroup) {
                    Part newPart = partGroup.newPart(part.getPos(), part.getSize())
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
