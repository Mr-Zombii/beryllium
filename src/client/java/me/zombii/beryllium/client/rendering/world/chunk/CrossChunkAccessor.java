package me.zombii.beryllium.client.rendering.world.chunk;

import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Zone;

import java.util.Arrays;

public class CrossChunkAccessor {

    private final Chunk[] chunks = new Chunk[3 * 3 * 3];

    public CrossChunkAccessor() {
    }

    private static int getIdx(int x, int y, int z) {
        return ((z + 1) * 9) + ((y + 1) * 3) + (x + 1);
    }

    public Chunk getChunk(int x, int y, int z) {
        return chunks[getIdx(x, y, z)];
    }

    private void setChunk(int x, int y, int z, Chunk chunk) {
        chunks[getIdx(x, y, z)] = chunk;
    }

    public BlockState getBlockState(
            Vector3 offs,
            int x, int y, int z
    ) {
        offs.add(x, y, z);

        int cx = 0;
        int cy = 0;
        int cz = 0;

        if (offs.x < 0) {
            offs.x = 15;
            cx = -1;
        } else if (offs.x > 15) {
            offs.x = 0;
            cx = 1;
        }

        if (offs.y < 0) {
            offs.y = 15;
            cy = -1;
        } else if (offs.y > 15) {
            offs.y = 0;
            cy = 1;
        }

        if (offs.z < 0) {
            offs.z = 15;
            cz = -1;
        } else if (offs.z > 15) {
            offs.z = 0;
            cz = 1;
        }

        Chunk c = getChunk(cx, cy, cz);
        if (c == null) return null;

        return c.getBlockState((int) offs.x, (int) offs.y, (int) offs.z);
    }

    public byte getSkyLight(
            Vector3 offs,
            int x, int y, int z
    ) {
        offs.add(x, y, z);

        int cx = 0;
        int cy = 0;
        int cz = 0;

        if (offs.x < 0) {
            offs.x = 15;
            cx = -1;
        } else if (offs.x > 15) {
            offs.x = 0;
            cx = 1;
        }

        if (offs.y < 0) {
            offs.y = 15;
            cy = -1;
        } else if (offs.y > 15) {
            offs.y = 0;
            cy = 1;
        }

        if (offs.z < 0) {
            offs.z = 15;
            cz = -1;
        } else if (offs.z > 15) {
            offs.z = 0;
            cz = 1;
        }

        Chunk c = getChunk(cx, cy, cz);
        if (c == null) return 0;

        return (byte) c.getSkyLight((int) offs.x, (int) offs.y, (int) offs.z);
    }

    public short getBlockLight(
            Vector3 offs,
            int x, int y, int z
    ) {
        offs.add(x, y, z);

        int cx = 0;
        int cy = 0;
        int cz = 0;

        if (offs.x < 0) {
            offs.x = 15;
            cx = -1;
        } else if (offs.x > 15) {
            offs.x = 0;
            cx = 1;
        }

        if (offs.y < 0) {
            offs.y = 15;
            cy = -1;
        } else if (offs.y > 15) {
            offs.y = 0;
            cy = 1;
        }

        if (offs.z < 0) {
            offs.z = 15;
            cz = -1;
        } else if (offs.z > 15) {
            offs.z = 0;
            cz = 1;
        }

        Chunk c = getChunk(cx, cy, cz);
        if (c == null) return 0;

        return c.getBlockLight((int) offs.x, (int) offs.y, (int) offs.z);
    }

    public void init(Zone zone, Chunk center) {
        Arrays.fill(chunks, null);

        int cx = center.chunkX;
        int cy = center.chunkY;
        int cz = center.chunkZ;

        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    Chunk chunk = zone.getChunkAtChunkCoords(cx + x, cy + y, cz + z);
                    setChunk(x, y, z, chunk);
                }
            }
        }
    }

}
