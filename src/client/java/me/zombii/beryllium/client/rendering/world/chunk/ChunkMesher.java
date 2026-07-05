package me.zombii.beryllium.client.rendering.world.chunk;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.model.baking.ModelBaker;
import me.zombii.beryllium.client.model.baking.parts.BakedFace;
import me.zombii.beryllium.client.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;
import me.zombii.beryllium.client.rendering.tessellation.minitess.BlockTessallator;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;
import me.zombii.beryllium.client.rendering.tessellation.minitess.DefaultBlockTessallator;
import me.zombii.beryllium.client.rendering.tessellation.minitess.DefaultRotatedBlockTessallator;
import me.zombii.beryllium.client.rendering.util.IBerylliumBlockState;

import java.util.Arrays;

public class ChunkMesher {

    public static final int quadsPerChunk = 16 * 16 * 16 * BakedBerylliumModel.MAX_FACES_PER_MODEL;

    private static Tessallator globalTessallator;
    private static boolean initialized = false;
    private static final CrossChunkAccessor crossChunkAccessor = new CrossChunkAccessor();
    private static Matrix4 transform;
    private static final Matrix4 tmpMatrix = new Matrix4();

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

    public static void meshChunk(Chunk chunk) {
        LayeredChunkMesh chunkMesh = (LayeredChunkMesh) chunk.getMeshGroup().getAllMeshData();
        if (chunk.region == null) return;
        crossChunkAccessor.init(chunk.region.zone, chunk);

        for (int i = 0; i < RenderLayers.LAYER_ORDER.length; i++) {
            RenderLayer layer = RenderLayers.LAYER_ORDER[i];

            meshChunk(layer, chunk, chunkMesh.getLayers()[i]);
        }
    }

