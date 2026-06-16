package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.rendering.IZoneRenderer;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Sky;
import finalforeach.cosmicreach.world.Zone;
import it.unimi.dsi.fastutil.objects.*;
import me.zombii.beryllium.client.rendering.BerylliumMesh;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesh;
import me.zombii.beryllium.common.BerylliumCommon;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

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

    int sunDirectionLoc = -1;
    int ambientWorldColorLoc = -1;
    int ambientSkyColorLoc = -1;
    int camPosLoc = -1;

    Vector3 tmp = new Vector3();

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
        try {
            meshList.addAll(chunkMeshes);
        } catch (Exception ignore) {}

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

        for (ChunkMesh chunkMesh : meshList) {
            if (chunkMesh == null) continue;
            if (!chunkMesh.isFinished()) continue;
            Chunk chunk = chunkMesh.getChunk();

            matrix4.idt();
            matrix4.translate(chunk.blockX, chunk.blockY, chunk.blockZ);
//            matrix4.scl(Math.max(chunkMesh.scale / 2, 1));
            chunkMesh.render(camera, renderLayer, matrix4, false);
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
        c.scheduleForDisposal();
        Gdx.app.postRunnable(c::dispose);
    }

    private void queueOrCreate(Chunk chunk) {
        ChunkMesh mesh = meshes.get(chunk);
        if (mesh != null && !mesh.isScheduledForDisposal()) {
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
