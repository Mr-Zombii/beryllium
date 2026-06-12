package me.zombii.beryllium.client.rendering;

import dev.puzzleshq.puzzleloader.cosmic.core.registries.GenericRegistry;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.IRegistry;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.common.BerylliumCommon;

public class TintProvider {

    public static final IRegistry<TintFunction> TINT_FUNCTION_REGISTRY = new GenericRegistry<>(Identifier.of(BerylliumCommon.NAMESPACE, "tint_functions"));

    public static void register(Block block, TintFunction function) {
        TINT_FUNCTION_REGISTRY.store(Identifier.of(block.getStringId()), function);
    }

    public interface TintFunction {
        int getTint(BlockState state, BlockPosition pos, int tintIdx);
    }

}
