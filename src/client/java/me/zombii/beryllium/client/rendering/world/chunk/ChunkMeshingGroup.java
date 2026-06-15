package me.zombii.beryllium.client.rendering.world.chunk;

import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Zone;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.zombii.beryllium.client.rendering.BerylliumMesh;
import me.zombii.beryllium.client.rendering.TintProvider;
import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import me.zombii.beryllium.client.rendering.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.model.loading.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.model.loading.baking.BakedFace;
import me.zombii.beryllium.client.rendering.model.loading.baking.ModelBaker;
import me.zombii.beryllium.client.rendering.model.loading.baking.Tessallator;

import java.lang.reflect.Field;

public class ChunkMeshingGroup {

    public static final int quadsPerChunk = 16 * 16 * 16 * BakedBerylliumModel.MAX_FACES_PER_MODEL;

    private static final Tessallator globalTessallator = new Tessallator(quadsPerChunk);

    public static void meshChunk(Chunk chunk, BerylliumMesh mesh) {
        if (mesh.isDisposed()) return;
        globalTessallator.reset();
        mesh.clear();

//        System.out.println("Meshing chunk " + chunk);
        int levelOfDetail = 0;
        int blockSize = 1 << levelOfDetail;

        int cx = chunk.getChunkX();
        int cy = chunk.getChunkY();
        int cz = chunk.getChunkZ();

        if (chunk.region == null) return;
        Zone zone = chunk.getZone();
        Chunk PX = zone.getChunkAtChunkCoords(cx + 1, cy, cz);
        Chunk PY = zone.getChunkAtChunkCoords(cx, cy + 1, cz);
        Chunk PZ = zone.getChunkAtChunkCoords(cx, cy, cz + 1);
        Chunk NX = zone.getChunkAtChunkCoords(cx - 1, cy, cz);
        Chunk NY = zone.getChunkAtChunkCoords(cx, cy - 1, cz);
        Chunk NZ = zone.getChunkAtChunkCoords(cx, cy, cz - 1);

        int maxBlockX = 15;
        int maxBlockY = 15;
        int maxBlockZ = 15;

        for (int x = 0; x < 16; x += 1) {
            for (int y = 0; y < 16; y += 1) {
                for (int z = 0; z < 16; z += 1) {
                    BlockState state = chunk.getBlockState(x, y, z);
//                    BlockState state = getMostAbundant(x, y, z, blockSize, chunk);

                    // skip model-less blocks
                    if (state.hasEmptyModel()) continue;

                    int faceMask = 0; // will be inverted for model stuff
                    if (x == 0 && NX != null && checkState(state, NX.getBlockState(maxBlockX, y, z))) faceMask |= BakedFace.NEG_X_SHOWING;
                    else if (checkState(state, chunk.getBlockState(x - blockSize, y, z))) faceMask |= BakedFace.NEG_X_SHOWING;

                    if (y == 0 && NY != null && checkState(state, NY.getBlockState(x, maxBlockY, z))) faceMask |= BakedFace.NEG_Y_SHOWING;
                    else if (checkState(state, chunk.getBlockState(x, y - blockSize, z))) faceMask |= BakedFace.NEG_Y_SHOWING;

                    if (z == 0 && NZ != null && checkState(state, NZ.getBlockState(x, y, maxBlockZ))) faceMask |= BakedFace.NEG_Z_SHOWING;
                    else if (checkState(state, chunk.getBlockState(x, y, z - blockSize))) faceMask |= BakedFace.NEG_Z_SHOWING;

                    if (x == maxBlockX && PX != null && checkState(state, PX.getBlockState(0, y, z))) faceMask |= BakedFace.POS_X_SHOWING;
                    else if (checkState(state, chunk.getBlockState(x + blockSize, y, z))) faceMask |= BakedFace.POS_X_SHOWING;

                    if (y == maxBlockY && PY != null && checkState(state, PY.getBlockState(x, 0, z))) faceMask |= BakedFace.POS_Y_SHOWING;
                    else if (checkState(state, chunk.getBlockState(x, y + blockSize, z))) faceMask |= BakedFace.POS_Y_SHOWING;

                    if (z == maxBlockZ && PZ != null && checkState(state, PZ.getBlockState(x, y, 0))) faceMask |= BakedFace.POS_Z_SHOWING;
                    else if (checkState(state, chunk.getBlockState(x, y, z + blockSize))) faceMask |= BakedFace.POS_Z_SHOWING;

                    if ((faceMask & BakedFace.ALL_CULLABLE_SHOWING) == BakedFace.ALL_CULLABLE_SHOWING)
                        continue;
                    faceMask = ~faceMask; // invertedForModel
                    faceMask |= BakedFace.NO_CULL_FACES;
//                    faceMask = BakedFace.ALL_FACES_SHOWING;

                    TintProvider.TintFunction tintFunction = TintProvider.getForState(state.getBlock());
                    short tintColor = tintFunction.getTint(state, new BlockPosition(chunk, x, y, z), -1);
                    BerylliumModel model = BerylliumModelLoader.getModel(getModelId(state));
//                    System.out.println(model.getID());
                    BakedBerylliumModel bakedModel = ModelBaker.get(model);
//                    System.out.println(bakedModel.getModel().getID());
                    bakedModel.addVertices(globalTessallator, (short) 0, faceMask, tintColor, x, y, z);
                }
            }
        }
        if (globalTessallator.getQuadsWritten() == 0) {
            mesh.clear();
            return;
        }
        mesh.resize(globalTessallator.getQuadsWritten());
        mesh.dump(globalTessallator, true);
    }

    private static Identifier getModelId(BlockState state) {
        return Identifier.of(state.modelName);
    }

    private static boolean checkState(BlockState self, BlockState state) {
        if (self.equals(state) && self.cullsSelf()) return true;
        if (state == null) return true;
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
