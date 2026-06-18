package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.rendering.IZoneRenderer;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Sky;
import finalforeach.cosmicreach.world.Zone;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesh;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class BerylliumZoneRenderer implements IZoneRenderer {

    private final Object2ObjectMap<Chunk, LayeredChunkMesh> meshes = Object2ObjectMaps.synchronize(new Object2ObjectLinkedOpenHashMap<>());

    public BerylliumZoneRenderer() {
        ChunkMesher.init();
    }

    @Override
    public void dispose() {
        for (LayeredChunkMesh value : meshes.values()) {
            for (ChunkMesh layer : value.getLayers()) {
                if (layer != null) layer.dispose();
            }
        }
        meshes.clear();
    }

    private final Matrix4 matrix4 = new Matrix4();
    private final ObjectList<LayeredChunkMesh> snapshotMeshLayerList = new ObjectArrayList<>();
    private ObjectList<ChunkMesh>[] multiLayerMeshList;

    int sunDirectionLoc = -1;
    int ambientWorldColorLoc = -1;
    int ambientSkyColorLoc = -1;
    int camPosLoc = -1;

    Vector3 tmp = new Vector3();

    boolean updated = true;

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

        if (updated) {
            ObjectCollection<LayeredChunkMesh> layeredChunkMeshes = meshes.values();

            snapshotMeshLayerList.clear();
            try {
                snapshotMeshLayerList.addAll(layeredChunkMeshes);
            } catch (Exception ignore) {}
            for (ObjectList<ChunkMesh> chunkMeshes : multiLayerMeshList) {
                chunkMeshes.clear();
            }
            for (LayeredChunkMesh layeredChunkMesh : snapshotMeshLayerList) {
                if (layeredChunkMesh == null) continue;

                for (int i = 0; i < RenderLayers.LAYER_ORDER.length; i++) {
                    multiLayerMeshList[i].add(layeredChunkMesh.getLayer(i));
                }
            }

            for (ObjectList<ChunkMesh> chunkMeshes : multiLayerMeshList) {
                chunkMeshes.sort((a, b) -> {
                    Chunk aChunk = a.getParent().getChunk();
                    Chunk bChunk = b.getParent().getChunk();

                    float aDst = Vector3.dst2(
                            aChunk.blockX, aChunk.blockY, aChunk.blockZ,
                            camera.position.x, camera.position.y, camera.position.z
                    );
                    float bDst = Vector3.dst2(
                            bChunk.blockX, bChunk.blockY, bChunk.blockZ,
                            camera.position.x, camera.position.y, camera.position.z
                    );
                    return Float.compare(aDst, bDst);
                });
            }

            updated = false;
        }

        for (int i = 0; i < RenderLayers.LAYER_ORDER.length; i++) {
            RenderLayer renderLayer = RenderLayers.LAYER_ORDER[i];
            ObjectList<ChunkMesh> meshObjectList = multiLayerMeshList[i];

            BerylliumShaderProgram program = renderLayer.getProgram();
            program.bind();
            if (sunDirectionLoc == -1)
                sunDirectionLoc = program.getUniformLocation("u_sunDirection");
            if (ambientWorldColorLoc == -1)
                ambientWorldColorLoc = program.getUniformLocation("u_ambientWorldColor");
            if (ambientSkyColorLoc == -1)
                ambientSkyColorLoc = program.getUniformLocation("u_ambientSkyColor");
            if (camPosLoc == -1)
                camPosLoc = program.getUniformLocation("u_cameraPos");

            Sky.currentSky.getSunDirection(tmp);
            if (sunDirectionLoc != -1) GL20.glUniform3f(sunDirectionLoc, tmp.x, tmp.y, tmp.z);
            Color ambientWorldColor = Sky.currentSky.currentAmbientColor;
            if (ambientWorldColorLoc != -1) GL20.glUniform3f(ambientWorldColorLoc, ambientWorldColor.r, ambientWorldColor.g, ambientWorldColor.b);
            Color ambientSkyColor = Sky.currentSky.currentSkyColor;
            if (ambientSkyColorLoc != -1) GL20.glUniform3f(ambientSkyColorLoc, ambientSkyColor.r, ambientSkyColor.g, ambientSkyColor.b);
            if (camPosLoc != -1) GL20.glUniform3f(camPosLoc, camera.position.x, camera.position.y, camera.position.z);

            for (ChunkMesh chunkMesh : meshObjectList) {
                if (!chunkMesh.getParent().canRenderOldMesh()) {
                    if (!chunkMesh.getParent().isFinished()) continue;
                }

                Chunk chunk = chunkMesh.getParent().getChunk();

                matrix4.idt();
                matrix4.translate(chunk.blockX, chunk.blockY, chunk.blockZ);
//            matrix4.scl(Math.max(chunkMesh.scale / 2, 1));
                chunkMesh.render(camera, renderLayer, matrix4, false);
            }
        }
    }

    @Override
    public void removeRegion(Region region) {

    }

    @Override
    public void unload() {
        updated = true;
        BerylliumMeshingThread.clear();
    }

    @Override
    public String getName() {
        return "BerylliumRenderer";
    }

    @Override
    public void removeChunk(Chunk chunk) {
        updated = true;
        LayeredChunkMesh c = meshes.remove(chunk);
        if (c == null) return;
        c.scheduleForDisposal();
        Gdx.app.postRunnable(c::dispose);
    }

    private void queueOrCreate(Chunk chunk, boolean immediate) {
        LayeredChunkMesh mesh = meshes.get(chunk);
        if (mesh != null && !mesh.isScheduledForDisposal()) {
            BerylliumMeshingThread.queueChunk(meshes.get(chunk), immediate);
        } else {
            meshes.put(chunk, BerylliumMeshingThread.queueChunk(chunk, immediate));
        }
    }

    @Override public void onChunkFlaggedForRemeshing(Chunk chunk) {
        queueOrCreate(chunk, true);
    }
    @Override public void addChunk(Chunk chunk) {
        queueOrCreate(chunk, false);
    }
    @Override
    public void onChunkMeshed(Chunk chunk) {

    }
}
