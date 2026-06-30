package me.zombii.beryllium.client.model.vanilla;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.OrderedMap;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import finalforeach.cosmicreach.rendering.IMeshData;
import finalforeach.cosmicreach.rendering.blockmodels.BlockModel;
import finalforeach.cosmicreach.rendering.blockmodels.IBlockModelTexture;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.collision.CollisionTriangle;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.zombii.beryllium.client.model.loading.NewClientModelLoader;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.Arrays;
import java.util.Objects;

public class BasicBlockModel extends BlockModel {

    private final float[] rotation;
    private final BoundingBox[] boxes;
    private final CollisionTriangle[] tris;
    private static final Int2ObjectMap<BasicBlockModel> MODEL_CACHE = new Int2ObjectOpenHashMap<>();

    private final boolean empty;

    public BasicBlockModel() {
        this.rotation = new float[]{0, 0, 0};
        this.boxes = new BoundingBox[0];
        this.tris = new CollisionTriangle[0];
        this.empty = true;
        this.name = "empty";
    }

    private static int modelsMade = 0;
    private final String name;

    public BasicBlockModel(String name, float[] rotation, BoundingBox[] boundingBoxes, CollisionTriangle[] tris) {
        System.out.println("Models made" + ++modelsMade + " " + name + " " + Arrays.toString(rotation) + " " + Arrays.toString(boundingBoxes));
        this.boxes = boundingBoxes;
        this.rotation = rotation;
        this.tris = tris;
        this.empty = false;
        this.name = name;
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
        return boxes.length;
    }

    @Override
    public OrderedMap<String, ? extends IBlockModelTexture> getTextures() {
        return null;
    }

    private static final BasicBlockModel EMPTY = new BasicBlockModel();
    private static final BasicBlockModel CUBE;

    private static int hash(String name, float[] rotations) {
        return Objects.hash(name, rotations[0], rotations[1], rotations[2]);
    }

    static {
        CUBE = fromJson(
                "base:models/blocks/cube.json",
                NewClientModelLoader.DEFAULT_ROTATION,
                IndependentAssetLoader.loadAsset(Identifier.of("base:models/blocks/cube.json")).getString(),
                false
        );
    }

