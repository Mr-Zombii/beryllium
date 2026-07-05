package me.zombii.beryllium.client.mixins;

import finalforeach.cosmicreach.blocks.Block;
import finalforeach.cosmicreach.blocks.BlockState;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.model.baking.ModelBaker;
import me.zombii.beryllium.client.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;
import me.zombii.beryllium.client.rendering.tessellation.minitess.BlockTessallator;
import me.zombii.beryllium.client.rendering.util.IBerylliumBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockState.class)
public abstract class MixinBlockState implements IBerylliumBlockState {

    @Shadow
    public String modelName;

    @Shadow
    public float[] rotation;

    @Shadow
    public abstract Block getBlock();

    @Unique
    private BlockTessallator tessallator;

    @Unique
    private TintProvider.TintFunction tintFunction;

    @Unique
    private BerylliumModel unbakedModel;

    @Unique
    private BakedBerylliumModel bakedModel;

    @Inject(method = "setBlockModel", at = @At("HEAD"))
    private void setModel(String modelName, CallbackInfo ci) {
        this.modelName = modelName;
    }

    @Override
    public void setTessallator(BlockTessallator tess) {
        tessallator = tess;
    }

    @Override
    public BlockTessallator getTessallator() {
        if (tessallator == null) {
            tessallator = BlockTessallator.getForState((BlockState)(Object) this);
        }
        return tessallator;
    }

    @Override
    public TintProvider.TintFunction getTintFunction() {
        if (tintFunction == null) {
            tintFunction = TintProvider.getForBlock(getBlock());
        }
        return tintFunction;
    }

    @Override
    public void setTintFunction(TintProvider.TintFunction tf) {
        tintFunction = tf;
    }

    @Override
    public BerylliumModel getModel() {
        if (unbakedModel == null) {
            unbakedModel = BerylliumModelLoader.getModel(modelName);
        }
        return unbakedModel;
    }

    @Override
    public BakedBerylliumModel getBakedModel() {
        if (bakedModel == null) {
            bakedModel = ModelBaker.get(modelName);
        }
        return bakedModel;
    }

}
