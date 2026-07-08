package me.zombii.beryllium.client.model.vanilla;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.OrientedBoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import com.badlogic.gdx.utils.Queue;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import finalforeach.cosmicreach.rendering.IMeshData;
import finalforeach.cosmicreach.rendering.blockmodels.BlockModel;
import finalforeach.cosmicreach.rendering.blockmodels.IBlockModelTexture;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.collision.CollisionTriangle;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.zombii.beryllium.client.exceptions.InvalidJsonArrayTypeException;
import me.zombii.beryllium.client.exceptions.InvalidJsonTypeException;
import me.zombii.beryllium.client.exceptions.MissingJsonFieldException;
import me.zombii.beryllium.client.model.loading.BerylliumModelLoader;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.Arrays;
import java.util.Objects;

public class BerylliumBlockModel extends BlockModel {
    private final float[] rotation;
    private final BoundingBox[] boxes;
    private final CollisionTriangle[] tris;
    private static final Int2ObjectMap<BerylliumBlockModel> MODEL_CACHE = new Int2ObjectOpenHashMap<>();

    private final boolean empty;

    private static int modelsMade = 0;
    private final String name;

    private static final Matrix4 groupRotMat = new Matrix4();
    private static final Matrix4 partPosMat = new Matrix4();
    private static final Matrix4 partRotMat = new Matrix4();
    private static final Matrix4 partMat = new Matrix4();
    private static final Vector3 tmpVec = new Vector3();
    private static final Vector3 tmpVec2 = new Vector3();
    private static final Vector3 tmpVec3 = new Vector3();
    private static final float sixteenth = 1/16f;

    public BerylliumBlockModel() {
        this.rotation = new float[]{0, 0, 0};
        this.boxes = new BoundingBox[0];
        this.tris = new CollisionTriangle[0];
        this.empty = true;
        this.name = "empty";
    }

    public BerylliumBlockModel(String name, float[] rotation, BoundingBox[] boundingBoxes, CollisionTriangle[] tris) {
        System.out.println("Models made " + ++modelsMade + " " + name + " " + Arrays.toString(rotation) + " " + Arrays.toString(boundingBoxes));
        this.boxes = boundingBoxes;
        this.calculateBoundingBox();
        this.rotation = rotation;
        this.tris = tris;
        this.empty = false;
        this.name = name;
    }

    private void calculateBoundingBox(){
        for (BoundingBox box : boxes){
            if (this.boundingBox.max.epsilonEquals(this.boundingBox.min)){
                this.boundingBox.set(box);
            }
            else {
                this.boundingBox.ext(box);
            }
        }

        if (this.boundingBox.max.epsilonEquals(this.boundingBox.min)) {
            this.boundingBox.min.set(0.0F, 0.0F, 0.0F);
            this.boundingBox.max.set(1.0F, 1.0F, 1.0F);
        }

        this.boundingBox.update();
    }

    private static int hash(String name, float[] rotations) {
        return Objects.hash(name, rotations[0], rotations[1], rotations[2]);
    }

    private static final BerylliumBlockModel EMPTY = new BerylliumBlockModel();

    private static CollisionTriangle createCollisionTri(Vector3 p1, Vector3 p2, Vector3 p3){
        CollisionTriangle tri = new CollisionTriangle();
        tri.set(p1, p2, p3);
        return tri;
    }

