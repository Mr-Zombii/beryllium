package me.zombii.beryllium.client.rendering.model.loading.baking;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.util.constants.Direction;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;

public record BakedQuad(
        int direction,
        boolean flipIndices,
        boolean flipVerts,
        boolean flipU,
        boolean flipV,
        int defaultRotation,
        float[] verts
) {

    public static final int[] indices = new int[]{
            0, 1, 2, 2, 1, 3
    };

    public static final int[] indices_flipped = new int[]{
            0, 2, 3, 3, 1, 0
    };

    private static final float sixteenth = 1/16f;

    public static final BakedQuad POS_X = new BakedQuad(
            Direction.POS_X.ordinal(),
            true,
            false,
            false,
            false,
            90,
            new float[]{
                    .5f, -.5f, -.5f, // c00
                    .5f, -.5f, .5f, // c01
                    .5f, .5f, -.5f, // c10
                    .5f, .5f, .5f, // c11
            }
    );

    public static final BakedQuad NEG_X = new BakedQuad(
            Direction.NEG_X.ordinal(),
            false,
            false,
            false,
            false,
            90,
            new float[]{
                    -.5f, -.5f, -.5f, // c00
                    -.5f, -.5f, .5f, // c01
                    -.5f, .5f, -.5f, // c10
                    -.5f, .5f, .5f, // c11
            }
    );

    public static final BakedQuad POS_Y = new BakedQuad(
            Direction.POS_Y.ordinal(),
            false,
            false,
            false,
            false,
            0,
            new float[]{
                    -.5f, .5f, -.5f, // c00
                    -.5f, .5f, .5f, // c01
                    .5f, .5f, -.5f, // c10
                    .5f, .5f, .5f, // c11
            }
    );

    public static final BakedQuad NEG_Y = new BakedQuad(
            Direction.NEG_Y.ordinal(),
            true,
            false,
            false,
            true,
            0,
            new float[]{
                    -.5f, -.5f, -.5f, // c00
                    -.5f, -.5f, .5f, // c01
                    .5f, -.5f, -.5f, // c10
                    .5f, -.5f, .5f, // c11
            }
    );

    public static final BakedQuad POS_Z = new BakedQuad(
            Direction.POS_Z.ordinal(),
            true,
            false,
            false,
            true,
            0,
            new float[]{
                    -.5f, -.5f, .5f, // c00
                    -.5f, .5f, .5f, // c01
                    .5f, -.5f, .5f, // c10
                    .5f, .5f, .5f, // c11
            }
    );

    public static final BakedQuad NEG_Z = new BakedQuad(
            Direction.NEG_Z.ordinal(),
            false,
            false,
            true,
            true,
            0,
            new float[]{
                    -.5f, -.5f, -.5f, // c00
                    -.5f, .5f, -.5f, // c01
                    .5f, -.5f, -.5f, // c10
                    .5f, .5f, -.5f, // c11
            }
    );

    public static final BakedQuad[] FACES = {
            NEG_X, POS_X, NEG_Y, POS_Y, NEG_Z, POS_Z
    };

}
