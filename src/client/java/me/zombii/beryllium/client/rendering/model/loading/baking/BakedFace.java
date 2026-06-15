package me.zombii.beryllium.client.rendering.model.loading.baking;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import dev.puzzleshq.puzzleloader.cosmic.game.util.QuadUvUtil;
import me.zombii.beryllium.client.BerylliumAtlases;
import me.zombii.beryllium.client.rendering.model.PartFace;

import java.util.Arrays;

public record BakedFace(
        BakedBerylliumModel model,
        float[] verts,
        String textureID,
        int faceUvIndex,
        int uvRotation,
        boolean doAO,
        int tintIndex,
        boolean flipIndices
) {

    private static final Matrix4 mat = new Matrix4();
    private static final Vector3 tmp = new Vector3();

    public static BakedFace bake(
            BakedBerylliumModel model,
            BaseQuad quad,
            PartFace face
    ) {
        return bake(model, quad, face, mat);
    }

    public static BakedFace bake(
            BakedBerylliumModel model,
            BaseQuad quad,
            PartFace face,
            Matrix4 transform
    ) {
//        float[] newUVS = QuadUvUtil.createRotatedUv(face.getUV(), face.getUVRotation() / 90);
        int uvIdx = ModelBaker.getOrMakePerFaceIdx(face.getUV());

        float[] oldVerts = quad.verts();
        float[] newVerts = new float[12];
        System.arraycopy(quad.verts(), 0, newVerts, 0, 12);
        for (int i = 0; i < oldVerts.length; i += 3) {
            float x = oldVerts[i];
            float y = oldVerts[i + 1];
            float z = oldVerts[i + 2];

            tmp.set(x, y, z);
            tmp.mul(transform);
            newVerts[i] = tmp.x;
            newVerts[i + 1] = tmp.y;
            newVerts[i + 2] = tmp.z;
        }

        System.out.println("-------------------");
        System.out.println(Arrays.toString(oldVerts));
        System.out.println(Arrays.toString(newVerts));
        System.out.println("-------------------");

        return new BakedFace(
            model, newVerts, face.getTextureID(), uvIdx, face.getUVRotation(),
            face.usesAO(), face.getTintIndex(), quad.flipIndices()
        );
    }

    public static final int NEG_X_SHOWING = 0b0000001;
    public static final int POS_X_SHOWING = 0b0000010;
    public static final int NEG_Y_SHOWING = 0b0000100;
    public static final int POS_Y_SHOWING = 0b0001000;
    public static final int NEG_Z_SHOWING = 0b0010000;
    public static final int POS_Z_SHOWING = 0b0100000;
    public static final int NO_CULL_FACES = 0b1000000;
    public static final int ALL_FACES_SHOWING = 0b1111111;
    public static final int ALL_CULLABLE_SHOWING = 0b0111111;
    public static final int[] MASKS = new int[] {
            NEG_X_SHOWING,
            POS_X_SHOWING,
            NEG_Y_SHOWING,
            POS_Y_SHOWING,
            NEG_Z_SHOWING,
            POS_Z_SHOWING,
            NO_CULL_FACES
    };

}
