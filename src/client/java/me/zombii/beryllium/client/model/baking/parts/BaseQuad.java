package me.zombii.beryllium.client.model.baking.parts;

import finalforeach.cosmicreach.util.constants.Direction;

public record BaseQuad(
        int direction,
        boolean flipU,
        boolean flipV,
        int defaultRotation,
        float[] verts
) {

    public static final int[] INDICES = new int[]{
            0, 1, 2, 2, 1, 3
    };


    //Base quad based on direction will have the bottom right corner be the coordinate be c00 (first 3 floats of verts)
    //Uses a right handed y up z forward direction so looking forward you see the neg z face
    //the negz face bottom right will be neg x neg y and neg z resulting in c00 be -0.5,-0.5,-0.5
    //c01 is top Right, c10 is bottom left, c11 is top left
    //pos y top faces neg z and neg top faces pos z
    public static final BaseQuad POS_X = new BaseQuad(
            Direction.POS_X.ordinal(),
            true,
            false,
            90,
            new float[]{
                    .5f, -.5f, -.5f, // c00
                    .5f, .5f, -.5f, // c01
                    .5f, -.5f, .5f, // c10
                    .5f, .5f, .5f, // c11
            }
    );

    public static final BaseQuad NEG_X = new BaseQuad(
            Direction.NEG_X.ordinal(),
            false,
            false,
            90,
            new float[]{
                    -.5f, -.5f, .5f, // c00
                    -.5f, .5f, .5f, // c01
                    -.5f, -.5f, -.5f, // c10
                    -.5f, .5f, -.5f, // c11
            }
    );

    public static final BaseQuad POS_Y = new BaseQuad(
            Direction.POS_Y.ordinal(),
            false,
            false,
            0,
            new float[]{ //cr might have facing y top be towards neg z idk
                    .5f, .5f, .5f, // c00 //bot right
                    .5f, .5f, -.5f, // c01 // top right
                    -.5f, .5f, .5f, // c10 bot right
                    -.5f, .5f, -.5f, // c11 //top right
            }
    );

    public static final BaseQuad NEG_Y = new BaseQuad(
            Direction.NEG_Y.ordinal(),
            true,
            false,
            0,
            new float[]{
                    .5f, -.5f, -.5f, // c00
                    .5f, -.5f, .5f, // c01
                    -.5f, -.5f, -.5f, // c10
                    -.5f, -.5f, .5f, // c11
            }
    );

    public static final BaseQuad POS_Z = new BaseQuad(
            Direction.POS_Z.ordinal(),
            true,
            false,
            0,
            new float[]{
                    .5f, -.5f, .5f, // c00
                    .5f, .5f, .5f, // c01
                    -.5f, -.5f, .5f, // c10
                    -.5f, .5f, .5f, // c11
            }
    );

    public static final BaseQuad NEG_Z = new BaseQuad(
            Direction.NEG_Z.ordinal(),
            false,
            false,
            0,
            new float[]{
                    -.5f, -.5f, -.5f, // c00 //bot right
                    -.5f, .5f, -.5f, // c01 //top right
                    .5f, -.5f, -.5f, // c10 // bot left
                    .5f, .5f, -.5f, // c11 // top left
            }
    );

    public static final BaseQuad[] FACES = {
            NEG_X, POS_X, NEG_Y, POS_Y, NEG_Z, POS_Z
    };

}