    public static BasicBlockModel fromJson(String name, float[] rotation, String json, boolean override) {
        int nameHash = hash(name, rotation);
        if (MODEL_CACHE.containsKey(nameHash) && !override) {
            return MODEL_CACHE.get(nameHash);
        }

        JsonValue modelValue = JsonValue.readHjson(json);
        if (modelValue == null || !modelValue.isObject()) {
            throw new RuntimeException("Expected object for model, not " + (modelValue == null ? null : modelValue.toString()));
        }

        JsonObject modelObject = modelValue.asObject();
        JsonValue cuboidsValue = modelObject.get("cuboids");
        if (cuboidsValue == null || (cuboidsValue.isArray() && cuboidsValue.asArray().isEmpty())) {
            String modelParent = modelObject.getString("parent", null);
            if (modelParent == null) {
                return EMPTY;
            }
            int parentHash = hash(modelParent, rotation);
            if (MODEL_CACHE.containsKey(parentHash) && !override) {
                return MODEL_CACHE.get(parentHash);
            }
            return fromJson(modelParent, rotation, IndependentAssetLoader.loadAsset(Identifier.of(modelParent)).getString(), override);
        }

        if (!cuboidsValue.isArray()) {
            throw new RuntimeException("Expected array for cuboids, not " + cuboidsValue);
        }

        JsonArray cuboids = cuboidsValue.asArray();
        if (cuboids.isEmpty()) {
            return BasicBlockModel.EMPTY;
        }

        BoundingBox[] boundingBoxes = new BoundingBox[cuboids.size()];
        CollisionTriangle[] tris = new CollisionTriangle[(cuboids.size() * 2) * 6];
        int bbIdx = 0;
        int triIdx = 0;
        int cubeCount = cuboids.size();

        if (cubeCount == 1 && CUBE != null)
            return CUBE;

        for (JsonValue cuboid : cuboids) {
            if (cuboid == null || !cuboid.isObject()) {
                throw new RuntimeException("Expected object for cuboid, not " + (cuboid == null ? null : cuboid.toString()));
            }

            JsonObject cuboidObject = cuboid.asObject();
            JsonValue localBounds = cuboidObject.get("localBounds");
            if (localBounds == null || !localBounds.isArray()) {
                throw new RuntimeException("Expected array for local bounds, not " + (localBounds == null ? null : localBounds.toString()));
            }

            JsonArray bounds = localBounds.asArray();
            if (bounds.size() < 6) throw new RuntimeException("Expected at least 6 in size for localBounds, not " + bounds.size());

            Vector3 min = new Vector3(bounds.get(0).asFloat() / 16f, bounds.get(1).asFloat() / 16f, bounds.get(2).asFloat() / 16f);
            Vector3 max = new Vector3(bounds.get(3).asFloat() / 16f, bounds.get(4).asFloat() / 16f, bounds.get(5).asFloat() / 16f);

            float minX = min.x;
            float minY = min.y;
            float minZ = min.z;

            float maxX = max.x;
            float maxY = max.y;
            float maxZ = max.z;

            min.set(Math.min(minX, maxX), Math.min(minY, maxY), Math.min(minZ, maxZ));
            max.set(Math.max(minX, maxX), Math.max(minY, maxY), Math.max(minZ, maxZ));

            BoundingBox box = new BoundingBox(min, max);
            boundingBoxes[bbIdx++] = box;

            CollisionTriangle triMinXA = tris[triIdx++] = new CollisionTriangle();
            CollisionTriangle triMinXB = tris[triIdx++] = new CollisionTriangle();

            triMinXA.p0.set(box.min.x, box.max.y, box.min.z);
            triMinXA.p1.set(box.min.x, box.min.y, box.min.z);
            triMinXA.p2.set(box.min.x, box.min.y, box.max.z);

            triMinXB.p0.set(box.min.x, box.max.y, box.min.z);
            triMinXB.p1.set(box.min.x, box.max.y, box.max.z);
            triMinXB.p2.set(box.min.x, box.min.y, box.max.z);

            CollisionTriangle triMaxXA = tris[triIdx++] = new CollisionTriangle();
            CollisionTriangle triMaxXB = tris[triIdx++] = new CollisionTriangle();

            triMaxXA.p0.set(box.max.x, box.max.y, box.min.z);
            triMaxXA.p1.set(box.max.x, box.min.y, box.min.z);
            triMaxXA.p2.set(box.max.x, box.min.y, box.max.z);

            triMaxXB.p0.set(box.max.x, box.max.y, box.min.z);
            triMaxXB.p1.set(box.max.x, box.max.y, box.max.z);
            triMaxXB.p2.set(box.max.x, box.min.y, box.max.z);

            CollisionTriangle triMinZA = tris[triIdx++] = new CollisionTriangle();
            CollisionTriangle triMinZB = tris[triIdx++] = new CollisionTriangle();

            triMinZA.p0.set(box.max.x, box.min.y, box.min.z);
            triMinZA.p1.set(box.min.x, box.min.y, box.min.z);
            triMinZA.p2.set(box.min.x, box.max.y, box.min.z);

            triMinZB.p0.set(box.max.x, box.min.y, box.min.z);
            triMinZB.p1.set(box.max.x, box.max.y, box.min.z);
            triMinZB.p2.set(box.min.x, box.max.y, box.min.z);

            CollisionTriangle triMaxZA = tris[triIdx++] = new CollisionTriangle();
            CollisionTriangle triMaxZB = tris[triIdx++] = new CollisionTriangle();

            triMaxZA.p0.set(box.max.x, box.min.y, box.max.z);
            triMaxZA.p1.set(box.min.x, box.min.y, box.max.z);
            triMaxZA.p2.set(box.min.x, box.max.y, box.max.z);

            triMaxZB.p0.set(box.max.x, box.min.y, box.max.z);
            triMaxZB.p1.set(box.max.x, box.max.y, box.max.z);
            triMaxZB.p2.set(box.min.x, box.max.y, box.max.z);

            CollisionTriangle triMinYA = tris[triIdx++] = new CollisionTriangle();
            CollisionTriangle triMinYB = tris[triIdx++] = new CollisionTriangle();

            triMinYA.p0.set(box.max.x, box.min.y, box.min.z);
            triMinYA.p1.set(box.min.x, box.min.y, box.min.z);
            triMinYA.p2.set(box.min.x, box.min.y, box.max.z);

            triMinYB.p0.set(box.max.x, box.min.y, box.min.z);
            triMinYB.p1.set(box.max.x, box.min.y, box.max.z);
            triMinYB.p2.set(box.min.x, box.min.y, box.max.z);

            CollisionTriangle triMaxYA = tris[triIdx++] = new CollisionTriangle();
            CollisionTriangle triMaxYB = tris[triIdx++] = new CollisionTriangle();

            triMaxYA.p0.set(box.max.x, box.max.y, box.min.z);
            triMaxYA.p1.set(box.min.x, box.max.y, box.min.z);
            triMaxYA.p2.set(box.min.x, box.max.y, box.max.z);

            triMaxYB.p0.set(box.max.x, box.max.y, box.min.z);
            triMaxYB.p1.set(box.max.x, box.max.y, box.max.z);
            triMaxYB.p2.set(box.min.x, box.max.y, box.max.z);
        }

        BasicBlockModel model = new BasicBlockModel(name, rotation, boundingBoxes, tris);
        MODEL_CACHE.put(hash(name, rotation), model);
        return model;
    }

    public float[] getRotation() {
        return rotation;
    }

    public BoundingBox[] getBoxes() {
        return boxes;
    }

    public CollisionTriangle[] getTris() {
        return tris;
    }
}
