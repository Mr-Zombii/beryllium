package me.zombii.beryllium.client.mixins;

import com.badlogic.gdx.utils.Queue;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.BlockLoader;
import me.zombii.beryllium.client.model.baking.ModelBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockLoader.class)
public class MixinBlockLoader {

    @Inject(method = "injectIntoQueue", at = @At("TAIL"))
    private static void inject(Queue<Runnable> queue, CallbackInfo ci) {
        queue.addLast(ModelBaker::collectAndBake);
    }

}
