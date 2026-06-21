package me.zombii.beryllium.client.model.baking;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.baking.parts.VertexGroup;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

public class BakedBerylliumModel implements Iterable<VertexGroup> {

    private final BerylliumModel model;
    private final ObjectList<VertexGroup> groupList;
    private final Object2ObjectMap<String, VertexGroup> groupMap;

    public static final int MAX_PARTS_PER_MODEL = 512;
    public static final int MAX_FACES_PER_MODEL = MAX_PARTS_PER_MODEL * 6;
    public static final int MAX_VERTICES_PER_MODEL = MAX_FACES_PER_MODEL * 4;
    public static final int MAX_VERTICES_PER_CHUNK = MAX_VERTICES_PER_MODEL * (16 * 16 * 16);

    public BakedBerylliumModel(
            BerylliumModel model,
            ObjectList<VertexGroup> groupList,
            Object2ObjectMap<String, VertexGroup> groupMap
    ) {
        this.model = model;
        this.groupList = groupList;
        this.groupMap = groupMap;
    }

    public BerylliumModel getModel() {
        return model;
    }

    public ObjectList<VertexGroup> getGroupList() {
        return ObjectLists.unmodifiable(groupList);
    }

    public Map<String, VertexGroup> getGroupMap() {
        return Object2ObjectMaps.unmodifiable(groupMap);
    }

    @Override
    @NonNull
    public Iterator<VertexGroup> iterator() {
        return groupList.iterator();
    }

    public void addVertices(
            Tessallator tessallator,
            short[] skyLightLevels,
            short[] blockLightLevels,
            byte[] aoLevels,
            int faceMask,
            Function<Integer, Short> tintGetter,
            int x, int y, int z
    ) {
        for (VertexGroup group : groupList) {
            group.addFaces(
                    tessallator,
                    skyLightLevels,
                    blockLightLevels,
                    aoLevels,
                    faceMask,
                    tintGetter,
                    x, y, z
            );
        }
    }

    public boolean isGreedy() {
        if (groupMap.size() == 1) {
            VertexGroup group = groupList.getFirst();
            return group.getPivot().isZero() && group.getRotation().isZero() && getModel().isGreedy();
        }
        return false;
    }

}
