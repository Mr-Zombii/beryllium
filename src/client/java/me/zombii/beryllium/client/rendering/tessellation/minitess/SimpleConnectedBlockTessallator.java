package me.zombii.beryllium.client.rendering.tessellation.minitess;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.puzzleshq.annotation.stability.Experimental;
import dev.puzzleshq.annotation.stability.Unstable;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.model.baking.parts.BakedFace;
import me.zombii.beryllium.client.model.baking.parts.BaseQuad;
import me.zombii.beryllium.client.model.baking.parts.VertexGroup;
import me.zombii.beryllium.client.model.parts.TextureEntry;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;
import me.zombii.beryllium.client.rendering.world.chunk.ChunkMesher;
import me.zombii.beryllium.client.rendering.world.chunk.CrossChunkAccessor;

@Unstable
@Experimental
public class SimpleConnectedBlockTessallator extends BlockTessallator {

    private static final SimpleConnectedBlockTessallator INSTANCE = new SimpleConnectedBlockTessallator();
    private static final Object2ObjectMap<BlockState, BakedBerylliumModel> MODEL_MAP = new Object2ObjectOpenHashMap<>();

    public static void registerState(BlockState state, BakedBerylliumModel model) {
        if (!model.isGreedy()) {
            throw new RuntimeException("Cannot register state \"" + state + "\" with model \"" + model.getModel().getName() + "\" because it is not a single 16x16x16 cube!");
        }
        if (!model.doesAllCulling()) {
            throw new RuntimeException("Cannot register state \"" + state + "\" with model \"" + model.getModel().getName() + "\" because it has cullFace turned off on some of its faces!");
        }
        BlockTessallator.register(state, INSTANCE);
        MODEL_MAP.put(state, model);
    }

    private final Vector3 tmpOffs = new Vector3();

    private static final int[] FACES_POS = {
            // X_FACES
            0,  0, -1,
            0,  1,  0,
            0,  0,  1,
            0, -1,  0,

            // Y_FACES
             1,  0,  0,
             0,  0, -1,
            -1,  0,  0,
             0,  0,  1,

            // Z_FACES
             1,  0, 0,
             0,  1, 0,
            -1,  0, 0,
             0, -1, 0,
    };

    private static Vector3 getOffs(Vector3 tmp, int axis, int idx) {
        int baseIdx = (axis * 12) + (idx * 3);

        return tmp.set(
                FACES_POS[baseIdx],
                FACES_POS[baseIdx + 1],
                FACES_POS[baseIdx + 2]
        );
    }

    private static Vector3 getOffs(Vector3 tmp, int axis, int idx, int x, int y, int z) {
        int baseIdx = (axis * 12) + (idx * 3);

        return tmp.set(
                FACES_POS[baseIdx],
                FACES_POS[baseIdx + 1],
                FACES_POS[baseIdx + 2]
        ).add(x, y, z);
    }

    public boolean checkState(
            CrossChunkAccessor accessor, BlockState state,
            int x, int y, int z,
            int axis, int idx,
            boolean flipX, boolean flipY, boolean flipZ
    ) {
        getOffs(tmpOffs, axis, idx);
        if (flipX) tmpOffs.x = -tmpOffs.x;
        if (flipY) tmpOffs.y = -tmpOffs.y;
        if (flipZ) tmpOffs.z = -tmpOffs.z;
        BlockState nState = accessor.getBlockState(tmpOffs, x, y, z);
        return nState != null && nState.equals(state);
    }

    private String tmpTex = "0-open-faces";
    private int tmpRot = 0;

    private void checkAxis(
            CrossChunkAccessor accessor, BlockState state,
           int axis, int x, int y, int z,
            boolean flipX, boolean flipY, boolean flipZ
    ) {
        boolean s01 = checkState(accessor, state, x, y, z, axis, 0, flipX, flipY, flipZ);
        boolean s10 = checkState(accessor, state, x, y, z, axis, 1, flipX, flipY, flipZ);
        boolean s02 = checkState(accessor, state, x, y, z, axis, 2, flipX, flipY, flipZ);
        boolean s20 = checkState(accessor, state, x, y, z, axis, 3, flipX, flipY, flipZ);

        int count = ((s01 ? 1 : 0) + (s10 ? 1 : 0) + (s02 ? 1 : 0) + (s20 ? 1 : 0));

        if (count == 0) {
            tmpTex = "0-open-faces";
            tmpRot = 0;
            return;
        }

        if (count == 1) {
            tmpTex = "1-open-faces";
            tmpRot = s02 ? 0 : (s10 ? 90 : (s01 ? 180 : 270));
            return;
        }

        if (count == 2) {
            if ((s01 && s20) || (s10 && s20)) {
                tmpTex = "2b-open-faces";
                tmpRot = s10 ? 0 : 90;
                return;
            }
            tmpTex = "1-open-faces";
            tmpRot = 0;
            return;
        }

        if (count == 3) {
            tmpTex = "3-open-faces";
            tmpRot = !s10 ? 0 : (!s01 ? 90 : (!s20 ? 180 : 270));
            return;
        }

        tmpTex = "4-open-faces";
        tmpRot = 0;
        return;
    }

