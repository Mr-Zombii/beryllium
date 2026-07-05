package me.zombii.beryllium.client.rendering.tessellation.minitess;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;
import me.zombii.beryllium.client.rendering.world.chunk.CrossChunkAccessor;

public class DefaultBlockTessallator extends BlockTessallator {

    public static final DefaultBlockTessallator INSTANCE = new DefaultBlockTessallator();

    @Override
    public void consume(
            Tessallator tessallator, Matrix4 tmp,
            CrossChunkAccessor accessor,
            Chunk chunk, BlockState state,
            int x, int y, int z,
            BakedBerylliumModel model,
            short[] skylight, short[] blocklight, byte[] aoValues,
            int visibleFacesMask, TintProvider.TintFunction tintFunction
    ) {
        model.addVertices(
                tessallator,
                skylight, blocklight, aoValues,
                visibleFacesMask, tintFunction, chunk, state,
                x, y, z
        );
    }
}
