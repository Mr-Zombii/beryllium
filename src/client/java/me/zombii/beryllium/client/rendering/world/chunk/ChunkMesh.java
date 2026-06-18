package me.zombii.beryllium.client.rendering.world.chunk;

import me.zombii.beryllium.client.rendering.BerylliumMesh;

public class ChunkMesh extends BerylliumMesh {

    private final LayeredChunkMesh parent;

    public ChunkMesh(LayeredChunkMesh parent) {
        super(128, true);
        this.parent = parent;
    }

    public LayeredChunkMesh getParent() {
        return parent;
    }
}