    @Override
    public void consume(
            Tessallator tessallator, Matrix4 tmp,
            CrossChunkAccessor accessor,
            Chunk chunk, BlockState state, int x, int y, int z,
            BakedBerylliumModel model, short[] skylight,
            short[] blocklight, byte[] aoValues, int visibleFacesMask,
            TintProvider.TintFunction tintFunction
    ) {
        BakedBerylliumModel bakedModel = MODEL_MAP.get(state);

//        TextureEntry openFaces0 = blockModel.getTexture("0-open-faces");
//        TextureEntry openFaces1 = blockModel.getTexture("1-open-faces");
//        TextureEntry openFaces2 = blockModel.getTexture("2-open-faces");
//        TextureEntry openFaces2b = blockModel.getTexture("2b-open-faces");
//        TextureEntry openFaces3 = blockModel.getTexture("3-open-faces");
//        TextureEntry openFaces4 = blockModel.getTexture("4-open-faces");

        boolean NEG_X = (visibleFacesMask & BakedFace.NEG_X_SHOWING) != 0;
        boolean NEG_Y = (visibleFacesMask & BakedFace.NEG_Y_SHOWING) != 0;
        boolean NEG_Z = (visibleFacesMask & BakedFace.NEG_Z_SHOWING) != 0;
        boolean POS_X = (visibleFacesMask & BakedFace.POS_X_SHOWING) != 0;
        boolean POS_Y = (visibleFacesMask & BakedFace.POS_Y_SHOWING) != 0;
        boolean POS_Z = (visibleFacesMask & BakedFace.POS_Z_SHOWING) != 0;

        VertexGroup group = bakedModel.getGroupList().getFirst();
        BakedFace NEG_X_FACE = group.getFacesByDirection(0).getFirst();
        BakedFace POS_X_FACE = group.getFacesByDirection(1).getFirst();
        BakedFace NEG_Y_FACE = group.getFacesByDirection(2).getFirst();
        BakedFace POS_Y_FACE = group.getFacesByDirection(3).getFirst();
        BakedFace NEG_Z_FACE = group.getFacesByDirection(4).getFirst();
        BakedFace POS_Z_FACE = group.getFacesByDirection(5).getFirst();

        if (NEG_X || NEG_Y) {
            if (NEG_X) {
                checkAxis(accessor, state, 0, x, y, z, false, false, true);
                short tint = tintFunction.getTint(chunk, state, x, y, z, NEG_X_FACE.tintIndex());
                tessallator.addQuad(NEG_X_FACE, tmpTex, tmpRot, skylight[0], blocklight[0], aoValues, tint, 0, x, y, z);
            }

            if (POS_X) {
                checkAxis(accessor, state, 0, x, y, z, false, false, false);
                short tint = tintFunction.getTint(chunk, state, x, y, z, POS_X_FACE.tintIndex());
                tessallator.addQuad(POS_X_FACE, tmpTex, tmpRot, skylight[1], blocklight[1], aoValues, tint, 1, x, y, z);
            }
        }

        if (NEG_Y || POS_Y) {
            if (NEG_Y) {
                checkAxis(accessor, state, 1, x, y, z, false, false, true);
                short tint = tintFunction.getTint(chunk, state, x, y, z, NEG_Y_FACE.tintIndex());
                tessallator.addQuad(NEG_Y_FACE, tmpTex, tmpRot, skylight[2], blocklight[2], aoValues, tint, 2, x, y, z);
            }

            if (POS_Y) {
                checkAxis(accessor, state, 1, x, y, z, false, false, false);
                short tint = tintFunction.getTint(chunk, state, x, y, z, POS_Y_FACE.tintIndex());
                tessallator.addQuad(POS_Y_FACE, tmpTex, tmpRot, skylight[3], blocklight[3], aoValues, tint, 3, x, y, z);
            }
        }

        if (NEG_Z || POS_Z) {
            if (NEG_Z) {
                checkAxis(accessor, state, 2, x, y, z, true, false, false);
                short tint = tintFunction.getTint(chunk, state, x, y, z, NEG_Z_FACE.tintIndex());
                tessallator.addQuad(NEG_Z_FACE, tmpTex, tmpRot, skylight[4], blocklight[4], aoValues, tint, 4, x, y, z);
            }

            if (POS_Z) {
                checkAxis(accessor, state, 2, x, y, z, false, false, false);
                short tint = tintFunction.getTint(chunk, state, x, y, z, POS_Z_FACE.tintIndex());
                tessallator.addQuad(POS_Z_FACE, tmpTex, tmpRot, skylight[5], blocklight[5], aoValues, tint, 5, x, y, z);
            }
        }

//        model.addVertices(
//                tessallator,
//                skylight, blocklight, aoValues,
//                visibleFacesMask, tintFunction, chunk, state,
//                x, y, z
//        );
    }

}
