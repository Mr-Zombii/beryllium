package me.zombii.beryllium.client.rendering.world.chunk;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import finalforeach.cosmicreach.rendering.IChunkMeshGroup;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.client.rendering.tessellation.DefaultBerylliumMesh;

public class BerylliumChunkBatch {
    public final Array<BerylliumChunkBatch> layer;
    Array<ChunkMesh> chunkMeshesToAdd = new Array<>(false, 4, ChunkMesh.class);
    public Array<IChunkMeshGroup<?>> chunkMeshGroups = new Array<>(false, 4, IChunkMeshGroup.class);
    DefaultBerylliumMesh mesh = new DefaultBerylliumMesh(128, true);
    BoundingBox boundingBox;
    public boolean seen;
    public boolean disposed;
    boolean needToRebuild = true;
    public Vector3 batchPosition;
    long seenCount;
    public Region region;
    static public long seenStep;

    static Matrix4 matrix4 = new Matrix4();

    public BerylliumChunkBatch(Vector3 pos, Array<BerylliumChunkBatch> layer){
        this.batchPosition = pos;
        this.layer = layer;
        this.boundingBox = new BoundingBox();
        this.boundingBox.min.set(pos).scl(4 * 16);
        this.boundingBox.max.set(this.boundingBox.min).add(64f);
        this.boundingBox.update();
        layer.add(this);
        this.seen = true;
    }

    public void dispose(boolean forceUnload){
        if (this.disposed){
            return;
        }
        if (this.mesh != null){
            if (!forceUnload){
                this.mesh.dispose();
            }
        }
        this.disposed = true;
        this.chunkMeshGroups.clear();
        this.layer.removeValue(this, true);
    }

    public void addChunkMesh(Chunk chunk, ChunkMesh chunkMesh){
        IChunkMeshGroup<?> meshGroup = chunk.getMeshGroup();
        if (!this.chunkMeshGroups.contains(meshGroup, true)) {
            this.chunkMeshGroups.add(meshGroup);
        }

        this.needToRebuild = true;
        this.chunkMeshesToAdd.add(chunkMesh);
        this.seen = true;
        this.seenCount = seenStep;
    }

    public void render(Camera worldCamera, BerylliumShaderProgram shader){
        if (this.seenCount == seenStep){
            if (this.needToRebuild){
                this.rebuildMesh();
                this.needToRebuild = false;
            }

            this.mesh.updateDirty();

            matrix4.idt();
            matrix4.translate(this.boundingBox.min);
            this.mesh.render(worldCamera, shader, matrix4);
        }

        this.seen = false;
        this.chunkMeshesToAdd.clear();
    }

    void rebuildMesh(){
        for (ChunkMesh newMesh : this.chunkMeshesToAdd){
            this.mesh.merge(newMesh);
        }
    }
}
