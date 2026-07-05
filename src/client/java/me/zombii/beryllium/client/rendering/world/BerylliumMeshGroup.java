package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.Gdx;
import finalforeach.cosmicreach.rendering.IChunkMeshGroup;
import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class BerylliumMeshGroup implements IChunkMeshGroup<LayeredChunkMesh> {

    private final LayeredChunkMesh mesh;
    private final AtomicBoolean flaggedForRemesh = new AtomicBoolean(false);
    private final AtomicBoolean isFlagImmediate = new AtomicBoolean(false);

    public BerylliumMeshGroup() {
        this.mesh = new LayeredChunkMesh();
    }

    @Override
    public boolean hasMesh() {
        return this.mesh.isFinished();
    }

    @Override
    public LayeredChunkMesh getAllMeshData() {
        return this.mesh;
    }

    @Override
    public void flagForRemeshing(boolean remesh) {
        flaggedForRemesh.set(remesh);
        isFlagImmediate.set(false);
    }

    @Override
    public boolean isFlaggedForRemeshing() {
        return flaggedForRemesh.get();
    }

    @Override
    public void flushRemeshRequests() {
        this.flaggedForRemesh.set(false);
        this.isFlagImmediate.set(false);
    }

    @Override
    public boolean isFlaggedForImmediateRemesh() {
        return isFlagImmediate.get();
    }

    @Override
    public void setToRemeshImmediately(boolean remesh) {
        this.isFlagImmediate.set(remesh);
        this.flaggedForRemesh.set(remesh);
    }

    @Override
    public void dispose() {
        mesh.scheduleForDisposal();
        Gdx.app.postRunnable(mesh::dispose);
    }

    @Override
    public boolean isAllMeshDataEmpty() {
        return Arrays.stream(mesh.getLayers()).anyMatch((c) -> c != null && !c.isEmpty());
    }

    @Override
    public void setMeshVertices(Chunk chunk, LayeredChunkMesh mesh) {

    }

    @Override
    public void setExpectedMeshGenCount() {

    }

    @Override
    public boolean hasExpectedMeshGenCount() {
        return false;
    }

    @Override
    public void setUninitializedMeshGenCount() {

    }

}