    public static BerylliumBlockModel fromJson(String name, float[] rotation, String json, boolean override) {
        int nameHash = hash(name, rotation);
        if (MODEL_CACHE.containsKey(nameHash) && !override) {
            return MODEL_CACHE.get(nameHash);
        }

        JsonValue modelValue = JsonValue.readHjson(json);
        if (modelValue == null || !modelValue.isObject()) {
            throw new RuntimeException("Expected object for model, not " + (modelValue == null ? null : modelValue.toString()));
        }

        JsonObject modelObject = modelValue.asObject();

        JsonValue groupsValue = modelObject.get("groups");
        if (groupsValue == null || (groupsValue.isArray() && groupsValue.asArray().isEmpty())) {
            String modelParent = modelObject.getString("parent-id", null);
            if (modelParent == null) {
                return EMPTY;
            }
            int parentHash = hash(modelParent, rotation);
            if (MODEL_CACHE.containsKey(parentHash) && !override) {
                return MODEL_CACHE.get(parentHash);
            }
            return fromJson(modelParent, rotation, IndependentAssetLoader.loadAsset(Identifier.of(modelParent + ".json")).getString(), override);
        }

        if (!groupsValue.isObject()) {
            throw new RuntimeException("Expected object for groups, not " + groupsValue);
        }

        JsonObject groupsObject = groupsValue.asObject();
        if (groupsObject.isEmpty()) {
            return EMPTY;
        }

        Array<OrientedBoundingBox> modelCuboids = new Array<>();

        for (String groupName : groupsObject.names()){
            JsonValue group = groupsObject.get(groupName);
            if (group == null || !group.isObject()){
                throw new RuntimeException("Expected object for group, not " + (group == null ? null : group.toString()));
            }

            JsonObject groupObject = group.asObject();
            Vector3 groupRotation = BerylliumModelLoader.getJsonVector3(name, groupObject, "rotation", Vector3.Zero);
            Vector3 groupPivot = BerylliumModelLoader.getJsonVector3(name, groupObject, "pivot", Vector3.Zero);

            tmpVec.set(groupPivot);
            tmpVec.scl(sixteenth);

            groupRotMat.idt();
            groupRotMat.translate(tmpVec);
            groupRotMat.rotate(Vector3.Z, groupRotation.z);
            groupRotMat.rotate(Vector3.Y, groupRotation.y);
            groupRotMat.rotate(Vector3.X, groupRotation.x);
            groupRotMat.translate(tmpVec.scl(-1));

            JsonValue partsValue = groupObject.get("parts");
            if (partsValue == null){
                throw new MissingJsonFieldException(name, "array", "parts");
            }
            if (!partsValue.isArray()){
                throw new InvalidJsonTypeException(name, "parts", "array", partsValue.getType().name());
            }
            JsonArray parts = partsValue.asArray();
            for (JsonValue part : parts){
                if (!part.isObject()){
                    throw new InvalidJsonArrayTypeException(name, "parts", "object", part.getType().name());
                }

                JsonObject partObject = part.asObject();
                if (!BerylliumModelLoader.getJsonBoolean(name, partObject, "canCollide", true)) continue;
                Vector3 partPos = BerylliumModelLoader.getJsonVector3(name, partObject, "pos", null);
                if (partPos == null) throw new MissingJsonFieldException(name, "array", "pos");
                Vector3 partRot = BerylliumModelLoader.getJsonVector3(name, partObject, "rotation", Vector3.Zero);
                Vector3 partPivot = BerylliumModelLoader.getJsonVector3(name, partObject, "pivot", Vector3.Zero);
                Vector3 partSize = BerylliumModelLoader.getJsonVector3(name, partObject, "size", null);
                if (partSize == null) throw new MissingJsonFieldException(name, "array", "size");

                tmpVec.set(partPos);
                tmpVec.scl(sixteenth);

                partPosMat.idt();
                partPosMat.translate(tmpVec);

                tmpVec.set(partPivot);
                tmpVec.scl(sixteenth);

                partRotMat.idt();
                partRotMat.translate(tmpVec);
                partRotMat.rotate(Vector3.Z, partRot.z);
                partRotMat.rotate(Vector3.Y, partRot.y);
                partRotMat.rotate(Vector3.X, partRot.x);
                partRotMat.translate(tmpVec.scl(-1));

                partMat.idt();
                partMat.mul(groupRotMat);
                partMat.mul(partRotMat);
                partMat.mul(partPosMat);

                tmpVec.set(partSize);
                tmpVec.scl(sixteenth);

                BoundingBox partBox = new BoundingBox(Vector3.Zero, tmpVec);
                OrientedBoundingBox partCuboid = new OrientedBoundingBox(partBox, partMat);
                modelCuboids.add(partCuboid);
            }
        }

        // This is the only way I could think of to handle rotated model parts.
        //
        // The other option is to wrench apart the entire game to use OrientedBoundingBoxes for collision
        // and I ain't doing that.
        CollisionTriangle[] tris = new CollisionTriangle[(modelCuboids.size * 2) * 6];
        Array<BoundingBox> boundingBoxes = new Array<>();
        int triIdx = 0;
        for (OrientedBoundingBox orientedBox : modelCuboids){
            // Creating collision triangles
            tris[triIdx++] = createCollisionTri(orientedBox.getCorner000(tmpVec), orientedBox.getCorner001(tmpVec2), orientedBox.getCorner010(tmpVec3));
            tris[triIdx++] = createCollisionTri(orientedBox.getCorner001(tmpVec), orientedBox.getCorner011(tmpVec2), orientedBox.getCorner010(tmpVec3));

            tris[triIdx++] = createCollisionTri(orientedBox.getCorner000(tmpVec), orientedBox.getCorner100(tmpVec2), orientedBox.getCorner110(tmpVec3));
            tris[triIdx++] = createCollisionTri(orientedBox.getCorner000(tmpVec), orientedBox.getCorner010(tmpVec2), orientedBox.getCorner110(tmpVec3));

            tris[triIdx++] = createCollisionTri(orientedBox.getCorner000(tmpVec), orientedBox.getCorner100(tmpVec2), orientedBox.getCorner101(tmpVec3));
            tris[triIdx++] = createCollisionTri(orientedBox.getCorner000(tmpVec), orientedBox.getCorner001(tmpVec2), orientedBox.getCorner101(tmpVec3));

            tris[triIdx++] = createCollisionTri(orientedBox.getCorner010(tmpVec), orientedBox.getCorner110(tmpVec2), orientedBox.getCorner111(tmpVec3));
            tris[triIdx++] = createCollisionTri(orientedBox.getCorner010(tmpVec), orientedBox.getCorner011(tmpVec2), orientedBox.getCorner111(tmpVec3));

            tris[triIdx++] = createCollisionTri(orientedBox.getCorner110(tmpVec), orientedBox.getCorner100(tmpVec2), orientedBox.getCorner101(tmpVec3));
            tris[triIdx++] = createCollisionTri(orientedBox.getCorner110(tmpVec), orientedBox.getCorner111(tmpVec2), orientedBox.getCorner101(tmpVec3));

            tris[triIdx++] = createCollisionTri(orientedBox.getCorner001(tmpVec), orientedBox.getCorner101(tmpVec2), orientedBox.getCorner111(tmpVec3));
            tris[triIdx++] = createCollisionTri(orientedBox.getCorner001(tmpVec), orientedBox.getCorner011(tmpVec2), orientedBox.getCorner111(tmpVec3));

            // Creating collision boxes
            orientedBox.getCorner000(tmpVec);
            orientedBox.getCorner111(tmpVec2);
            BoundingBox masterBB = new BoundingBox();
            masterBB.min.x = Math.min(tmpVec.x, tmpVec2.x);
            masterBB.min.y = Math.min(tmpVec.y, tmpVec2.y);
            masterBB.min.z = Math.min(tmpVec.z, tmpVec2.z);

            masterBB.max.x = Math.max(tmpVec.x, tmpVec2.x);
            masterBB.max.y = Math.max(tmpVec.y, tmpVec2.y);
            masterBB.max.z = Math.max(tmpVec.z, tmpVec2.z);

            masterBB.ext(orientedBox.getCorner001(tmpVec));
            masterBB.ext(orientedBox.getCorner010(tmpVec));
            masterBB.ext(orientedBox.getCorner011(tmpVec));
            masterBB.ext(orientedBox.getCorner100(tmpVec));
            masterBB.ext(orientedBox.getCorner101(tmpVec));
            masterBB.ext(orientedBox.getCorner110(tmpVec));

            masterBB.update();

            orientedBox.getBounds().getDimensions(tmpVec);
            tmpVec.scl(0.5f);
            partMat.set(orientedBox.getTransform());
            partMat.translate(tmpVec);
            partMat.scl(0.9f);
            partMat.translate(tmpVec.scl(-1));
            OrientedBoundingBox checkOBB = new OrientedBoundingBox( // Gets rid of some excess boxes
                    orientedBox.getBounds(),
                    partMat
            );

            // Starting with one box, approximate the rotated bounding box
            // using many small non-rotated boxes
            Queue<BoundingBox> boxesToCheck = new Queue<>();
            boxesToCheck.addLast(masterBB);

            while (!boxesToCheck.isEmpty()){
                BoundingBox box = boxesToCheck.removeFirst();
                if (orientedBox.contains(box)) {
                    boundingBoxes.add(box);
                    continue;
                }
                if (!checkOBB.intersects(box)) continue;
                if (box.getHeight() < (sixteenth * 4 - 0.001f) || box.getWidth() < (sixteenth * 4 - 0.001f) || box.getDepth() < (sixteenth * 4 - 0.001f)){
                    boundingBoxes.add(box); // Cap the box size at 1/8th
                    continue;
                }
                // The box gets split into 8, which go back into the queue
                box.getCenter(tmpVec);
                tmpVec2.set(box.min);
                tmpVec3.set(tmpVec);
                boxesToCheck.addLast(new BoundingBox(tmpVec2, tmpVec3));
                tmpVec2.y = tmpVec.y;
                tmpVec3.y = box.max.y;
                boxesToCheck.addLast(new BoundingBox(tmpVec2, tmpVec3));
                tmpVec2.x = tmpVec.x;
                tmpVec3.x = box.max.x;
                boxesToCheck.addLast(new BoundingBox(tmpVec2, tmpVec3));
                tmpVec2.y = box.min.y;
                tmpVec3.y = tmpVec.y;
                boxesToCheck.addLast(new BoundingBox(tmpVec2, tmpVec3));
                tmpVec2.z = tmpVec.z;
                tmpVec3.z = box.max.z;
                boxesToCheck.addLast(new BoundingBox(tmpVec2, tmpVec3));
                tmpVec2.x = box.min.x;
                tmpVec3.x = tmpVec.x;
                boxesToCheck.addLast(new BoundingBox(tmpVec2, tmpVec3));
                tmpVec2.y = tmpVec.y;
                tmpVec3.y = box.max.y;
                boxesToCheck.addLast(new BoundingBox(tmpVec2, tmpVec3));
                boxesToCheck.addLast(new BoundingBox(tmpVec, box.max));
            }
        }

        BoundingBox[] result = new BoundingBox[boundingBoxes.size];
        for (int bbidx = 0; bbidx < result.length; ++bbidx){
            result[bbidx] = boundingBoxes.get(bbidx);
        }
        BerylliumBlockModel model = new BerylliumBlockModel(name, rotation, result, tris);
        MODEL_CACHE.put(hash(name, rotation), model);
        return model;
    }

