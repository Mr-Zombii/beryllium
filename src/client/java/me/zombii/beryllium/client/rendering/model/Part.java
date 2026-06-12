package me.zombii.beryllium.client.rendering.model;

import com.badlogic.gdx.math.Vector3;

import java.util.concurrent.atomic.AtomicReference;

public class Part {
    private final PartFace[] faces = new PartFace[6];
    private final BerylliumModel model;
    private final PartGroup group;

    protected Part(BerylliumModel model, PartGroup group) {
        this.model = model;
        this.group = group;
    }

    private final Vector3 pos = new Vector3();
    private final Vector3 size = new Vector3(16, 16, 16);
    private final Vector3 pivot = new Vector3();
    private final Vector3 rotation = new Vector3();

    private final AtomicReference<Float> scale = new AtomicReference<>(0f);

    public PartFace[] getFaces() {
        return faces;
    }

    public Vector3 getPos() {
        return pos;
    }

    public Vector3 getSize() {
        return size;
    }

    public float getScale() {
        return scale.get();
    }

    public Part setScale(float scale) {
        this.scale.set(scale);
        return this;
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public Vector3 getPivot() {
        return pivot;
    }

    public Part setPivot(Vector3 pivot) {
        this.pivot.set(pivot);
        return this;
    }

    public Part setOrigin(float x, float y, float z) {
        this.pivot.set(x, y, z);
        return this;
    }

    public Part setRotation(Vector3 rotation) {
        this.rotation.set(rotation);
        return this;
    }

    public Part setRotation(float x, float y, float z) {
        this.rotation.set(x, y, z);
        return this;
    }

    public Part setPosition(Vector3 pos) {
        this.pos.set(pos);
        return this;
    }

    public Part setPosition(float x, float y, float z) {
        this.pos.set(x, y, z);
        return this;
    }

    public Part setSize(Vector3 size) {
        this.size.set(size);
        return this;
    }

    public Part setSize(float x, float y, float z) {
        this.size.set(x, y, z);
        return this;
    }

    public BerylliumModel getModel() {
        return model;
    }

    public PartGroup getGroup() {
        return group;
    }
}