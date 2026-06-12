package me.zombii.beryllium.client.rendering.model.loading.baking;

import com.badlogic.gdx.math.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.concurrent.atomic.AtomicBoolean;

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
            int faceMask,
            short lightLevel,
            byte[] faceAO
    ) {
        for (int i = 0; i < BakedFace.MASKS.length; i++) {
            if ((faceMask & BakedFace.MASKS[i]) != 0) {
                addFaces(
                        tessallator,
                        lightLevel,
                        faceAO,
                        i
                );
            }
        }
    }

    public void addFaces(
            Tessallator tessallator,
            short lightLevel,
            byte[] faceAO,
            int direction
    ) {
        ObjectList<BakedFace> faces = getFacesByDirection(direction);
        for (BakedFace face : faces) {
            tessallator.addQuad(
                    face,
                    lightLevel,
                    faceAO
            );
        }
    }
}

