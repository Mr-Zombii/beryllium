package me.zombii.beryllium.client.rendering.world.chunk;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Zone;
import me.zombii.beryllium.client.IdentifierCache;
import me.zombii.beryllium.client.rendering.TintProvider;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import me.zombii.beryllium.client.rendering.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.model.loading.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.model.loading.baking.BakedFace;
import me.zombii.beryllium.client.rendering.model.loading.baking.ModelBaker;
import me.zombii.beryllium.client.rendering.model.loading.baking.Tessallator;

import java.util.Arrays;
import java.util.function.Function;

public class ChunkMesher {

    public static final int quadsPerChunk = 16 * 16 * 16 * BakedBerylliumModel.MAX_FACES_PER_MODEL;
    private static final ReusableTintGetter tintFunction = new ReusableTintGetter();
    private static Tessallator globalTessallator;
    private static boolean initialized = false;
    private static final CrossChunkAccessor crossChunkAccessor = new CrossChunkAccessor();
    private static Matrix4 transform;

    public static void init() {
        if (initialized) return;
        initialized = true;

        globalTessallator = new Tessallator(quadsPerChunk);
        transform = globalTessallator.getTransform();

        generateAoTable();
    }

    private static final short[] TMP_SKY_LIGHT = new short[6];
    private static final short[] TMP_BLOCK_LIGHT = new short[6];
    private static final byte[] TMP_AO_VALUES = new byte[4 * 6];

    public static void meshChunk(Chunk chunk, LayeredChunkMesh chunkMesh) {
        for (int i = 0; i < RenderLayers.LAYER_ORDER.length; i++) {
            RenderLayer layer = RenderLayers.LAYER_ORDER[i];
            meshChunk(layer, chunk, chunkMesh.getLayers()[i]);
        }
    }

    public static void meshChunk(RenderLayer layer, Chunk chunk, ChunkMesh mesh) {
        if (mesh == null) return;
        if (mesh.isDisposed()) return;
        globalTessallator.reset();
        mesh.clear();

//        int levelOfDetail = 0;
//        int blockSize = 1 << levelOfDetail;

        if (chunk.region == null) return;
        Zone zone = chunk.getZone();
        crossChunkAccessor.init(zone, chunk);

        for (int x = 0; x < 16; x += 1) {
            for (int y = 0; y < 16; y += 1) {
                for (int z = 0; z < 16; z += 1) {
                    BlockState self = chunk.getBlockState(x, y, z);

                    // skip model-less blocks
                    if (self == null) continue;
                    if (self.hasEmptyModel()) continue;

                    BerylliumModel model = BerylliumModelLoader.getModel(getModelId(self));
                    if (model.getRenderLayer() != layer) continue;

                    int visibleFaces = getVisibleFaces(self, model, x, y, z);
                    getSkyLight(TMP_SKY_LIGHT, x, y, z);
                    getBlockLight(TMP_BLOCK_LIGHT, x, y, z);
                    getAmbientOcclusion(TMP_AO_VALUES, x, y, z);


                    //The lambda expression was allocating a new Function<> for every single block
                    TintProvider.TintFunction altTintFunction = TintProvider.getForState(self.getBlock());
                    tintFunction.set(altTintFunction, self, chunk, x,y,z);


                    BakedBerylliumModel bakedModel = ModelBaker.get(model);

                    transform.idt();
                    transform.translate(.5f + x, .5f + y, .5f + z);
                    transform.rotate(Vector3.Z, self.rotation[2]);
                    transform.rotate(Vector3.Y, 360-self.rotation[1]);
                    transform.rotate(Vector3.X, 360-self.rotation[0]);
                    transform.translate(-.5f - x, -.5f - y, -.5f - z);

                    bakedModel.addVertices(
                            globalTessallator,
                            TMP_SKY_LIGHT, TMP_BLOCK_LIGHT, TMP_AO_VALUES,
                            visibleFaces, tintFunction,
                            x, y, z
                    );
                }
            }
        }
        if (mesh.getParent().isScheduledForDisposal()) return;
        if (globalTessallator.getQuadsWritten() == 0) {
            mesh.clear();
            return;
        }
        mesh.resize(globalTessallator.getQuadsWritten());
        mesh.dump(globalTessallator, true);
    }

