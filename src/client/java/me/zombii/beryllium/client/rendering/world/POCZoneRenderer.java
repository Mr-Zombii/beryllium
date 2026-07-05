package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.rendering.IZoneRenderer;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.client.rendering.tessellation.BerylliumMeshUniformMaterial;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesh;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;
import org.lwjgl.opengl.GL11;

public class POCZoneRenderer implements IZoneRenderer {

    private final ObjectSet<Chunk> chunks = ObjectSets.synchronize(new ObjectOpenHashSet<>());

    public POCZoneRenderer() {
        ChunkMesher.init();
    }

    @Override
    public void dispose() {
        for (Chunk chunk : chunks) {
            chunk.dispose();
        }
        chunks.clear();
    }

    private final Matrix4 matrix4 = new Matrix4();
    private final ObjectList<Chunk> snapshotMeshLayerList = new ObjectArrayList<>();
    private ObjectList<ChunkMesh>[] multiLayerMeshList;

    private static final BerylliumMeshUniformMaterial material = new BerylliumMeshUniformMaterial(true);

    @Override
    public void render(Zone zone, Camera camera) {
        if (multiLayerMeshList == null) {
            multiLayerMeshList = new ObjectList[RenderLayers.LAYER_ORDER.length];
            for (int i = 0; i < multiLayerMeshList.length; i++) {
                multiLayerMeshList[i] = new ObjectArrayList<>();
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glCullFace(GL11.GL_BACK);

        for (ObjectList<ChunkMesh> chunkMeshes : multiLayerMeshList) {
            chunkMeshes.clear();
        }
        snapshotMeshLayerList.clear();
        synchronized (chunks) {
            snapshotMeshLayerList.addAll(chunks);
        }

        snapshotMeshLayerList.sort((a, b) -> {
            float aDst = Vector3.dst2(
                    a.blockX, a.blockY, a.blockZ,
                    camera.position.x, camera.position.y, camera.position.z
            );
            float bDst = Vector3.dst2(
                    b.blockX, b.blockY, b.blockZ,
                    camera.position.x, camera.position.y, camera.position.z
            );
            return Float.compare(aDst, bDst);
        });

        for (Chunk c : snapshotMeshLayerList) {
            if (c == null || c.getMeshGroup() == null) continue;

            ((LayeredChunkMesh)c.getMeshGroup().getAllMeshData()).setChunk(c);
            for (int i = 0; i < RenderLayers.LAYER_ORDER.length; i++) {
                multiLayerMeshList[i].add(((LayeredChunkMesh)c.getMeshGroup().getAllMeshData()).getLayer(i));
            }
        }

        for (int i = 0; i < RenderLayers.LAYER_ORDER.length; i++) {
            RenderLayer renderLayer = RenderLayers.LAYER_ORDER[i];
            ObjectList<ChunkMesh> meshObjectList = multiLayerMeshList[i];

            BerylliumShaderProgram program = renderLayer.getProgram();
            program.bind();
            material.bind(program, camera);

            boolean wasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            if (renderLayer.usesDepthBuffer()) {
                if (!wasEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
            else if (wasEnabled) GL11.glDisable(GL11.GL_DEPTH_TEST);

            for (ChunkMesh chunkMesh : meshObjectList) {
                if (chunkMesh.isDisposed() || chunkMesh.getParent().isScheduledForDisposal()) continue;
                if (!chunkMesh.getParent().canRenderOldMesh() && !chunkMesh.getParent().isFinished()) {
                    continue;
                }
                chunkMesh.updateDirty();

                Chunk chunk = chunkMesh.getParent().getChunk();

                matrix4.idt();
                matrix4.translate(chunk.blockX, chunk.blockY, chunk.blockZ);
//            matrix4.scl(Math.max(chunkMesh.scale / 2, 1));
                chunkMesh.render(camera, program, matrix4);
            }

            if (wasEnabled) {
                if (!renderLayer.usesDepthBuffer()) GL11.glEnable(GL11.GL_DEPTH_TEST);
            } else if (renderLayer.usesDepthBuffer()) GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
    }

    @Override
    public void removeRegion(Region region) {

    }

    @Override
    public void unload() {
        POCMeshingThread.clear();
    }

    @Override
    public String getName() {
        return "BerylliumRenderer";
    }

    @Override
    public void removeChunk(Chunk chunk) {
        chunks.remove(chunk);
        if (chunk.getMeshGroup() == null) return;
        Gdx.app.postRunnable(chunk::dispose);
    }

    @Override
    public void onChunkFlaggedForRemeshing(Chunk chunk) {
        chunks.add(chunk);
        POCMeshingThread.queueChunk(chunk);
    }
    @Override
    public void addChunk(Chunk chunk) {
        chunks.add(chunk);
        POCMeshingThread.queueChunk(chunk);
    }
    @Override
    public void onChunkMeshed(Chunk chunk) {

    }
}
