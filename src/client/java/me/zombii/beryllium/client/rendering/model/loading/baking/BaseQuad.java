package me.zombii.beryllium.client.rendering.model.loading.baking;

import finalforeach.cosmicreach.util.constants.Direction;

public record BaseQuad(
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

    public static final BaseQuad POS_X = new BaseQuad(
            Direction.POS_X.ordinal(),
            true,
            false,
            false,
            true,
            90,
            new float[]{
                    .5f, -.5f, -.5f, // c00
                    .5f, -.5f, .5f, // c01
                    .5f, .5f, -.5f, // c10
                    .5f, .5f, .5f, // c11
            }
    );

    public static final BaseQuad NEG_X = new BaseQuad(
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

    public static final BaseQuad POS_Y = new BaseQuad(
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

    public static final BaseQuad NEG_Y = new BaseQuad(
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

    public static final BaseQuad POS_Z = new BaseQuad(
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

    public static final BaseQuad NEG_Z = new BaseQuad(
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

    public static final BaseQuad[] FACES = {
            NEG_X, POS_X, NEG_Y, POS_Y, NEG_Z, POS_Z
    };

}