    private static int getVisibleFaces(BlockState self, BerylliumModel model, int x, int y, int z) {
        int visibilityMask = BakedFace.NO_CULL_FACES;

        BlockState nx = crossChunkAccessor.getBlockState(tmp.set(-1,  0,  0), x, y, z);
        BlockState px = crossChunkAccessor.getBlockState(tmp.set( 1,  0,  0), x, y, z);
        BlockState ny = crossChunkAccessor.getBlockState(tmp.set( 0, -1,  0), x, y, z);
        BlockState py = crossChunkAccessor.getBlockState(tmp.set( 0,  1,  0), x, y, z);
        BlockState nz = crossChunkAccessor.getBlockState(tmp.set( 0,  0, -1), x, y, z);
        BlockState pz = crossChunkAccessor.getBlockState(tmp.set( 0,  0,  1), x, y, z);

        visibilityMask |= isOccluded(0,1, self, model, nx) ? 0 : BakedFace.NEG_X_SHOWING;
        visibilityMask |= isOccluded(1,0, self, model, px) ? 0 : BakedFace.POS_X_SHOWING;
        visibilityMask |= isOccluded(2,3, self, model, ny) ? 0 : BakedFace.NEG_Y_SHOWING;
        visibilityMask |= isOccluded(3,2, self, model, py) ? 0 : BakedFace.POS_Y_SHOWING;
        visibilityMask |= isOccluded(4,5, self, model, nz) ? 0 : BakedFace.NEG_Z_SHOWING;
        visibilityMask |= isOccluded(5,4, self, model, pz) ? 0 : BakedFace.POS_Z_SHOWING;

        return visibilityMask;
    }

    static final int CORNER_NX_NZ = 0;
    static final int CORNER_NX_PZ = 1;
    static final int CORNER_PX_NZ = 2;
    static final int CORNER_PX_PZ = 3;

    static final int SIDE_NZ = 0;
    static final int SIDE_NX = 1;
    static final int SIDE_PZ = 2;
    static final int SIDE_PX = 3;

    static final int[] cornerTable = {
            -1, -1,
            -1,  1,
             1, -1,
             1,  1
    };

    static final int[] sideTable = {
             0, -1,
            -1,  0,
             0,  1,
             1,  0
    };

    static final int[] aoCornerTables = new int[(4 * 3) * 6];
    static final int[] aoSideTables = new int[(4 * 3) * 6];

    public static void generateAoTable() {
        for (int d = 0; d < 6; d++) {
            for (int c = 0; c < 4; c++) {
                generateAoTableEntry(aoCornerTables, cornerTable, d, c);
                generateAoTableEntry(aoSideTables, sideTable, d, c);
            }
        }
    }

    public static void generateAoTableEntry(
            int[] table, int[] source,
            int direction, int entry
    ) {
        int idx = (direction * 12) + (entry * 3);
        int a = source[(entry * 2)];
        int b = source[(entry * 2) + 1];

        int x = 0;
        int y = 0;
        int z = 0;

        switch (direction) {
            case 0 -> {
                x = -1;
                y = b;
                z = -a;
            }
            case 1 -> {
                x = 1;
                y = b;
                z = a;
            }
            case 2 -> {
                x = -a;
                y = -1;
                z = b;
            }
            case 3 -> {
                x = -a;
                y = 1;
                z = -b;
            }
            case 4 -> {
                x = a;
                y = b;
                z = -1;
            }
            case 5 -> {
                x = -a;
                y = b;
                z = 1;
            }
        }

        table[idx] = x;
        table[idx + 1] = y;
        table[idx + 2] = z;
    }

    static final Vector3 tmp = new Vector3();

    private static Vector3 getOffsEntry(int[] table, int direction, int entry) {
        int idx = (direction * 12) + (entry * 3);
        return tmp.set(table[idx], table[idx + 1], table[idx + 2]);
    }

    private static int getCornerAO(
            int x, int y, int z,
            int direction, int corner, int sideA, int sideB
    ) {
        Vector3 sideAOffs = getOffsEntry(aoSideTables, direction, sideA);
        BlockState sideABlock = crossChunkAccessor.getBlockState(sideAOffs, x, y, z);
        Vector3 sideBOffs = getOffsEntry(aoSideTables, direction, sideB);
        BlockState sideBBlock = crossChunkAccessor.getBlockState(sideBOffs, x, y, z);
        Vector3 cornerOffs = getOffsEntry(aoCornerTables, direction, corner);
        BlockState cornerBlock = crossChunkAccessor.getBlockState(cornerOffs, x, y, z);

        int activeSideA = (sideABlock != null && sideABlock.isOpaque && !sideABlock.hasEmptyModel()) ? 1 : 0;
        int activeSideB = (sideBBlock != null && sideBBlock.isOpaque && !sideBBlock.hasEmptyModel()) ? 1 : 0;
        int activeCorner = (cornerBlock != null && cornerBlock.isOpaque && !cornerBlock.hasEmptyModel()) ? 1 : 0;

        if (activeSideA == 1 && activeSideB == 1) return 0;
        return 3 - (activeSideA + activeSideB + activeCorner);
    }

