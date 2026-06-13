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
            short tintColor
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
                face.uvRotation(),
                lightLevel,
                albedoIdx,
                (short) face.faceUvIndex(),
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
            float uvRotation,
            short lightLevel,
            short albedoIdx,
            short faceUVIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            byte[] aoLevels,
            short tintColor
    ) {
        float[] verts = bakedQuad.verts();

        addQuad(
                verts[0], verts[1], verts[2],
                verts[3], verts[4], verts[5],
                verts[6], verts[7], verts[8],
                verts[9], verts[10], verts[11],
                uvRotation,
                lightLevel,
                albedoIdx,
                faceUVIdx,
                emissiveIdx,
                normalIdx,
                materialIdx,
                bakedQuad.direction(),
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
            float uvRotation,
            short lightLevel,
            short albedoIdx,
            short faceUVIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            int direction,
            byte[] aoLevels,
            boolean flipIndices,
            short tintColor
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

        addVertex(c00x, c00y, c00z, nX, nY, nZ, uvRotation, albedoIdx, faceUVIdx, emissiveIdx, normalIdx, materialIdx, direction, 0, lightLevel, aoLevels[0], tintColor);
        addVertex(c01x, c01y, c01z, nX, nY, nZ, uvRotation, albedoIdx, faceUVIdx, emissiveIdx, normalIdx, materialIdx, direction, 1, lightLevel, aoLevels[1], tintColor);
        addVertex(c10x, c10y, c10z, nX, nY, nZ, uvRotation, albedoIdx, faceUVIdx, emissiveIdx, normalIdx, materialIdx, direction, 2, lightLevel, aoLevels[2], tintColor);
        addVertex(c11x, c11y, c11z, nX, nY, nZ, uvRotation, albedoIdx, faceUVIdx, emissiveIdx, normalIdx, materialIdx, direction, 3, lightLevel, aoLevels[3], tintColor);

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
            float uvRotation,
            short albedoIdx,
            short faceUVIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            int direction,
            int cornerID,
            short lightLevel,
            byte aoLevel,
            short tintColor
    ) {
        int packedData = 0;
        packedData |= (cornerID & 0b11);
        packedData |= (direction & 0b111) << 2;
        packedData |= (aoLevel & 0b11) << 5;
        packedData |= (lightLevel & 0xFFF) << 7;

        int packedIndices = 0;
        packedIndices |= tintColor << 16;
        packedIndices |= faceUVIdx;

        short Xi = Float.floatToFloat16(x);
        short Yi = Float.floatToFloat16(y);
        short Zi = Float.floatToFloat16(z);

        short nXi = Float.floatToFloat16(nX);
        short nYi = Float.floatToFloat16(nY);
        short nZi = Float.floatToFloat16(nZ);

        vertices.putShort(Xi);
        vertices.putShort(Yi);
        vertices.putShort(Zi);

        vertices.putShort(nXi);
        vertices.putShort(nYi);
        vertices.putShort(nZi);

        vertices.putInt(packedData);
        vertices.putInt(packedIndices);
        vertices.putShort(Float.floatToFloat16(uvRotation));
        vertices.putShort(albedoIdx);

        BerylliumConfig config = BerylliumConfig.getOrLoad();

        if (config.enableEmissiveAtlas) vertices.putShort(emissiveIdx);
        if (config.enableNormalAtlas) vertices.putShort(normalIdx);
        if (config.enableMaterialAtlas) vertices.putShort(materialIdx);
    }

    public static int VERTEX_SIZE = 24;

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
