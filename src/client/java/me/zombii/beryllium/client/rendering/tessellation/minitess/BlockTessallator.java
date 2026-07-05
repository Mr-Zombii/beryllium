package me.zombii.beryllium.client.rendering.tessellation.minitess;

import com.badlogic.gdx.math.Matrix4;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.GenericRegistry;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.IRegistry;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.Chunk;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;
import me.zombii.beryllium.client.rendering.world.chunk.CrossChunkAccessor;
import me.zombii.beryllium.common.BerylliumCommon;

public abstract class BlockTessallator {

    public static final IRegistry<BlockTessallator> BLOCK_TESSALLATOR_REGISTRY = new GenericRegistry<>(Identifier.of(BerylliumCommon.NAMESPACE, "tint_functions"));


    public static void register(BlockState block, BlockTessallator function) {
        BLOCK_TESSALLATOR_REGISTRY.store(Identifier.of(block.getSaveKey()), function);
    }

    public static BlockTessallator getForState(BlockState block) {
        if (BLOCK_TESSALLATOR_REGISTRY.contains(Identifier.of(block.getSaveKey()))) {
            return BLOCK_TESSALLATOR_REGISTRY.get(Identifier.of(block.getSaveKey()));
        }
        if (block.rotation[0] == 0 && block.rotation[1] == 0 && block.rotation[2] == 0) {
            return DefaultBlockTessallator.INSTANCE;
        }
        return DefaultRotatedBlockTessallator.INSTANCE;
    }

    abstract public void consume(
            Tessallator tessallator, Matrix4 tmp,
            CrossChunkAccessor accessor,
            Chunk chunk, BlockState state,
            int x, int y, int z,
            BakedBerylliumModel model,
            short[] skylight, short[] blocklight, byte[] aoValues,
            int visibleFacesMask, TintProvider.TintFunction tintFunction
    );

}
