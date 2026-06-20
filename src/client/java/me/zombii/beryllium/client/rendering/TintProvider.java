package me.zombii.beryllium.client.rendering;

import dev.puzzleshq.puzzleloader.cosmic.core.registries.GenericRegistry;
import dev.puzzleshq.puzzleloader.cosmic.core.registries.IRegistry;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.IdentifierCache;
import me.zombii.beryllium.common.BerylliumCommon;

import java.awt.*;

public class TintProvider {

    public static final IRegistry<TintFunction> TINT_FUNCTION_REGISTRY = new GenericRegistry<>(Identifier.of(BerylliumCommon.NAMESPACE, "tint_functions"));

    public static final TintFunction DEFAULT_TINT_FUNCTION = (s, p, i) -> (short) -1;

    public static short argb8888ToRgb565(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        r >>= 3;
        g >>= 2;
        b >>= 3;
        return (short) ((r << 11) | (g << 5) | b);
    }

    public static void register(Block block, TintFunction function) {
        TINT_FUNCTION_REGISTRY.store(Identifier.of(block.getStringId()), function);
    }

    public static TintFunction getForState(Block block) {
        Identifier identifier = IdentifierCache.getOrInsert(block.getStringId());
        if (TINT_FUNCTION_REGISTRY.contains(identifier)) {
            return TINT_FUNCTION_REGISTRY.get(identifier);
        }
        return DEFAULT_TINT_FUNCTION;
    }

    public interface TintFunction {
        short getTint(BlockState state, BlockPosition pos, int tintIdx);
    }

}
