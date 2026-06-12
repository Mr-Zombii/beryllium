package me.zombii.beryllium.client.mixins;

import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.block.IModBlock;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.generation.model.BlockModelGenerator;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.BlockLoader;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.ISidedModelLoader;
import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.BerylliumClient;
import me.zombii.beryllium.client.rendering.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.model.loading.baking.ModelBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLoader.class)
public class MixinBlockLoader {

    @Inject(method = "generate", at = @At(value = "INVOKE", target = "Ldev/puzzleshq/puzzleloader/cosmic/game/blockloader/loading/ISidedModelLoader;loadModel(Ldev/puzzleshq/puzzleloader/cosmic/game/blockloader/generation/model/BlockModelGenerator;Z)V"))
    public void e(IModBlock block, CallbackInfoReturnable<Block> cir) {
        BlockModelGenerator[] generators = block.getModelGenerators();
        if (generators == null) return;
        for (BlockModelGenerator modelGenerator : generators) {
            ModelBaker.requestModelToBake(BerylliumModelLoader.loadVanillaBlockModel(Identifier.of(modelGenerator.getName()), modelGenerator.toString()));
//            BerylliumClient.INTERCEPTED_MODELS.add();
        }
    }

}
