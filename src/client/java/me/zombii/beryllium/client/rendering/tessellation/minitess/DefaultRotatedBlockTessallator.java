package me.zombii.beryllium.client.rendering.tessellation.minitess;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;
import me.zombii.beryllium.client.rendering.world.chunk.CrossChunkAccessor;

public class DefaultRotatedBlockTessallator extends BlockTessallator {

    public static final DefaultRotatedBlockTessallator INSTANCE = new DefaultRotatedBlockTessallator();

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
        Matrix4 tessMat = tessallator.getTransform();
        tessMat.translate(.5f + x, .5f + y, .5f + z);
        tessMat.rotate(Vector3.Z, state.rotation[2]);
        tessMat.rotate(Vector3.Y, 360-state.rotation[1]);
        tessMat.rotate(Vector3.X, 360-state.rotation[0]);
        tessMat.translate(-.5f - x, -.5f - y, -.5f - z);

        model.addVertices(
                tessallator,
                skylight, blocklight, aoValues,
                visibleFacesMask, tintFunction, chunk, state,
                x, y, z
        );
    }
}
