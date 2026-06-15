package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.rendering.IZoneRenderer;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesh;
import me.zombii.beryllium.common.BerylliumCommon;
import org.lwjgl.opengl.GL11;
public class BerylliumZoneRenderer implements IZoneRenderer {

    private final Object2ObjectMap<Chunk, ChunkMesh> meshes = Object2ObjectMaps.synchronize(new Object2ObjectLinkedOpenHashMap<>());
    private RenderLayer renderLayer;

    @Override
    public void dispose() {
        for (ChunkMesh value : meshes.values()) {
            value.dispose();
        }
        meshes.clear();
    }

    private final Matrix4 matrix4 = new Matrix4();
    private final ObjectList<ChunkMesh> meshList = new ObjectArrayList<>();

    @Override
    public void render(Zone zone, Camera camera) {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glCullFace(GL11.GL_BACK);

        if (renderLayer == null) {
            renderLayer = RenderLayers.LAYER_REGISTRY.get(Identifier.of(BerylliumCommon.NAMESPACE, "opaque-block-render-layer"));
        }
        ObjectCollection<ChunkMesh> chunkMeshes = meshes.values();

        meshList.clear();
        meshList.addAll(chunkMeshes);

//        meshList.sort((a, b) -> {
//            Chunk aChunk = a.getChunk();
//            Chunk bChunk = b.getChunk();
//
//            float aDst = Vector3.dst2(
//                    aChunk.blockX, aChunk.blockY, aChunk.blockZ,
//                    camera.position.x, camera.position.y, camera.position.z
//            );
//            float bDst = Vector3.dst2(
//                    bChunk.blockX, bChunk.blockY, bChunk.blockZ,
//                    camera.position.x, camera.position.y, camera.position.z
//            );
////            return Float.compare(bDst, aDst);
//            return Float.compare(aDst, bDst);
//        });
        for (ChunkMesh chunkMesh : meshList) {
            if (chunkMesh == null) continue;
            if (!chunkMesh.isFinished()) continue;
//            if (!chunkMesh.isInitialized()) {
//                chunkMesh.initGL();
//            }
            Chunk chunk = chunkMesh.getChunk();

            matrix4.idt();
            matrix4.translate(chunk.blockX, chunk.blockY, chunk.blockZ);
            chunkMesh.render(camera, renderLayer, matrix4);
        }
    }

    @Override
    public void removeRegion(Region region) {

    }

    @Override
    public void unload() {
        BerylliumMeshingThread.clear();
    }

    @Override
    public String getName() {
        return "BerylliumRenderer";
    }

    @Override
    public void removeChunk(Chunk chunk) {
        ChunkMesh c = meshes.remove(chunk);
        if (c == null) return;
        Gdx.app.postRunnable(c::dispose);
    }

    private void queueOrCreate(Chunk chunk) {
        if (meshes.containsKey(chunk)) {
            if (!meshes.get(chunk).isDisposed())
                BerylliumMeshingThread.queueChunk(meshes.get(chunk));
        } else {
            meshes.put(chunk, BerylliumMeshingThread.queueChunk(chunk));
        }
    }

    @Override public void onChunkFlaggedForRemeshing(Chunk chunk) { queueOrCreate(chunk); }
    @Override public void addChunk(Chunk chunk) { queueOrCreate(chunk); }
    @Override
    public void onChunkMeshed(Chunk chunk) {

    }
}
