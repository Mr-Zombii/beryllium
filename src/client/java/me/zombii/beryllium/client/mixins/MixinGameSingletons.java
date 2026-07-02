package me.zombii.beryllium.client.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.puzzleshq.puzzleloader.cosmic.game.GameRegistries;
import finalforeach.cosmicreach.singletons.GameSingletons;
import me.zombii.beryllium.client.events.EventDebugBlockLoadingQueue;
import me.zombii.beryllium.client.model.baking.ModelBakingThread;
import me.zombii.beryllium.common.BerylliumConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static finalforeach.cosmicreach.singletons.GameSingletons.loadingQueue;

@Mixin(GameSingletons.class)
public class MixinGameSingletons {

    @ModifyReturnValue(method = "isDoneLoadingGameObjects", at = @At("RETURN"))
    private static boolean isDoneLoadingGameObjects(boolean original) {
        if (BerylliumConfig.INSTANCE.enableBerylliumRendering) {
            return original && ModelBakingThread.isDoneBaking();
        }
        return original;
    }

    @Inject(method = "postCreate", at = @At(value = "INVOKE", target = "Lfinalforeach/cosmicreach/blocks/Block;loadAllBlocks(Lcom/badlogic/gdx/utils/Queue;)V", shift = At.Shift.AFTER))
    private static void postCreate(CallbackInfo ci) {
        EventDebugBlockLoadingQueue event = new EventDebugBlockLoadingQueue(loadingQueue);
        GameRegistries.COSMIC_EVENT_BUS.post(event);
    }

}