    private static void meshChunk(RenderLayer layer, Chunk chunk, ChunkMesh mesh) {
        if (mesh == null) return;
        if (mesh.isDisposed()) return;
        globalTessallator.reset();
        mesh.clear();

//        int levelOfDetail = 0;
//        int blockSize = 1 << levelOfDetail;

        for (int x = 0; x < 16; x += 1) {
            for (int y = 0; y < 16; y += 1) {
                for (int z = 0; z < 16; z += 1) {
                    BlockState self = chunk.getBlockState(x, y, z);

                    // skip model-less blocks
                    if (self == null) continue;
                    if (self.hasEmptyModel()) continue;

                    IBerylliumBlockState berylliumState = (IBerylliumBlockState) self;
                    BerylliumModel model = berylliumState.getModel();
                    if (!model.getRenderLayer().getId().equals(layer.getId())) continue;

                    rotateMasks(
                            self.rotation[0],
                            self.rotation[1],
                            self.rotation[2]
                    );

                    BakedBerylliumModel bakedModel = berylliumState.getBakedModel();
                    int visibleFaces = BakedFace.ALL_FACES_SHOWING;

                    getSkyLight(TMP_SKY_LIGHT, x, y, z);
                    getBlockLight(TMP_BLOCK_LIGHT, x, y, z);

                    byte[] aoValues = Tessallator.EMPTY_AO;
                    if (bakedModel.doesCulling()) visibleFaces = getVisibleFaces(self, model, x, y, z);
                    if (bakedModel.usesAO()) getAmbientOcclusion(aoValues=TMP_AO_VALUES, x, y, z);

                    TintProvider.TintFunction tintFunction = berylliumState.getTintFunction();

                    transform.idt();
                    tmpMatrix.idt();

                    BlockTessallator blockTessallator = berylliumState.getTessallator();
                    blockTessallator.consume(
                            globalTessallator, tmpMatrix,
                            crossChunkAccessor,
                            chunk, self, x, y, z,
                            bakedModel, TMP_SKY_LIGHT, TMP_BLOCK_LIGHT,
                            aoValues, visibleFaces, tintFunction
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

    private static final int[] cullingMasks = {
            BakedFace.NEG_X_SHOWING,
            BakedFace.POS_X_SHOWING,
            BakedFace.NEG_Y_SHOWING,
            BakedFace.POS_Y_SHOWING,
            BakedFace.NEG_Z_SHOWING,
            BakedFace.POS_Z_SHOWING
    };

    private static final int[] rotatedMasks = new int[6];

    private static final int[] sides = {
            -1,  0,  0,
             1,  0,  0,
             0, -1,  0,
             0,  1,  0,
             0,  0, -1,
             0,  0,  1,
    };
    private static final int[] sideIndices = {
            0, 1, 2, 3, 4, 5
    };

    private static final int[] rotatedIndices = new int[6];

    private static int getVisibleFaces(BlockState self, BerylliumModel model, int x, int y, int z) {
        float xAxisRot = self.rotation[0]; // Affects Y and Z faces
        float yAxisRot = self.rotation[1]; // Affects X and Z faces
        float zAxisRot = self.rotation[2]; // Affects X and Y faces

        // if rotations are not perfectly 90deg, disable culling on affected face.
        boolean doCullXFaces = (zAxisRot % 90 == 0) && (yAxisRot % 90 == 0);
        boolean doCullYFaces = (zAxisRot % 90 == 0) && (xAxisRot % 90 == 0);
        boolean doCullZFaces = (yAxisRot % 90 == 0) && (xAxisRot % 90 == 0);

        int visibilityMask = BakedFace.NO_CULL_FACES;

        BlockState nx = crossChunkAccessor.getBlockState(tmp.set(-1,  0,  0), x, y, z);
        BlockState px = crossChunkAccessor.getBlockState(tmp.set( 1,  0,  0), x, y, z);
        BlockState ny = crossChunkAccessor.getBlockState(tmp.set( 0, -1,  0), x, y, z);
        BlockState py = crossChunkAccessor.getBlockState(tmp.set( 0,  1,  0), x, y, z);
        BlockState nz = crossChunkAccessor.getBlockState(tmp.set( 0,  0, -1), x, y, z);
        BlockState pz = crossChunkAccessor.getBlockState(tmp.set( 0,  0,  1), x, y, z);

        visibilityMask |= doCullXFaces ? (isOccluded(0,1, self, model, nx) ? 0 : rotatedMasks[0]) : BakedFace.NEG_X_SHOWING;
        visibilityMask |= doCullXFaces ? (isOccluded(1,0, self, model, px) ? 0 : rotatedMasks[1]) : BakedFace.POS_X_SHOWING;
        visibilityMask |= doCullYFaces ? (isOccluded(2,3, self, model, ny) ? 0 : rotatedMasks[2]) : BakedFace.NEG_Y_SHOWING;
        visibilityMask |= doCullYFaces ? (isOccluded(3,2, self, model, py) ? 0 : rotatedMasks[3]) : BakedFace.POS_Y_SHOWING;
        visibilityMask |= doCullZFaces ? (isOccluded(4,5, self, model, nz) ? 0 : rotatedMasks[4]) : BakedFace.NEG_Z_SHOWING;
        visibilityMask |= doCullZFaces ? (isOccluded(5,4, self, model, pz) ? 0 : rotatedMasks[5]) : BakedFace.POS_Z_SHOWING;

        return visibilityMask;
    }

    private static void rotateMasks(float xAxisRot, float yAxisRot, float zAxisRot) {
        int zRotCount = (int) Math.floor(zAxisRot / 90); // change X and Y
        int yRotCount = (int) Math.floor(yAxisRot / 90); // change X and Z
        int xRotCount = (int) Math.floor(xAxisRot / 90); // change Y and Z

        int zInvRotCount = (int) Math.floor((360 - zAxisRot) / 90); // change X and Y
        int yInvRotCount = (int) Math.floor((360 - yAxisRot) / 90); // change X and Z
        int xInvRotCount = (int) Math.floor((360 - xAxisRot) / 90); // change Y and Z

        System.arraycopy(sideIndices, 0, rotatedIndices, 0, 6);
        rotate(rotatedIndices, 2, zInvRotCount);
        rotate(rotatedIndices, 1, yInvRotCount);
        rotate(rotatedIndices, 0, xInvRotCount);

        System.arraycopy(cullingMasks, 0, rotatedMasks, 0, 6);
        rotate(rotatedMasks, 2, zRotCount);
        rotate(rotatedMasks, 1, yRotCount);
        rotate(rotatedMasks, 0, xRotCount);
    }

    private static final int DIR_NEG_X = 0;
    private static final int DIR_POS_X = 1;
    private static final int DIR_NEG_Y = 2;
    private static final int DIR_POS_Y = 3;
    private static final int DIR_NEG_Z = 4;
    private static final int DIR_POS_Z = 5;

    private static void rotate(int[] array, int axis, int count) {
        switch (axis) {
            // Z-AXIS
            case 2 -> {
                int ny = array[DIR_NEG_Y];
                int py = array[DIR_POS_Y];
                int nx = array[DIR_NEG_X];
                int px = array[DIR_POS_X];

                switch (count) {
                    case 1 -> {
                        array[DIR_POS_Y] = nx;
                        array[DIR_NEG_Y] = px;
                        array[DIR_POS_X] = py;
                        array[DIR_NEG_X] = ny;
                    }
                    case 2 -> {
                        array[DIR_POS_Y] = ny;
                        array[DIR_NEG_Y] = py;
                        array[DIR_POS_X] = nx;
                        array[DIR_NEG_X] = px;
                    }
                    case 3 -> {
                        array[DIR_POS_Y] = px;
                        array[DIR_NEG_Y] = nx;
                        array[DIR_POS_X] = ny;
                        array[DIR_NEG_X] = py;
                    }
                    default -> {}
                }
            }
            // Y-AXIS
            case 1 -> {
                int nx = array[DIR_NEG_X];
                int px = array[DIR_POS_X];
                int nz = array[DIR_NEG_Z];
                int pz = array[DIR_POS_Z];

                switch (count) {
                    case 1 -> {
                        array[DIR_POS_X] = nz;
                        array[DIR_NEG_X] = pz;
                        array[DIR_POS_Z] = px;
                        array[DIR_NEG_Z] = nx;
                    }
                    case 2 -> {
                        array[DIR_POS_X] = nx;
                        array[DIR_NEG_X] = px;
                        array[DIR_POS_Z] = nz;
                        array[DIR_NEG_Z] = pz;
                    }
                    case 3 -> {
                        array[DIR_POS_X] = pz;
                        array[DIR_NEG_X] = nz;
                        array[DIR_POS_Z] = nx;
                        array[DIR_NEG_Z] = px;
                    }
                    default -> {}
                }
            }
            // X-AXIS
            case 0 -> {
                int ny = array[DIR_NEG_Y];
                int py = array[DIR_POS_Y];
                int nz = array[DIR_NEG_Z];
                int pz = array[DIR_POS_Z];

                switch (count) {
                    case 1 -> {
                        array[DIR_POS_Y] = nz;
                        array[DIR_NEG_Y] = pz;
                        array[DIR_POS_Z] = py;
                        array[DIR_NEG_Z] = ny;
                    }
                    case 2 -> {
                        array[DIR_POS_Y] = ny;
                        array[DIR_NEG_Y] = py;
                        array[DIR_POS_Z] = nz;
                        array[DIR_NEG_Z] = pz;
                    }
                    case 3 -> {
                        array[DIR_POS_Y] = pz;
                        array[DIR_NEG_Y] = nz;
                        array[DIR_POS_Z] = ny;
                        array[DIR_NEG_Z] = py;
                    }
                    default -> {}
                }
            }
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        }
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

        skyLight[0] = crossChunkAccessor.getSkyLight(setSideVector(tmp, 0), x, y, z);
        skyLight[1] = crossChunkAccessor.getSkyLight(setSideVector(tmp, 1), x, y, z);
        skyLight[2] = crossChunkAccessor.getSkyLight(setSideVector(tmp, 2), x, y, z);
        skyLight[3] = crossChunkAccessor.getSkyLight(setSideVector(tmp, 3), x, y, z);
        skyLight[4] = crossChunkAccessor.getSkyLight(setSideVector(tmp, 4), x, y, z);
        skyLight[5] = crossChunkAccessor.getSkyLight(setSideVector(tmp, 5), x, y, z);
    }

    private static Vector3 setSideVector(Vector3 tmp, int i) {
        i = rotatedIndices[i];

        tmp.x = sides[(i * 3)];
        tmp.y = sides[(i * 3) + 1];
        tmp.z = sides[(i * 3) + 2];

        return tmp;
    }

    private static void getBlockLight(
            short[] blockLight,
            int x, int y, int z
    ) {
        Arrays.fill(blockLight, (short) 0);

        blockLight[0] = crossChunkAccessor.getBlockLight(setSideVector(tmp, 0), x, y, z);
        blockLight[1] = crossChunkAccessor.getBlockLight(setSideVector(tmp, 1), x, y, z);
        blockLight[2] = crossChunkAccessor.getBlockLight(setSideVector(tmp, 2), x, y, z);
        blockLight[3] = crossChunkAccessor.getBlockLight(setSideVector(tmp, 3), x, y, z);
        blockLight[4] = crossChunkAccessor.getBlockLight(setSideVector(tmp, 4), x, y, z);
        blockLight[5] = crossChunkAccessor.getBlockLight(setSideVector(tmp, 5), x, y, z);
    }

    private static boolean isOccluded(int d, int od, BlockState self, BerylliumModel selfModel, BlockState state) {
        if (state == null || state.hasEmptyModel()) return false;
        BerylliumModel stateModel = BerylliumModelLoader.getModel(state.modelName);
        if (stateModel.getName().equals(selfModel.getName()) && self.cullsSelf()) return true;

        if (stateModel.isTransparent() || selfModel.isTransparent()) return false;
        if (!state.isOpaque || !self.isOpaque) return false;
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

}
