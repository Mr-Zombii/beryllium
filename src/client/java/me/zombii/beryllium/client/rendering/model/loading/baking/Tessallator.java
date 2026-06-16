package me.zombii.beryllium.client.rendering.model.loading.baking;

import me.zombii.beryllium.client.BerylliumAtlases;
import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import me.zombii.beryllium.client.rendering.model.TextureEntry;
import me.zombii.beryllium.common.BerylliumConfig;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Function;

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
            short skyLightLevel,
            short blockLightLevel,
            byte[] aoLevels,
            short faceTint,
            int aoIndex,
            int x, int y, int z
    ) {
        BerylliumModel model = face.model().getModel();

        TextureEntry entry = model.getTexture(face.textureID());
        short albedoIdx = (short) BerylliumAtlases.ALBEDO_ATLAS.get(entry.getAlbedoTexturePath().toString()).getTBOIndex();
        short emissiveIdx = 0;
        if (BerylliumConfig.INSTANCE.enableEmissiveAtlas) {
            emissiveIdx = (short) BerylliumAtlases.EMISSIVE_ATLAS.get(entry.getEmissiveTexturePath().toString()).getTBOIndex();
        }
        short normalIdx = 0;
        short materialIdx = 0;

        float[] verts = face.verts();

        addQuad(
                verts[0] + x, verts[1] + y, verts[2] + z,
                verts[3] + x, verts[4] + y, verts[5] + z,
                verts[6] + x, verts[7] + y, verts[8] + z,
                verts[9] + x, verts[10] + y, verts[11] + z,
                skyLightLevel, blockLightLevel, aoLevels,
                faceTint, aoIndex,
                face.uvRotation(),
                (short) face.faceUvIndex(),
                albedoIdx,
                emissiveIdx,
                normalIdx,
                materialIdx,
                face.flipIndices()
        );
    }

    public void addQuad(
            BaseQuad bakedQuad,
            int uvRotation,
            short faceUVIdx,
            short albedoIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            short skyLightLevel,
            short blockLightLevel,
            byte[] aoLevels,
            short faceTint,
            int aoIndex,
            int x, int y, int z
    ) {
        float[] verts = bakedQuad.verts();

        addQuad(
                verts[0] + x, verts[1] + y, verts[2] + z,
                verts[3] + x, verts[4] + y, verts[5] + z,
                verts[6] + x, verts[7] + y, verts[8] + z,
                verts[9] + x, verts[10] + y, verts[11] + z,
                skyLightLevel, blockLightLevel, aoLevels,
                faceTint, aoIndex,
                uvRotation,
                faceUVIdx,
                albedoIdx,
                emissiveIdx,
                normalIdx,
                materialIdx,
                bakedQuad.flipIndices()
        );
    }

    public void addQuad(
            float c00x, float c00y, float c00z,
            float c01x, float c01y, float c01z,
            float c10x, float c10y, float c10z,
            float c11x, float c11y, float c11z,
            short skyLightLevel, short blockLightLevel,
            byte[] aoLevels, short faceTint,
            int aoIndex,
            int uvRotation,
            short faceUVIdx,
            short albedoIdx,
            short emissiveIdx,
            short normalIdx,
            short materialIdx,
            boolean flipIndices
    ) {
        if (uvRotation % 90 != 0) throw new IllegalArgumentException("uvRotation is not a multiple of 90 or 0");
        byte uvRot = (byte) ((uvRotation % 360) / 90);

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

        byte c00ao = aoLevels[aoIndex];
        byte c01ao = aoLevels[aoIndex + 1];
        byte c10ao = aoLevels[aoIndex + 2];
        byte c11ao = aoLevels[aoIndex + 3];

        boolean flipQuad = c00ao + c11ao > c01ao + c10ao;

        if (flipQuad) {
            addVertex(c01x, c01y, c01z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c01ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 1);
            addVertex(c11x, c11y, c11z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c11ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 3);
            addVertex(c00x, c00y, c00z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c00ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 0);
            addVertex(c10x, c10y, c10z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c10ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 2);
        } else {
            addVertex(c00x, c00y, c00z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c00ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 0);
            addVertex(c01x, c01y, c01z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c01ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 1);
            addVertex(c10x, c10y, c10z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c10ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 2);
            addVertex(c11x, c11y, c11z, nX, nY, nZ, skyLightLevel, blockLightLevel, uvRot, c11ao, faceTint, faceUVIdx, albedoIdx, emissiveIdx, normalIdx, materialIdx, 3);
        }
//        flipIndices = false;
//        int[] indices = flipIndices ? BaseQuad.indices_flipped : BaseQuad.indices;
        for (int index : BaseQuad.indices) {
            this.indices.putInt(index + indexCount);
        }
        indexCount += 4;
        this.quadsWritten++;
    }

    private void addVertex(
            float x, float y, float z,
            float nX, float nY, float nZ,
            short skyLightLevel, short blockLightLevel,
            byte uvRotation, byte aoLevel,
            short vertexTint, short faceUVIdx,
            short albedoIdx, short emissiveIdx,
            short normalIdx, short materialIdx,
            int cornerID
    ) {
        int packedData = 0;
        packedData |= (cornerID & 0b11);
        packedData |= (uvRotation & 0b11) << 2;
        packedData |= (aoLevel & 0b11) << 4;
        packedData |= (blockLightLevel & 0xFFF) << 6;
        packedData |= (skyLightLevel & 0xF) << 18;

        int packedData2 = 0;
        packedData2 |= vertexTint << 16;
        packedData2 |= faceUVIdx;

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
        vertices.putInt(packedData2);
        vertices.putShort(albedoIdx);

        BerylliumConfig config = BerylliumConfig.INSTANCE;

        if (config.enableEmissiveAtlas) vertices.putShort(emissiveIdx);
        if (config.enableNormalAtlas) vertices.putShort(normalIdx);
        if (config.enableMaterialAtlas) vertices.putShort(materialIdx);
    }

    public static int VERTEX_SIZE = 22
            + ((BerylliumConfig.INSTANCE.enableEmissiveAtlas) ? 2 : 0)
            + ((BerylliumConfig.INSTANCE.enableNormalAtlas) ? 2 : 0)
            + ((BerylliumConfig.INSTANCE.enableMaterialAtlas) ? 2 : 0)
            ;

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
