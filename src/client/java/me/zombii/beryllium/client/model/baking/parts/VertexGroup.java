package me.zombii.beryllium.client.model.baking.parts;

import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class VertexGroup {

    private final BakedBerylliumModel model;
    private final String name;
    private final String parentName;

    private final Vector3 rotation = new Vector3();
    private final Vector3 pivot = new Vector3();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    private final ObjectList<BakedFace> posXFaces;
    private final ObjectList<BakedFace> posYFaces;
    private final ObjectList<BakedFace> posZFaces;
    private final ObjectList<BakedFace> negXFaces;
    private final ObjectList<BakedFace> negYFaces;
    private final ObjectList<BakedFace> negZFaces;
    private final ObjectList<BakedFace> unculledFaces;

    public VertexGroup(
            BakedBerylliumModel model,
            String name,
            String parentName
    ) {
        this.name = name;
        this.model = model;
        this.parentName = parentName;

        this.unculledFaces = new ObjectArrayList<>();
        this.posXFaces = new ObjectArrayList<>();
        this.posYFaces = new ObjectArrayList<>();
        this.posZFaces = new ObjectArrayList<>();
        this.negXFaces = new ObjectArrayList<>();
        this.negYFaces = new ObjectArrayList<>();
        this.negZFaces = new ObjectArrayList<>();
    }

    public ObjectList<BakedFace> getNegXFaces() {
        return negXFaces;
    }

    public ObjectList<BakedFace> getNegYFaces() {
        return negYFaces;
    }

    public ObjectList<BakedFace> getNegZFaces() {
        return negZFaces;
    }

    public ObjectList<BakedFace> getPosXFaces() {
        return posXFaces;
    }

    public ObjectList<BakedFace> getPosYFaces() {
        return posYFaces;
    }

    public ObjectList<BakedFace> getPosZFaces() {
        return posZFaces;
    }

    public ObjectList<BakedFace> getUnculledFaces() {
        return unculledFaces;
    }

    public ObjectList<BakedFace> getFacesByDirection(int d) {
        return switch (d) {
            case -1 -> getUnculledFaces();
            case 0 -> getNegXFaces();
            case 1 -> getPosXFaces();
            case 2 -> getNegYFaces();
            case 3 -> getPosYFaces();
            case 4 -> getNegZFaces();
            case 5 -> getPosZFaces();
            default -> throw new IllegalArgumentException("Invalid direction " + d);
        };
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public Vector3 getPivot() {
        return pivot;
    }

    public VertexGroup setRotation(float x, float y, float z) {
        rotation.set(x, y, z);
        return this;
    }

    public VertexGroup setPivot(float x, float y, float z) {
        pivot.set(x, y, z);
        return this;
    }

    public VertexGroup setRotation(Vector3 pos) {
        this.rotation.set(pos);
        return this;
    }

    public VertexGroup setPivot(Vector3 pos) {
        this.pivot.set(pos);
        return this;
    }

    public String getName() {
        return name;
    }

    public BakedBerylliumModel getModel() {
        return model;
    }

    public String getParentName() {
        return parentName;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public void addFaces(
            Tessallator tessallator,
            short[] skyLightLevels,
            short[] blockLightLevels,
            byte[] aoLevels,
            int faceMask,
            TintProvider.TintFunction tintFunction, Chunk chunk,
            BlockState state,
            int x, int y, int z
    ) {
        for (int d = 0; d < BakedFace.MASKS.length; d++) {
            if ((faceMask & BakedFace.MASKS[d]) != 0) {
                addFaces(
                        tessallator,
                        skyLightLevels,
                        blockLightLevels,
                        aoLevels,
                        tintFunction, chunk, state,
                        d, x, y, z
                );
            }
        }
    }

    public void addFaces(
            Tessallator tessallator,
            short[] skyLightLevels,
            short[] blockLightLevels,
            byte[] aoLevels,
            TintProvider.TintFunction tintFunction, Chunk chunk,
            BlockState state,
            int direction, int x, int y, int z
    ) {
        ObjectList<BakedFace> faces = getFacesByDirection(direction == 6 ? -1 : direction);
        for (BakedFace face : faces) {
            int directionOrdinal = face.direction();

            tessallator.addQuad(
                    face,
                    skyLightLevels[directionOrdinal],
                    blockLightLevels[directionOrdinal],
                    aoLevels,
                    tintFunction.getTint(chunk, state, x, y, z, face.tintIndex()),
                    directionOrdinal * 4, x, y, z
            );
        }
    }
}

