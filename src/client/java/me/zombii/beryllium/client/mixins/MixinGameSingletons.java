package me.zombii.beryllium.client.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import finalforeach.cosmicreach.singletons.GameSingletons;
import me.zombii.beryllium.client.model.baking.ModelBakingThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameSingletons.class)
public class MixinGameSingletons {

    @ModifyReturnValue(method = "isDoneLoadingGameObjects", at = @At("RETURN"))
    private static boolean isDoneLoadingGameObjects(boolean original) {
        return original && ModelBakingThread.isDoneBaking();
    }

}
