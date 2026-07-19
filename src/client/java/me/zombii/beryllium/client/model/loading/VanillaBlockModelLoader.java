package me.zombii.beryllium.client.model.loading;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
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
import me.zombii.beryllium.common.BerylliumCommon;
import me.zombii.beryllium.common.BerylliumConfig;
import org.apache.logging.log4j.Level;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class VanillaBlockModelLoader extends BerylliumModelLoader {

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
                    parentModel = VanillaBlockModelLoader.loadVanillaBlockModel(parentModelName.trim(), IndependentAssetLoader.loadAsset(Identifier.of(parentModelName.trim())));
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
        } else {
            if (foundParentModel != null) {
                for (PartGroup group : foundParentModel) {
                    PartGroup newGroup = model.getOrCreateGroup(group.getName());
                    newGroup.setPivot(group.getPivot());
                    newGroup.setRotation(group.getRotation());
                    newGroup.setParentName(group.getParentName());
                    for (CubePart part : group) {
                        CubePart newPart = newGroup.newPart(part.getPos(), part.getSize())
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

                CubePart part = rootGroup.newPart(
                        partPos.x,
                        partPos.y,
                        partPos.z,
                        length,
                        width,
                        0
                ).setPivot(center).setRotation(
                        eulerAngles
                ).setCanCollide(false);

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
                int uvRot = get(model, uvsArray) * 90;

                topFace.setUVRotation(uvRot);
                bottomFace.setUVRotation(uvRot);
                System.arraycopy(TMP_UV.get(), 0, topFace.getUV(), 0, 4);
                System.arraycopy(TMP_UV.get(), 0, bottomFace.getUV(), 0, 4);

//                float[] topUvs = topFace.getUV();
//                topUvs[0] = uvsArray.get(0).asFloat();
//                topUvs[1] = uvsArray.get(1).asFloat();
//                topUvs[2] = uvsArray.get(4).asFloat();
//                topUvs[3] = uvsArray.get(5).asFloat();
//
//                float[] bottomUvs = bottomFace.getUV();
//                bottomUvs[0] = uvsArray.get(2).asFloat();
//                bottomUvs[1] = uvsArray.get(3).asFloat();
//                bottomUvs[2] = uvsArray.get(6).asFloat();
//                bottomUvs[3] = uvsArray.get(7).asFloat();
            });
        }

        if (debugMode) LOGGER.log(Level.INFO, "Loading Vanilla Block Model \"{}\"", name);
        return register(model);
    }

    private static final ThreadLocal<float[]> TMP_UV = ThreadLocal.withInitial(() -> new float[4]);
    private static final ThreadLocal<int[]> TMP_UV_IDX = ThreadLocal.withInitial(() -> new int[4]);
    private static final ThreadLocal<Set<Float>> TMP_UNIQUE_VALES = ThreadLocal.withInitial(() -> HashSet.newHashSet(2));

    private static int get(BerylliumModel model, JsonArray array) {
        float[] tmp = TMP_UV.get();
        TMP_UV.set(tmp);
        Set<Float> test = TMP_UNIQUE_VALES.get();
        test.clear();
        TMP_UNIQUE_VALES.set(test);

        for (int i = 0; i < array.size(); i++) test.add(array.get(i).asFloat());
        if (test.size() > 4)
            throw new ModelException(model, "Non uniform uvs found when processing plane! More than 4 unique values for the uvs!");

        // common uv order
        float u00 = array.get(0).asFloat();
        float v00 = array.get(1).asFloat();

        float u01 = array.get(2).asFloat();
        float v01 = array.get(3).asFloat();

        float u11 = array.get(4).asFloat();
        float v11 = array.get(5).asFloat();

        float u10 = array.get(6).asFloat();
        float v10 = array.get(7).asFloat();

        float minU = Math.min(u00, Math.min(u01, Math.min(u10, u11)));
        float minV = Math.min(v00, Math.min(v01, Math.min(v10, v11)));

        float maxU = Math.max(u00, Math.max(u01, Math.max(u10, u11)));
        float maxV = Math.max(v00, Math.max(v01, Math.max(v10, v11)));

        tmp[0] = minU;
        tmp[1] = minV;
        tmp[2] = maxU;
        tmp[3] = maxV;

        int flags = findRotation(
                model,
                u00, v00,
                u01, v01,
                u11, v11,
                u10, v10,
                minU, minV,
                maxU, maxV
        );
        int flip = (flags >>> 2) & 0b11;
        int rot = flip & 0b11;

        boolean flipU = (flip & 0b10) != 0;
        boolean flipV = (flip & 0b01) != 0;

        if (flipU) {
            tmp[0] = maxU;
            tmp[2] = minU;
        }

        if (flipV) {
            tmp[1] = maxV;
            tmp[3] = minV;
        }

        return rot;
    }

    private static int check(
            float minU, float minV,
            float u, float v
    ) {
        int uCheck = u != minU ? 1 : 0;
        int vCheck = v != minV ? 1 : 0;

        return (uCheck << 1) | vCheck;
    }

    // top left
    private static final int UV00 = 0b00;

    // top right
    private static final int UV01 = 0b01;

    // bottom left
    private static final int UV10 = 0b10;

    // bottom right
    private static final int UV11 = 0b11;

    private static final int ROT_0 = UV00 << 6 | UV01 << 4 | UV10 << 1 | UV11;
    private static final int ROT_3 = UV10 << 6 | UV00 << 4 | UV11 << 1 | UV01;
    private static final int ROT_2 = UV11 << 6 | UV10 << 4 | UV01 << 1 | UV00;
    private static final int ROT_1 = UV01 << 6 | UV11 << 4 | UV00 << 1 | UV10;

    private static int checkRotation(
            int c0, int c1, int c2, int c3
    ) {
        int check = c3 << 6 | c2 << 4 | c1 << 2 | c0;

        return switch (check) {
            case ROT_0 -> 0;
            case ROT_1 -> 1;
            case ROT_2 -> 2;
            case ROT_3 -> 3;
            default -> -1; // flips has been applied pre- or post-rotation
        };
    }

    private static int findRotation(
            BerylliumModel model,
            float u00, float v00,
            float u01, float v01,
            float u11, float v11,
            float u10, float v10,
            float minU, float minV,
            float maxU, float maxV
    ) {
        int uv00Idx = check(minU, minV, u00, v00);
        int uv01Idx = check(minU, minV, u01, v01);
        int uv11Idx = check(minU, minV, u11, v11);
        int uv10Idx = check(minU, minV, u10, v10);

        int[] indices = TMP_UV_IDX.get();
        indices[0] = uv11Idx;
        indices[1] = uv10Idx;
        indices[2] = uv01Idx;
        indices[3] = uv00Idx;
        TMP_UV_IDX.set(indices);

        int flip = 0;
        int rot = checkRotation(uv11Idx, uv10Idx, uv01Idx, uv00Idx);
        if (rot == -1) {
            // check for flipped u
            flip = 0b10;
            rot = checkRotation(uv01Idx, uv00Idx, uv11Idx, uv10Idx);
        }
        if (rot == -1) {
            // check for flipped v
            flip = 0b01;
            rot = checkRotation(uv10Idx, uv11Idx, uv00Idx, uv01Idx);
        }
        if (rot == -1) {
            // check for flipped uv
            flip = 0b11;
            rot = checkRotation(uv00Idx, uv01Idx, uv10Idx, uv11Idx);
        }
        if (rot == -1)
            throw new ModelException(model, "Unhandled, uv flip must have happened post rotation!");

        return flip << 2 | rot;
    }

}