    @Override
    public void addVertices(IMeshData iMeshData, int i, int i1, int i2, int i3, short[] shorts, int[] ints) {

    }

    @Override
    public boolean isGreedyCube() {
        return false;
    }

    @Override
    public boolean canGreedyCombine() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public void getAllBoundingBoxes(Array<BoundingBox> array, int x, int y, int z) {
        array.size = 0;
        if (boxes.length == 0) return;

        Object[] items = array.items;

        for (BoundingBox box : boxes) {

            BoundingBox item;
            if (array.items.length >= array.size) {
                item = new BoundingBox();
            } else {
                item = (BoundingBox) items[array.size];
                if (item == null) item = new BoundingBox();
            }

            item.min.set(box.min);
            item.max.set(box.max);
            item.min.add(x, y, z);
            item.max.add(x, y, z);
            item.update();

            array.add(item);
        }
    }

    @Override
    public void getAllCollisionTriangles(Array<CollisionTriangle> array, int x, int y, int z) {
        array.size = 0;
        if (tris.length == 0) return;

        Object[] items = array.items;

        for (int i = 0; i < tris.length; i++) {
            CollisionTriangle item;
            if (i > items.length - 1) {
                item = new CollisionTriangle();
            } else {
                item = (CollisionTriangle) items[i];
                if (item == null) item = new CollisionTriangle();
            }

            item.p0.set(tris[i].p0);
            item.p1.set(tris[i].p1);
            item.p2.set(tris[i].p2);

            item.p0.add(x, y, z);
            item.p1.add(x, y, z);
            item.p2.add(x, y, z);

            array.add(item);
        }
    }

    @Override
    public int getNumberOfBoundingBoxes() {
        return this.boxes.length;
    }

    @Override
    public OrderedMap<String, ? extends IBlockModelTexture> getTextures() {
        return null;
    }
}
