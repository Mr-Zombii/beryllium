package me.zombii.beryllium.client.rendering.model.loading.baking;

import me.zombii.beryllium.client.BerylliumAtlases;
import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import me.zombii.beryllium.common.BerylliumConfig;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Tessallator {

    public static final byte[] EMPTY_AO = new byte[4];

    /*
    *   float 32
    *   x, y, z
    *
    *   float 32
    *   nX, nY, nZ
    *
    *   2bit AOIdx, RGB444 short coloredLightData
    *   short uvBufferIdx
    */

    ByteBuffer vertices;
    ByteBuffer indices;
    int indexCount;
    int quadsWritten;

    public Tessallator(int quadBudget) {
        this.indexCount = 0;
        this.quadsWritten = 0;
        this.vertices = MemoryUtil.memAlloc(quadBudget * (Tessallator.VERTEX_SIZE * 4)).order(ByteOrder.LITTLE_ENDIAN);
        this.indices = MemoryUtil.memAlloc(quadBudget * (4 * 6)).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void reset() {
        this.indexCount = 0;
        this.quadsWritten = 0;
        this.vertices.clear();
        this.indices.clear();
        this.vertices.position(0);
        this.indices.position(0);
    }

    public void addQuad(
            BakedFace face,
            short lightLevel,
            byte[] aoLevels,
            int tintColor
    ) {
        BerylliumModel model = face.model().getModel();

        short albedoIdx = (short) BerylliumAtlases.ALBEDO_ATLAS.get(model.getTexture(face.textureID()).getAlbedoTexturePath().toString()).getTBOIndex();
        short emissiveIdx = 0;
        short normalIdx = 0;
        short materialIdx = 0;

        float[] verts = face.verts();

        addQuad(
                verts[0], verts[1], verts[2],
                verts[3], verts[4], verts[5],
                verts[6], verts[7], verts[8],
                verts[9], verts[10], verts[11],
                lightLevel,
                albedoIdx,
                emissiveIdx,
                normalIdx,
                materialIdx,
                face.faceID(),
                aoLevels,
                face.flipIndices(),
                tintColor
        );
    }

    public void addQuad(
            BaseQuad bakedQuad,
            short lightLevel,
            short albedoIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            byte[] aoLevels,
            int tintColor
    ) {
        float[] verts = bakedQuad.verts();

        int faceID = (bakedQuad.defaultRotation() / 90) & 3;
        faceID |= (bakedQuad.flipU() ? 1 : 0) << 3;
        faceID |= (bakedQuad.flipV() ? 1 : 0) << 2;
        faceID |= (bakedQuad.direction() & 7) << 4;

        addQuad(
                verts[0], verts[1], verts[2],
                verts[3], verts[4], verts[5],
                verts[6], verts[7], verts[8],
                verts[9], verts[10], verts[11],
                lightLevel,
                albedoIdx,
                emissiveIdx,
                normalIdx,
                materialIdx,
                faceID,
                aoLevels,
                bakedQuad.flipIndices(),
                tintColor
        );
    }

    public void addQuad(
            float c00x, float c00y, float c00z,
            float c01x, float c01y, float c01z,
            float c10x, float c10y, float c10z,
            float c11x, float c11y, float c11z,
            short lightLevel,
            short albedoIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            int faceID,
            byte[] aoLevels,
            boolean flipIndices,
            int tintColor
    ) {
        float xA = c01x - c00x;
        float yA = c01y - c00y;
        float zA = c01z - c00z;

        float xB = c10x - c00x;
        float yB = c10y - c00y;
        float zB = c10z - c00z;

        float nX = (yA * zB) - (zA * yB);
        float nY = (zA * xB) - (xA * zB);
        float nZ = (xA * yB) - (yA * xB);

        float len = (float) Math.sqrt(nX*nX + nY*nY + nZ*nZ);
        nX /= len;
        nY /= len;
        nZ /= len;

        addVertex(c00x, c00y, c00z, nX, nY, nZ, albedoIdx, emissiveIdx, normalIdx, materialIdx, faceID, (byte) 0, lightLevel, aoLevels[0], tintColor);
        addVertex(c01x, c01y, c01z, nX, nY, nZ, albedoIdx, emissiveIdx, normalIdx, materialIdx, faceID, (byte) 1, lightLevel, aoLevels[1], tintColor);
        addVertex(c10x, c10y, c10z, nX, nY, nZ, albedoIdx, emissiveIdx, normalIdx, materialIdx, faceID, (byte) 2, lightLevel, aoLevels[2], tintColor);
        addVertex(c11x, c11y, c11z, nX, nY, nZ, albedoIdx, emissiveIdx, normalIdx, materialIdx, faceID, (byte) 3, lightLevel, aoLevels[3], tintColor);

        int[] indices = flipIndices ? BaseQuad.indices_flipped : BaseQuad.indices;
        for (int index : indices) {
            this.indices.putInt(index + indexCount);
        }
        indexCount += 4;
        this.quadsWritten++;
    }

    private void addVertex(
            float x, float y, float z,
            float nX, float nY, float nZ,
            short albedoIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            int faceID,
            byte cornerID,
            short lightLevel,
            byte aoLevel,
            int tintColor
    ) {
        int Xi = Float.floatToRawIntBits(x);
        int Yi = Float.floatToRawIntBits(y);
        int Zi = Float.floatToRawIntBits(z);

        int nXi = Float.floatToRawIntBits(nX);
        int nYi = Float.floatToRawIntBits(nY);
        int nZi = Float.floatToRawIntBits(nZ);

        short light = (short) ((((int)aoLevel & 3) << 12) | (lightLevel & 0x0FFF));

        vertices.putInt(Xi);
        vertices.putInt(Yi);
        vertices.putInt(Zi);

        vertices.putInt(nXi);
        vertices.putInt(nYi);
        vertices.putInt(nZi);

        vertices.putShort(light);
        vertices.putShort((short) ((((short)cornerID) << 8) | (faceID & 0xFF)));
        vertices.putInt(tintColor);

        BerylliumConfig config = BerylliumConfig.getOrLoad();
        vertices.putShort(albedoIdx);

        if (config.enableEmissiveAtlas) vertices.putShort(emissiveIdx);
        if (config.enableNormalAtlas) vertices.putShort(normalIdx);
        if (config.enableMaterialAtlas) vertices.putShort(materialIdx);
    }

    public static int VERTEX_SIZE = 34;

    static {
        BerylliumConfig config = BerylliumConfig.getOrLoad();
        if (config.enableEmissiveAtlas) VERTEX_SIZE += 2;
        if (config.enableNormalAtlas) VERTEX_SIZE += 2;
        if (config.enableMaterialAtlas) VERTEX_SIZE += 2;
    }

    public void dispose() {
        MemoryUtil.memFree(this.vertices);
        MemoryUtil.memFree(this.indices);
    }

    public void flipBuf() {
        this.vertices.flip();
        this.indices.flip();
    }

    public ByteBuffer getVertices() {
        return vertices;
    }

    public ByteBuffer getIndices() {
        return indices;
    }

    public int getQuadsWritten() {
        return quadsWritten;
    }
}
