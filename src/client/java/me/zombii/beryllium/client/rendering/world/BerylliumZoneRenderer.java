package me.zombii.beryllium.client.rendering.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Frustum;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.*;
import finalforeach.cosmicreach.rendering.*;
import finalforeach.cosmicreach.singletons.GameSingletons;
import finalforeach.cosmicreach.util.ArrayUtils;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.RegionOctant;
import finalforeach.cosmicreach.world.Zone;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.client.rendering.tessellation.BerylliumMeshUniformMaterial;
import me.zombii.beryllium.client.rendering.world.chunk.BerylliumChunkBatch;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesh;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.LayeredChunkMesh;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class BerylliumZoneRenderer implements IZoneRenderer {
    private final IdentityMap<Region, Array<Chunk>> regionChunksToRender = new IdentityMap<>();
    private final Array<Region> regionsToRender = new Array<>(false, 16, Region.class);
    private final IntMap<Array<BerylliumChunkBatch>> layers = new IntMap<>();
    public IntMap<BerylliumChunkBatch> batchMap = new IntMap<>();
    private final IntSet seenLayerNums = new IntSet();
    private Vector3 lastCameraPosition;
    private Vector3 lastCameraDirection;
    private final ObjectIntMap<Region> numChunksPerRegion = new ObjectIntMap<>();
    private boolean gotNewChunksToRender;
    private final Array<Chunk> chunksToNoLongerRender = new Array<>();
    private int lastLightingVisionMode = 0;
    private static final Vector3 tmpVec = new Vector3();
    private static final Vector3 tmpBatchCoords = new Vector3();
    private static final BoundingBox tmpBounds = new BoundingBox();
    private static final ObjectSet<Chunk> tmpLastRendered = new ObjectSet<>();
    boolean haveChunksToRemove;

    private static final BerylliumMeshUniformMaterial material = new BerylliumMeshUniformMaterial(true);

    public BerylliumZoneRenderer(){
        ChunkMesher.init();
    }

    @Override
    public void dispose() {
        IWorldRenderingMeshGenThread meshGenThread = GameSingletons.meshGenThread;
        meshGenThread.unload();
        meshGenThread.stopThread();
    }

    @Override
    public void render(Zone zone, Camera worldCamera) {
        this.prepare(zone, worldCamera);
        this.renderLayerRange(zone, worldCamera, 0, RenderLayers.LAYER_ORDER.length - 1);
    }

    @Override
    public void removeRegion(Region region) {
        this.numChunksPerRegion.remove(region, 0);
        this.regionChunksToRender.remove(region);
        this.regionsToRender.removeValue(region, true);
    }

    @Override
    public void unload() {
        this.disposeUnusedBatches(true);
        IWorldRenderingMeshGenThread thread = GameSingletons.meshGenThread;
        thread.unload();
    }

    @Override
    public String getName() {
        return "Beryllium renderer";
    }

    @Override
    public void onChunkFlaggedForRemeshing(Chunk chunk) {
        if (chunk.isGenerated()) {
            IChunkMeshGroup<?> meshGroup = chunk.getMeshGroup();
            if (meshGroup != null) {
                IWorldRenderingMeshGenThread meshGenThread = GameSingletons.meshGenThread;
                synchronized(meshGenThread.getAddChunkLock()) {
                    meshGroup.flushRemeshRequests();
                    meshGenThread.addChunk(chunk);
                }

                this.haveChunksToRemove = true;
                Region region = chunk.region;
                if (region != null) {
                    RegionOctant octant = region.getOctant(chunk);
                    Array<Chunk> m = octant.getMeshedChunks();
                    if (!m.contains(chunk, true)) {
                        m.add(chunk);
                    }
                }

            }
        }
    }

    @Override
    public void removeChunk(Chunk chunk) {
        this.haveChunksToRemove = true;
    }

    @Override
    public void addChunk(Chunk chunk) {
        chunk.initMeshGroup(BerylliumMeshGroup::new);
    }

    @Override
    public void onChunkMeshed(Chunk chunk) {
        this.haveChunksToRemove = true;
    }

    private void disposeUnusedBatches(boolean unloadAll){
        boolean haveChunksToRemove = this.haveChunksToRemove;
        this.haveChunksToRemove = false;
        if (haveChunksToRemove || unloadAll){
            Iterator<BerylliumChunkBatch> batches = this.batchMap.values().iterator();

            while (batches.hasNext()){
                BerylliumChunkBatch batch = batches.next();

                if (!batch.seen || unloadAll){
                    for (IChunkMeshGroup<?> meshGroup : batch.chunkMeshGroups){
                        if (unloadAll || !meshGroup.hasMesh() || meshGroup.isAllMeshDataEmpty() || meshGroup.isFlaggedForRemeshing()) {
                            batch.dispose(unloadAll);
                            break;
                        }
                    }
                }

                if (batch.disposed){
                    batch.layer.removeValue(batch, true);
                    batches.remove();
                }
            }
        }
    }

    private boolean getChunksToRenderShouldEarlyExit(Camera worldCamera) {
        this.gotNewChunksToRender = false;
        if (this.lastCameraPosition == null) {
            this.lastCameraPosition = new Vector3(worldCamera.position);
            this.lastCameraDirection = new Vector3(worldCamera.direction);
        } else if (this.lastCameraPosition.epsilonEquals(worldCamera.position) && this.lastCameraDirection.epsilonEquals(worldCamera.direction)) {
            return !this.haveChunksToRemove && !this.gotNewChunksToRender && !ChunkMeshGroup.setMeshGenRecently;
        }

        return false;
    }

    private boolean regionInBounds(Camera worldCamera, Region r) {
        return r.boundingBox.contains(worldCamera.position) || worldCamera.frustum.boundsInFrustum(r.boundingBox);
    }

    private boolean octantInBounds(Camera worldCamera, Region r, RegionOctant octant, BoundingBox tmpBounds) {
        octant.getBounds(r, tmpBounds);
        return worldCamera.frustum.boundsInFrustum(tmpBounds);
    }

    private boolean chunkBatchInBounds(Camera worldCamera, Vector3 batchCoords) {
        Frustum frustum = worldCamera.frustum;
        float halfW = 32.0F;
        return frustum.boundsInFrustum(batchCoords.x * 64 + halfW, batchCoords.y * 64 + halfW, batchCoords.z * 64 + halfW, halfW, halfW, halfW);
    }

    private boolean boundsContainedInView(Camera worldCamera, BoundingBox box) {
        Frustum frustum = worldCamera.frustum;
        tmpVec.set(box.min);
        if (!frustum.pointInFrustum(tmpVec)) {
            return false;
        } else {
            tmpVec.add(box.max.x - box.min.x, 0.0F, 0.0F);
            if (!frustum.pointInFrustum(tmpVec)) {
                return false;
            } else {
                tmpVec.set(box.min).add(0.0F, box.max.y - box.min.y, 0.0F);
                if (!frustum.pointInFrustum(tmpVec)) {
                    return false;
                } else {
                    tmpVec.set(box.min).add(0.0F, 0.0F, box.max.z - box.min.z);
                    if (!frustum.pointInFrustum(tmpVec)) {
                        return false;
                    } else {
                        tmpVec.set(box.max);
                        if (!frustum.pointInFrustum(tmpVec)) {
                            return false;
                        } else {
                            tmpVec.add(box.min.x - box.max.x, 0.0F, 0.0F);
                            if (!frustum.pointInFrustum(tmpVec)) {
                                return false;
                            } else {
                                tmpVec.set(box.max).add(0.0F, box.min.y - box.max.y, 0.0F);
                                if (!frustum.pointInFrustum(tmpVec)) {
                                    return false;
                                } else {
                                    tmpVec.set(box.max).add(0.0F, 0.0F, box.min.z - box.max.z);
                                    return frustum.pointInFrustum(tmpVec);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void getChunksToRender(Zone zone, Camera worldCamera){
        if (!this.getChunksToRenderShouldEarlyExit(worldCamera)) {
            this.lastCameraPosition.set(worldCamera.position);
            this.lastCameraDirection.set(worldCamera.direction);

            for(Region r : zone.getRegions()) {
                Array<Chunk> chunksToRender = this.regionChunksToRender.get(r);
                if (!this.regionInBounds(worldCamera, r)) {
                    this.regionChunksToRender.remove(r);
                    this.regionsToRender.removeValue(r, true);
                } else {
                    if (chunksToRender == null) {
                        chunksToRender = new Array<>(Chunk.class);
                        this.regionChunksToRender.put(r, chunksToRender);
                        if (!this.regionsToRender.contains(r, true)) {
                            this.regionsToRender.add(r);
                        }
                    }

                    int prevChunksToRenderSize = chunksToRender.size;
                    boolean regionContained = this.boundsContainedInView(worldCamera, r.boundingBox);
                    int numChunksInRegion = r.getNumberOfChunks();
                    ArrayUtils.clearAndReplace(this.chunksToNoLongerRender, chunksToRender);
                    chunksToRender.size = 0;
                    this.numChunksPerRegion.put(r, numChunksInRegion);

                    for(RegionOctant octant : r.octants) {
                        if (this.octantInBounds(worldCamera, r, octant, tmpBounds)) {
                            boolean octantContained = regionContained;
                            if (!regionContained) {
                                octant.getBounds(r, tmpBounds);
                                octantContained = this.boundsContainedInView(worldCamera, tmpBounds);
                            }

                            Array<Chunk> meshedChunks = octant.getMeshedChunks();
                            Array.ArrayIterator<Chunk> octantChunkIt = meshedChunks.iterator();
                            int batchInBoundsCalcMask = 0;
                            int batchInBoundsMask = 0;

                            while(octantChunkIt.hasNext()) {
                                Chunk chunk;
                                try {
                                    chunk = octantChunkIt.next();
                                } catch (NoSuchElementException var25) {
                                    chunk = null;
                                }

                                if (chunk != null) {
                                    IChunkMeshGroup<?> meshGroup = chunk.getMeshGroup();
                                    if ((meshGroup == null || !meshGroup.hasMesh() || meshGroup.isFlaggedForRemeshing() || !meshGroup.isAllMeshDataEmpty()) && !octantContained) {
                                        this.setTmpBatchCoordsFromChunk(chunk);
                                        int shift = Math.floorMod((int) tmpBatchCoords.x, 2) << 2 | Math.floorMod((int) tmpBatchCoords.y, 2) << 1 | Math.floorMod((int) tmpBatchCoords.z, 2);
                                        int index = 1 << shift;
                                        boolean inBounds = (batchInBoundsMask & index) != 0;
                                        if ((batchInBoundsCalcMask & index) == 0) {
                                            inBounds = this.chunkBatchInBounds(worldCamera, tmpBatchCoords);
                                            if (inBounds) {
                                                batchInBoundsMask |= index;
                                            }
                                        }

                                        batchInBoundsCalcMask |= index;
                                        if (!inBounds) {
                                            continue;
                                        }
                                    }

                                    chunksToRender.add(chunk);
                                }
                            }
                        }
                    }

                    if (prevChunksToRenderSize > chunksToRender.size) {
                        this.gotNewChunksToRender = true;
                    } else if (!this.gotNewChunksToRender) {
                        this.resetLastRendered(chunksToRender);
                    }
                }
            }
        }
    }

    private void resetLastRendered(Array<Chunk> chunksToRender) {
        tmpLastRendered.clear();
        this.chunksToNoLongerRender.forEach(tmpLastRendered::add);

        for (Chunk chunk : chunksToRender) {
            this.gotNewChunksToRender = !tmpLastRendered.contains(chunk);
            if (this.gotNewChunksToRender) {
                return;
            }
        }
    }

    private void requestMeshes() {
        GameSingletons.meshGenThread.meshChunks(this);
    }

    private void setTmpBatchCoordsFromChunk(Chunk chunk){
        tmpBatchCoords.set(Math.floorDiv(chunk.chunkX, 4), Math.floorDiv(chunk.chunkY, 4), Math.floorDiv(chunk.chunkZ, 4));
    }

    private Array<BerylliumChunkBatch> getLayer(int layerID) {
        Array<BerylliumChunkBatch> layer = this.layers.get(layerID);
        if (layer == null) {
            layer = new Array<>(BerylliumChunkBatch.class);
            this.layers.put(layerID, layer);
        }
        return layer;
    }

    private BerylliumChunkBatch getBatch(Vector3 batchCoords, int renderLayer) {
        int batchHash = Objects.hash(batchCoords.x, batchCoords.y, batchCoords.z, renderLayer);

        BerylliumChunkBatch batch = this.batchMap.get(batchHash);
        if (batch == null) {
            Array<BerylliumChunkBatch> layer = this.getLayer(renderLayer);
            batch = new BerylliumChunkBatch(batchCoords.cpy(), layer);
            this.batchMap.put(batchHash, batch);
        }

        return batch;
    }

    private void addMeshDatasToChunkBatches() {
        if (this.gotNewChunksToRender) {
            ++BerylliumChunkBatch.seenStep;
            this.seenLayerNums.clear();

            for(Region region : this.regionsToRender.items) {
                if (region == null) {
                    break;
                }

                Array<Chunk> chunksToRender = this.regionChunksToRender.get(region);

                for(int ci = 0; ci < chunksToRender.size; ++ci) {
                    Chunk chunk = ((Chunk[])chunksToRender.items)[ci];
                    this.setTmpBatchCoordsFromChunk(chunk);
                    BerylliumMeshGroup meshGroup = (BerylliumMeshGroup) chunk.getMeshGroup();
                    if (meshGroup != null) {
                        LayeredChunkMesh chunkAllMeshData = meshGroup.getAllMeshData();
                        ChunkMesh[] meshLayers = chunkAllMeshData.getLayers();

                        for(int mi = 0; mi < meshLayers.length; ++mi) {
                            ChunkMesh cm = meshLayers[mi];
                            BerylliumChunkBatch batch = this.getBatch(tmpBatchCoords, mi);
                            batch.region = chunk.region;
                            batch.addChunkMesh(chunk, cm);
                            this.seenLayerNums.add(ci);
                        }

                        RegionOctant octant = chunk.region.getOctant(chunk);
                        if (octant != null && meshGroup.hasMesh() && !meshGroup.isFlaggedForRemeshing() && meshGroup.isAllMeshDataEmpty()) {
                            try {
                                octant.getMeshedChunks().removeValue(chunk, true);
                            } catch (Exception _) {
                            }
                        }
                    }
                }
            }
        }
    }

    public void prepare(Zone zone, Camera worldCamera) {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        this.getChunksToRender(zone, worldCamera);
        this.requestMeshes();
        this.disposeUnusedBatches(false);
        this.addMeshDatasToChunkBatches();
        if (this.lastLightingVisionMode != LightingVision.getMode()) {
            this.lastLightingVisionMode = LightingVision.getMode();

            for(Region r : zone.getRegions()) {
                for(Chunk c : r.getChunks()) {
                    if (c != null) {
                        c.flagForRemeshing(false);
                    }
                }
            }
        }
    }

    public void renderLayerRange(Zone zone, Camera worldCamera, int minLayer, int maxLayer) {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for(int layerNum = Math.max(minLayer, 0); layerNum <= Math.min(maxLayer, RenderLayers.LAYER_ORDER.length - 1); ++layerNum) {
            Array<BerylliumChunkBatch> layer = this.layers.get(layerNum);
            RenderLayer renderLayer = RenderLayers.LAYER_ORDER[layerNum];
            if (layer != null) {
                Gdx.gl.glDepthMask(renderLayer.usesDepthBuffer());

                BerylliumShaderProgram program = renderLayer.getProgram();

                program.bind();
                material.bind(program, worldCamera);

                for (BerylliumChunkBatch berylliumChunkBatch : layer) {
                    berylliumChunkBatch.render(worldCamera, program);
                }
            }
        }

        Gdx.gl.glDepthMask(true);
    }
}
