package me.zombii.beryllium.client.rendering.world.chunk;

import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Zone;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.zombii.beryllium.client.rendering.TintProvider;
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

    private static Tessallator globalTessallator;
    private static boolean initialized = false;
    private static final CrossChunkAccessor crossChunkAccessor = new CrossChunkAccessor();

    public static void init() {
        if (initialized) return;
        initialized = true;

        globalTessallator = new Tessallator(quadsPerChunk);
        generateAoTable();
    }

    private static final short[] TMP_SKY_LIGHT = new short[6];
    private static final short[] TMP_BLOCK_LIGHT = new short[6];
    private static final byte[] TMP_AO_VALUES = new byte[4 * 6];

    public static void meshChunk(Chunk chunk, ChunkMesh mesh) {
        if (mesh.isDisposed()) return;
        globalTessallator.reset();
        mesh.clear();

        int levelOfDetail = 0;
        int blockSize = 1 << levelOfDetail;
        mesh.scale = blockSize;

        if (chunk.region == null) return;
        Zone zone = chunk.getZone();
        crossChunkAccessor.init(zone, chunk);
        Chunk PX = crossChunkAccessor.getChunk(1, 0, 0);
        Chunk PY = crossChunkAccessor.getChunk(0, 1, 0);
        Chunk PZ = crossChunkAccessor.getChunk(0, 0, 1);
        Chunk NX = crossChunkAccessor.getChunk(-1, 0, 0);
        Chunk NY = crossChunkAccessor.getChunk(0, -1, 0);
        Chunk NZ = crossChunkAccessor.getChunk(0, 0, -1);

        int maxBlockX = (16 / blockSize) - 1;
        int maxBlockY = (16 / blockSize) - 1;
        int maxBlockZ = (16 / blockSize) - 1;

        for (int x = 0; x < 16; x += 1) {
            for (int y = 0; y < 16; y += 1) {
                for (int z = 0; z < 16; z += 1) {
                    BlockState state = chunk.getBlockState(x, y, z);
//                    BlockState state = getMostAbundant(x, y, z, blockSize, chunk);

                    // skip model-less blocks
                    if (state == null) continue;
                    if (state.hasEmptyModel()) continue;

                    int faceMask = 0; // will be inverted for model stuff

                    if (x == 0 && NX != null && checkState(state, getMostAbundant(maxBlockX, y, z, blockSize, NX))) {
                        faceMask |= BakedFace.NEG_X_SHOWING;
                    }
                    else if (checkState(state, getMostAbundant(x - blockSize, y, z, blockSize, chunk))) {
                        faceMask |= BakedFace.NEG_X_SHOWING;
                    }

                    if (y == 0 && NY != null && checkState(state, getMostAbundant(x, maxBlockY, z, blockSize, NY))) {
                        faceMask |= BakedFace.NEG_Y_SHOWING;
                    }
                    else if (checkState(state, getMostAbundant(x, y - blockSize, z, blockSize, chunk))) {
                        faceMask |= BakedFace.NEG_Y_SHOWING;
                    }

                    if (z == 0 && NZ != null && checkState(state, getMostAbundant(x, y, maxBlockZ, blockSize, NZ))) {
                        faceMask |= BakedFace.NEG_Z_SHOWING;
                    }
                    else if (checkState(state, getMostAbundant(x, y, z - blockSize, blockSize, chunk))) {
                        faceMask |= BakedFace.NEG_Z_SHOWING;
                    }

                    if (x == maxBlockX && PX != null && checkState(state, getMostAbundant(0, y, z, blockSize, PX))) {
                        faceMask |= BakedFace.POS_X_SHOWING;
                    }
                    else if (checkState(state, getMostAbundant(x + blockSize, y, z, blockSize, chunk))) {
                        faceMask |= BakedFace.POS_X_SHOWING;
                    }

                    if (y == maxBlockY && PY != null && checkState(state, getMostAbundant(x, 0, z, blockSize, PY))) {
                        faceMask |= BakedFace.POS_Y_SHOWING;
                    }
                    else if (checkState(state, getMostAbundant(x, y + blockSize, z, blockSize, chunk))) {
                        faceMask |= BakedFace.POS_Y_SHOWING;
                    }

                    if (z == maxBlockZ && PZ != null && checkState(state, getMostAbundant(x, y, 0, blockSize, PZ))) {
                        faceMask |= BakedFace.POS_Z_SHOWING;
                    }
                    else if (checkState(state, getMostAbundant(x, y, z + blockSize, blockSize, chunk))) {
                        faceMask |= BakedFace.POS_Z_SHOWING;
                    }

                    getSkyLight(
                            chunk, TMP_SKY_LIGHT,
                            PX, PY, PZ,
                            NX, NY, NZ,
                            x, y, z,
                            maxBlockX, maxBlockY, maxBlockZ,
                            blockSize
                    );
                    getBlockLight(
                            chunk, TMP_BLOCK_LIGHT,
                            PX, PY, PZ,
                            NX, NY, NZ,
                            x, y, z,
                            maxBlockX, maxBlockY, maxBlockZ,
                            blockSize
                    );
                    getAmbientOcclusion(TMP_AO_VALUES, x, y, z);

                    if ((faceMask & BakedFace.ALL_CULLABLE_SHOWING) == BakedFace.ALL_CULLABLE_SHOWING)
                        continue;
                    faceMask = ~faceMask; // invertedForModel
                    faceMask |= BakedFace.NO_CULL_FACES;
//                    faceMask = BakedFace.ALL_FACES_SHOWING;

//                    lightRGB444 |= lightLevel & 15;
//                    lightRGB444 |= (lightLevel & 15) << 4;
//                    lightRGB444 |= (lightLevel & 15) << 8;

                    final int xFinal = x;
                    final int yFinal = y;
                    final int zFinal = z;
                    TintProvider.TintFunction tintFunction = TintProvider.getForState(state.getBlock());
                    Function<Integer, Short> tintGetter = (idx) ->
                            tintFunction.getTint(
                                    state,
                                    new BlockPosition(
                                            chunk,
                                            xFinal, yFinal, zFinal
                                    ),
                            idx);

                    BerylliumModel model = BerylliumModelLoader.getModel(getModelId(state));
                    BakedBerylliumModel bakedModel = ModelBaker.get(model);
                    bakedModel.addVertices(
                            globalTessallator,
                            TMP_SKY_LIGHT, TMP_BLOCK_LIGHT, TMP_AO_VALUES,
                            faceMask, tintGetter,
                            x, y, z
                    );
                }
            }
        }
        if (mesh.isScheduledForDisposal()) return;
        if (globalTessallator.getQuadsWritten() == 0) {
            mesh.clear();
            return;
        }
        mesh.resize(globalTessallator.getQuadsWritten());
        mesh.dump(globalTessallator, true);
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
            Chunk chunk, short[] skyLight,
            Chunk px, Chunk py, Chunk pz,
            Chunk nx, Chunk ny, Chunk nz,
            int x, int y, int z,
            int xM, int yM, int zM,
            int blockSize
    ) {
        Arrays.fill(skyLight, (short) 0);

        if (x == 0) skyLight[0] = nx == null ? 15 : (short) nx.getSkyLight(xM, y, z);
        else skyLight[0] = (short) chunk.getSkyLight(x - blockSize, y, z);
        if (y == 0) skyLight[2] = ny == null ? 15 : (short) ny.getSkyLight(x, yM, z);
        else skyLight[2] = (short) chunk.getSkyLight(x, y - blockSize, z);
        if (z == 0) skyLight[4] = nz == null ? 15 : (short) nz.getSkyLight(x, y, zM);
        else skyLight[4] = (short) chunk.getSkyLight(x, y, z - blockSize);

        if (x == xM) skyLight[1] = px == null ? 15 : (short) px.getSkyLight(0, y, z);
        else skyLight[1] = (short) chunk.getSkyLight(x + blockSize, y, z);
        if (y == yM) skyLight[3] = py == null ? 15 : (short) py.getSkyLight(x, 0, z);
        else skyLight[3] = (short) chunk.getSkyLight(x, y + blockSize, z);
        if (z == zM) skyLight[5] = pz == null ? 15 : (short) pz.getSkyLight(x, y, 0);
        else skyLight[5] = (short) chunk.getSkyLight(x, y, z + blockSize);
    }

    private static void getBlockLight(
            Chunk chunk, short[] blockLight,
            Chunk px, Chunk py, Chunk pz,
            Chunk nx, Chunk ny, Chunk nz,
            int x, int y, int z,
            int xM, int yM, int zM,
            int blockSize
    ) {
        Arrays.fill(blockLight, (short) 0);

        if (x == 0) blockLight[0] = nx == null ? 0 : nx.getBlockLight(xM, y, z);
        else blockLight[0] = chunk.getBlockLight(x - blockSize, y, z);
        if (y == 0) blockLight[2] = ny == null ? 0 : ny.getBlockLight(x, yM, z);
        else blockLight[2] = chunk.getBlockLight(x, y - blockSize, z);
        if (z == 0) blockLight[4] = nz == null ? 0 : nz.getBlockLight(x, y, zM);
        else blockLight[4] = chunk.getBlockLight(x, y, z - blockSize);

        if (x == xM) blockLight[1] = px == null ? 0 : px.getBlockLight(0, y, z);
        else blockLight[1] = chunk.getBlockLight(x + blockSize, y, z);
        if (y == yM) blockLight[3] = py == null ? 0 : py.getBlockLight(x, 0, z);
        else blockLight[3] = chunk.getBlockLight(x, y + blockSize, z);
        if (z == zM) blockLight[5] = pz == null ? 0 : pz.getBlockLight(x, y, 0);
        else blockLight[5] = chunk.getBlockLight(x, y, z + blockSize);
    }

    private static Identifier getModelId(BlockState state) {
        return Identifier.of(state.modelName);
    }

    private static boolean checkState(BlockState self, BlockState state) {
        if (self.equals(state) && self.cullsSelf()) return true;
        if (state == null) return false;
        return state.isOpaque && !state.isFluid && !state.hasEmptyModel();
    }

    private static final Object2IntMap<BlockState> stateAbundanceMap = new Object2IntArrayMap<>();

    private static BlockState getMostAbundant(
            int x, int y, int z,
            int blockSize,
            Chunk chunk
    ) {
        if (blockSize == 1) return chunk.getBlockState(x, y, z);
        stateAbundanceMap.clear();

        for (int bx = x; bx < x + blockSize - 1; bx++) {
            for (int by = y; by < y + blockSize - 1; by++) {
                for (int bz = z; bz < z + blockSize - 1; bz++) {
                    BlockState state = chunk.getBlockState(bx, by, bz);
                    stateAbundanceMap.put(state, stateAbundanceMap.getOrDefault(state, 0) + 1);
                }
            }
        }

        BlockState blockState = chunk.getBlockState(x, y, z);
        int largest = 0;
        for (BlockState state : stateAbundanceMap.keySet()) {
            int i = stateAbundanceMap.getInt(state);
            if (i > largest) {
                largest = i;
                blockState = state;
            }
        }

        return blockState;
    }

}
