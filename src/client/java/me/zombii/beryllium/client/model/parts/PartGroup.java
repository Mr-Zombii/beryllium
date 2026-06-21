package me.zombii.beryllium.client.model.parts;

import com.badlogic.gdx.math.Vector3;
import dev.puzzleshq.puzzleloader.cosmic.game.util.HJsonSerializable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import me.zombii.beryllium.client.model.BerylliumModel;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PartGroup implements Iterable<Part>, HJsonSerializable {

    private final ObjectList<Part> parts = new ObjectArrayList<>();

    private final Vector3 pivot = new Vector3();
    private final Vector3 rotation = new Vector3();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    private final String name;
    private String parentName;
    private final BerylliumModel model;

    protected PartGroup(BerylliumModel model, String name) {
        this.name = name;
        this.model = model;
    }

    public Part newPart(float x, float y, float z, float sizeX, float sizeY, float sizeZ) {
        Part part = new Part(model, this).setPosition(x, y, z).setSize(sizeX, sizeY, sizeZ);
        parts.add(part);
        return part;
    }

    public Part newPart(float x, float y, float z, Vector3 size) {
        Part part = new Part(model, this).setPosition(x, y, z).setSize(size);
        parts.add(part);
        return part;
    }

    public Part newPart(Vector3 position, float sizeX, float sizeY, float sizeZ) {
        Part part = new Part(model, this).setPosition(position).setSize(sizeX, sizeY, sizeZ);
        parts.add(part);
        return part;
    }

    public Part newPart(Vector3 position, Vector3 size) {
        Part part = new Part(model, this).setPosition(position).setSize(size);
        parts.add(part);
        return part;
    }

    public Part removePart(Part part) {
        parts.remove(part);
        return part;
    }

    public List<Part> getParts() {
        return ObjectLists.unmodifiable(parts);
    }

    public Vector3 getPivot() {
        return pivot;
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public String getName() {
        return name;
    }

    public String getParentName() {
        return parentName;
    }

    public PartGroup setParentName(String parentName) {
        this.parentName = parentName;
        return this;
    }

    public PartGroup setPivot(Vector3 pivot) {
        this.pivot.set(pivot);
        return this;
    }

    public PartGroup setOrigin(float x, float y, float z) {
        this.pivot.set(x, y, z);
        return this;
    }

    public PartGroup setRotation(Vector3 rotation) {
        this.rotation.set(rotation);
        return this;
    }

    public PartGroup setRotation(float x, float y, float z) {
        this.rotation.set(x, y, z);
        return this;
    }

    public BerylliumModel getModel() {
        return model;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    @NonNull
    public Iterator<Part> iterator() {
        return parts.iterator();
    }


    @Override
    public JsonValue toHJson() {
        JsonObject obj = new JsonObject();
        obj.set("pivot", new JsonArray().add(pivot.x).add(pivot.y).add(pivot.z));
        obj.set("rotation", new JsonArray().add(rotation.x).add(rotation.y).add(rotation.z));
        obj.set("name", name);
        if (parentName != null)
            obj.set("parent", parentName);
        obj.set("enabled", enabled.get());

        JsonArray parts = new JsonArray();
        for (Part part : this) parts.add(part.toHJson());
        obj.set("parts", parts);
        
        return obj;
    }
}