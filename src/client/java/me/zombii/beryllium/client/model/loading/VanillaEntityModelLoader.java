package me.zombii.beryllium.client.model.loading;

import com.badlogic.gdx.files.FileHandle;
import dev.puzzleshq.annotation.stability.Experimental;
import dev.puzzleshq.annotation.stability.Unstable;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import dev.puzzleshq.puzzleloader.loader.util.RawAssetLoader;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.util.constants.Direction;
import me.zombii.beryllium.client.exceptions.ModelException;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.parts.CubePart;
import me.zombii.beryllium.client.model.parts.PartFace;
import me.zombii.beryllium.client.model.parts.PartGroup;
import me.zombii.beryllium.client.model.parts.TextureEntry;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

@Unstable
@Experimental
public class VanillaEntityModelLoader extends BerylliumModelLoader {

    public static BerylliumModel loadVanillaEntityModel(String modelID, File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] bytes = fis.readAllBytes();
        fis.close();

        String json = new String(bytes);
        return loadVanillaEntityModel(modelID, json);
    }

    public static BerylliumModel loadVanillaEntityModel(String modelID, FileHandle handle) throws IOException {
        String json = handle.readString();
        return loadVanillaEntityModel(modelID, json);
    }

    public static BerylliumModel loadVanillaEntityModel(String modelID) {
        String json = IndependentAssetLoader.loadAsset(Identifier.of(modelID)).getString();
        return loadVanillaEntityModel(modelID, json);
    }

    public static BerylliumModel loadVanillaEntityModel(String modelID, RawAssetLoader.RawFileHandle handle) throws IOException {
        String json = handle.getString();
        return loadVanillaEntityModel(modelID, json);
    }

    public static BerylliumModel loadVanillaEntityModel(String name, String json) {
        if (modelMap.containsKey(name)) {
            return modelMap.get(name);
        }

        boolean debugMode = BerylliumConfig.INSTANCE.debugMode;

        JsonValue value = JsonValue.readHjson(json);
        if (!value.isObject()) throw new ModelException(name, "Expected a json object as input, got type \"" + value.getType() + "\" instead");
        BerylliumModel model = new BerylliumModel(name);

        JsonObject object = value.asObject();
        if (object.isEmpty()) {
            if (debugMode) LOGGER.log(Level.INFO, "Loading Empty Vanilla Entity Model \"{}\"", name);
            return register(model);
        }

        JsonValue textureValues = object.get("textures");
        if (textureValues != null) {
            if (!textureValues.isObject())
                throw new ModelException(name,
                        "Expected json object for texture dict, got type \"" + textureValues.getType() + "\" instead"
                );

            JsonObject textures = textureValues.asObject();
            TextureEntry entry = model.createTexture("root");

            String albedoTexture = textures.getString("diffuse", null);
            if (albedoTexture != null) {
                entry.setAlbedoTexturePath(Identifier.of(albedoTexture));
            }
            String emissiveTexture = textures.getString("emission", null);
            if (emissiveTexture != null) {
                entry.setEmissiveTexturePath(Identifier.of(emissiveTexture));
            }
            String aoMapTexture = textures.getString("aoMap", null);
            if (aoMapTexture != null) {
                entry.setAoMapTexturePath(Identifier.of(aoMapTexture));
            }
            String normalTexture = textures.getString("normalMap", null);
            if (normalTexture != null) {
                entry.setNormalMapTexturePath(Identifier.of(normalTexture));
            }
            String roughnessMapTexture = textures.getString("roughnessMap", null);
            if (roughnessMapTexture != null) {
                entry.setRoughnessMapTexturePath(Identifier.of(roughnessMapTexture));
            }
            String metalnessMapTexture = textures.getString("metalnessMap", null);
            if (metalnessMapTexture != null) {
                entry.setMetalnessMapTexturePath(Identifier.of(metalnessMapTexture));
            }
            String depthMapTexture = textures.getString("depthMap", null);
            if (depthMapTexture != null) {
                entry.setDepthMapTexturePath(Identifier.of(depthMapTexture));
            }
        }

        JsonValue bonesValue = object.get("bones");
        if (bonesValue != null) {
            if (!bonesValue.isArray())
                throw new ModelException(name, "Expected bones to be a json array, got type \"" + bonesValue.getType() + "\" instead");

            JsonArray bones = bonesValue.asArray();
            for (JsonValue boneValue : bones) {
                if (!boneValue.isArray())
                    throw new ModelException(name, "Expected bone to be a json object, got type \"" + boneValue.getType() + "\" instead");

                JsonObject bone = boneValue.asObject();

                String boneParent = bone.getString("parent", null);
                String boneName = bone.get("name").asString();

                PartGroup group = model.getOrCreateGroup(boneName);
                group.setParentName(boneParent);

                JsonValue pivotValue = bone.get("pivot");
                if (pivotValue != null) {
                    if (!pivotValue.isArray())
                        throw new ModelException(name,
                                "Expected pivot to be a json array, got type \"" + pivotValue.getType() + "\" instead"
                        );
                    JsonArray pivot = pivotValue.asArray();
                    if (pivot.size() != 3) throw new ModelException(name, "Expected pivot to be 3 numbers in length");
                    group.setPivot(pivot.get(0).asFloat(), pivot.get(1).asFloat(), pivot.get(2).asFloat());
                }

                JsonValue cubesValue = bone.get("cubes");
                if (cubesValue != null) {
                    if (!cubesValue.isArray())
                        throw new ModelException(name,
                                "Expected cubes to be a json array, got type \"" + cubesValue.getType() + "\" instead"
                        );

                    JsonArray cubes = cubesValue.asArray();
                    for (JsonValue cubeValue : cubes) {
                        if (!cubeValue.isArray())
                            throw new ModelException(name,
                                    "Expected cube to be a json object, got type \"" + cubeValue.getType() + "\" instead"
                            );

                        JsonObject cube = cubeValue.asObject();

                        JsonValue originValue = cube.get("origin");
                        if (!originValue.isArray())
                            throw new ModelException(name,
                                    "Expected origin to be a json array, got type \"" + originValue.getType() + "\" instead"
                            );
                        JsonArray origin = originValue.asArray();
                        if (origin.size() != 3) throw new ModelException(name, "Expected origin to be 3 numbers in length");

                        JsonValue sizeValue = cube.get("size");
                        if (!sizeValue.isArray())
                            throw new ModelException(name,
                                    "Expected size to be a json array, got type \"" + sizeValue.getType() + "\" instead"
                            );
                        JsonArray size = sizeValue.asArray();
                        if (size.size() != 3) throw new ModelException(name, "Expected size to be 3 numbers in length");

                        CubePart part = group.newPart(
                                origin.get(0).asFloat(),
                                origin.get(1).asFloat(),
                                origin.get(2).asFloat(),
                                size.get(0).asFloat(),
                                size.get(1).asFloat(),
                                size.get(2).asFloat()
                        );

                        JsonValue cubePivotValue = cube.get("size");
                        if (cubePivotValue != null) {
                            if (!cubePivotValue.isArray())
                                throw new ModelException(name,
                                        "Expected cube pivot to be a json array, got type \"" + cubePivotValue.getType() + "\" instead"
                                );
                            JsonArray pivot = cubePivotValue.asArray();
                            if (pivot.size() != 3) throw new ModelException(name, "Expected cube pivot to be 3 numbers in length");

                            part.setPivot(pivot.get(0).asFloat(), pivot.get(1).asFloat(), pivot.get(2).asFloat());
                        }

                        part.setInflate(cube.getFloat("inflate", 0));

                        JsonValue cubeUVValue = cube.get("uv");
                        if (!cubeUVValue.isArray())
                            throw new ModelException(name,
                                    "Expected uv to be a json array, got type \"" + cubeUVValue.getType() + "\" instead"
                            );
                        JsonArray cubeUV = cubeUVValue.asArray();
                        if (cubeUV.size() != 2) throw new ModelException(name, "Expected uv to be 2 numbers in length");

                    }
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
                CubePart part = rootGroup.newPart(
                        localBounds.get(0).asFloat(),
                        localBounds.get(1).asFloat(),
                        localBounds.get(2).asFloat(),
                        localBounds.get(3).asFloat() - localBounds.get(0).asFloat(),
                        localBounds.get(4).asFloat() - localBounds.get(1).asFloat(),
                        localBounds.get(5).asFloat() - localBounds.get(2).asFloat()
                ).setPivot(8, 8, 8);
                part.setInflate(cuboidObject.getFloat("inflate", 0));
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
                    float[] uvs = face.getUV();
                    uvs[0] = uvsArray.get(0).asFloat();
                    uvs[1] = uvsArray.get(1).asFloat();
                    uvs[2] = uvsArray.get(2).asFloat();
                    uvs[3] = uvsArray.get(3).asFloat();
                }
            });
        }
        // idk, maybe detect off texture
        model.setTransparent(true);

        if (debugMode) LOGGER.log(Level.INFO, "Loading Vanilla Entity Model \"{}\"", name);
        return register(model);
    }

}
