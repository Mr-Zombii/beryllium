package me.zombii.beryllium.client.rendering.model.loading.baking;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import me.zombii.beryllium.client.rendering.model.TextureEntry;

import java.util.Arrays;
import java.util.Map;

public record BakedFace(
        BakedBerylliumModel model,
        String textureID,
        BakedQuad bakedQuad,

        boolean useAO,

        float[] verts
) {

    public BakedFace(
            BakedBerylliumModel model,
            String textureID,
            BakedQuad quad,
            boolean usesAO
    ) {
        this(
                model,
                textureID,
                quad,
                usesAO,
                new float[12]
        );

        if (quad.flipVerts()) {
            System.arraycopy(quad.verts(), 0, verts, 5, 6);
            System.arraycopy(quad.verts(), 5, verts, 0, 6);
            return;
        }
        System.arraycopy(quad.verts(), 0, verts, 0, 12);
    }

    private static final Vector3 tmp = new Vector3();

    public BakedFace(
            BakedBerylliumModel model,
            String textureID,
            Matrix4 mat,
            BakedQuad quad,
            boolean usesAO
    ) {
        this(
                model,
                textureID,
                quad,
                usesAO,
                new float[12]
        );

        float[] oldVerts = quad.verts();

        for (int i = 0; i < verts.length; i += 3) {
            tmp.set(oldVerts[i], oldVerts[i + 1], oldVerts[i + 2]);
//            tmp.mul(mat);
            verts[i] = tmp.x;
            verts[i + 1] = tmp.y;
            verts[i + 2] = tmp.z;
        }
        System.out.println("TESSELLATING VERTEX " + Arrays.toString(verts));
        System.out.println("TESSELLATING VERTEX " + Arrays.toString(oldVerts));
    }

    public static final int NEG_X_SHOWING = 0b000001;
    public static final int POS_X_SHOWING = 0b000010;
    public static final int NEG_Y_SHOWING = 0b000100;
    public static final int POS_Y_SHOWING = 0b001000;
    public static final int NEG_Z_SHOWING = 0b010000;
    public static final int POS_Z_SHOWING = 0b100000;
    public static final int ALL_FACES_SHOWING = 0b111111;
    public static final int[] MASKS = new int[] {
            NEG_X_SHOWING,
            POS_X_SHOWING,
            NEG_Y_SHOWING,
            POS_Y_SHOWING,
            NEG_Z_SHOWING,
            POS_Z_SHOWING
    };

}
