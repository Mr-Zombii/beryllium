package me.zombii.beryllium.client.mixins;

import finalforeach.cosmicreach.blocks.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockState.class)
public class MixinBlockState {

    @Shadow
    public String modelName;

    @Inject(method = "setBlockModel", at = @At("HEAD"))
    private void setModel(String modelName, CallbackInfo ci) {
        this.modelName = modelName;
    }

}
