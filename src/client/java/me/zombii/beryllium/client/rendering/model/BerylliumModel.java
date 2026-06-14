package me.zombii.beryllium.client.rendering.model;

import dev.puzzleshq.puzzleloader.cosmic.game.util.HJsonSerializable;
import finalforeach.cosmicreach.util.Identifier;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.common.BerylliumCommon;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BerylliumModel implements Iterable<PartGroup>, HJsonSerializable {

    private final Object2ObjectMap<String, TextureEntry> textureMap = new Object2ObjectArrayMap<>();

    private final ObjectList<PartGroup> groups = new ObjectArrayList<>();
    private final Object2ObjectMap<String, PartGroup> groupMap = new Object2ObjectArrayMap<>();

    private final Identifier id;
    private Identifier renderLayerId;

    public BerylliumModel(
            Identifier id
    ) {
        this.id = id;
        setRenderLayer(null);
    }

    public void setRenderLayer(Identifier layerID) {
        if (layerID == null) {
            this.renderLayerId = Identifier.of(BerylliumCommon.NAMESPACE, "opaque-block-render-layer");
            return;
        }
        this.renderLayerId = layerID;
    }

    public Identifier getRenderLayerId() {
        return renderLayerId;
    }

    public RenderLayer getRenderLayer() {
        return RenderLayers.LAYER_REGISTRY.get(renderLayerId);
    }

    public PartGroup getOrCreateGroup(String name) {
        if (groupMap.containsKey(name)) return groupMap.get(name);

        PartGroup group = new PartGroup(this, name);
        groupMap.put(name, group);
        groups.add(group);
        return group;
    }

    public PartGroup getGroup(String name) {
        return groupMap.get(name);
    }

    public PartGroup removeGroup(String name) {
        PartGroup group = groupMap.get(name);
        groups.remove(group);
        groupMap.remove(name, group);
        return group;
    }

    public List<PartGroup> getGroups() {
        return ObjectLists.unmodifiable(groups);
    }

    public Map<String, PartGroup> getGroupMap() {
        return Object2ObjectMaps.unmodifiable(groupMap);
    }

    public TextureEntry getTexture(String name) {
        if (!textureMap.containsKey(name)) return textureMap.get("all");
        return textureMap.get(name);
    }

    public BerylliumModel addTexture(TextureEntry texture) {
        textureMap.put(texture.getName(), texture);
        return this;
    }

    public BerylliumModel removeTexture(String name) {
        textureMap.remove(name);
        return this;
    }

    public TextureEntry createTexture(String name) {
        TextureEntry texture = new TextureEntry(name);
        textureMap.put(name, texture);
        return texture;
    }

    public Map<String, TextureEntry> getTextureMap() {
        return Object2ObjectMaps.unmodifiable(textureMap);
    }

    public Identifier getID() {
        return id;
    }

    @Override
    @NonNull
    public Iterator<PartGroup> iterator() {
        return groups.iterator();
    }

    public boolean isGreedy() {
        if (groupMap.size() == 1) {
            PartGroup group = groups.getFirst();
            return group.getPivot().isZero() && group.getRotation().isZero() && group.getParts().size() == 1;
        }
        return false;
    }

    @Override
    public JsonValue toHJson() {
        JsonObject obj = new JsonObject();
        JsonObject textures = new JsonObject();
        this.textureMap.forEach((name, texture) -> {
            textures.set(name, texture.toHJson());
        });
        obj.add("textures", textures);

        JsonObject groups = new JsonObject();
        for (PartGroup group : this.groups) {
            groups.set(group.getName(), group.toHJson());
        }
        obj.add("groups", groups);

        obj.set("id", this.id.toString());
        obj.set("renderLayer", this.renderLayerId.toString());

        return obj;
    }
}