    private static void getAmbientOcclusion(
            byte[] aoValues,
            int x, int y, int z
    ) {
        Arrays.fill(aoValues, (byte) 0);

        for (int d = 0; d < 6; d++) {
            int C00 = getCornerAO(x, y, z, d, CORNER_NX_NZ, SIDE_NX, SIDE_NZ);
            int C01 = getCornerAO(x, y, z, d, CORNER_NX_PZ, SIDE_NX, SIDE_PZ);
            int C10 = getCornerAO(x, y, z, d, CORNER_PX_NZ, SIDE_PX, SIDE_NZ);
            int C11 = getCornerAO(x, y, z, d, CORNER_PX_PZ, SIDE_PX, SIDE_PZ);

            aoValues[d * 4] = (byte) C00;
            aoValues[(d * 4) + 1] = (byte) C01;
            aoValues[(d * 4) + 2] = (byte) C10;
            aoValues[(d * 4) + 3] = (byte) C11;
        }
    }

    private static void getSkyLight(
            short[] skyLight,
            int x, int y, int z
    ) {
        Arrays.fill(skyLight, (short) 0);

        skyLight[0] = crossChunkAccessor.getSkyLight(tmp.set(-1,  0,  0), x, y, z);
        skyLight[1] = crossChunkAccessor.getSkyLight(tmp.set( 1,  0,  0), x, y, z);
        skyLight[2] = crossChunkAccessor.getSkyLight(tmp.set( 0, -1,  0), x, y, z);
        skyLight[3] = crossChunkAccessor.getSkyLight(tmp.set( 0,  1,  0), x, y, z);
        skyLight[4] = crossChunkAccessor.getSkyLight(tmp.set( 0,  0, -1), x, y, z);
        skyLight[5] = crossChunkAccessor.getSkyLight(tmp.set( 0,  0,  1), x, y, z);
    }

    private static void getBlockLight(
            short[] blockLight,
            int x, int y, int z
    ) {
        Arrays.fill(blockLight, (short) 0);

        blockLight[0] = crossChunkAccessor.getBlockLight(tmp.set(-1,  0,  0), x, y, z);
        blockLight[1] = crossChunkAccessor.getBlockLight(tmp.set( 1,  0,  0), x, y, z);
        blockLight[2] = crossChunkAccessor.getBlockLight(tmp.set( 0, -1,  0), x, y, z);
        blockLight[3] = crossChunkAccessor.getBlockLight(tmp.set( 0,  1,  0), x, y, z);
        blockLight[4] = crossChunkAccessor.getBlockLight(tmp.set( 0,  0, -1), x, y, z);
        blockLight[5] = crossChunkAccessor.getBlockLight(tmp.set( 0,  0,  1), x, y, z);
    }

    private static Identifier getModelId(BlockState state) {
        return IdentifierCache.getOrInsert(state.modelName);
    }

    private static boolean isOccluded(int d, int od, BlockState self, BerylliumModel selfModel, BlockState state) {
        if (state == null || state.hasEmptyModel()) return false;
        if (self.getBlock().equals(state.getBlock()) && self.cullsSelf()) return true;

        BerylliumModel stateModel = BerylliumModelLoader.getModel(getModelId(state));
        if (stateModel.isTransparent() || selfModel.isTransparent()) return false;
        return selfModel.canAllCullInDirection(d) && stateModel.canAllCullInDirection(od);
    }

//    private static final Object2IntMap<BlockState> stateAbundanceMap = new Object2IntArrayMap<>();
//
//    private static BlockState getMostAbundant(
//            int x, int y, int z,
//            int blockSize,
//            Chunk chunk
//    ) {
//        if (blockSize == 1) return chunk.getBlockState(x, y, z);
//        stateAbundanceMap.clear();
//
//        for (int bx = x; bx < x + blockSize - 1; bx++) {
//            for (int by = y; by < y + blockSize - 1; by++) {
//                for (int bz = z; bz < z + blockSize - 1; bz++) {
//                    BlockState state = chunk.getBlockState(bx, by, bz);
//                    stateAbundanceMap.put(state, stateAbundanceMap.getOrDefault(state, 0) + 1);
//                }
//            }
//        }
//
//        BlockState blockState = chunk.getBlockState(x, y, z);
//        int largest = 0;
//        for (BlockState state : stateAbundanceMap.keySet()) {
//            int i = stateAbundanceMap.getInt(state);
//            if (i > largest) {
//                largest = i;
//                blockState = state;
//            }
//        }
//
//        return blockState;
//    }
static final class ReusableTintGetter implements Function<Integer, Short> {
    private TintProvider.TintFunction tintFunction;
    private BlockState self;
    private BlockPosition scratch = new BlockPosition();

    public ReusableTintGetter() {
        this.tintFunction = TintProvider.DEFAULT_TINT_FUNCTION;
    }

    void set(TintProvider.TintFunction tintFunction, BlockState self, Chunk chunk, int x, int y, int z) {
        this.tintFunction = tintFunction;
        this.self = self;
        this.scratch.set(chunk, x, y, z);

    }

    @Override
    public Short apply(Integer idx) {
        return tintFunction.getTint(self, scratch, idx);
    }
}
}
