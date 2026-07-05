package me.zombii.beryllium.client.mixins;

import com.badlogic.gdx.utils.Queue;
import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.BlockLoader;
import finalforeach.cosmicreach.util.assets.GameAssetLoader;
import it.unimi.dsi.fastutil.Pair;
import me.zombii.beryllium.client.model.baking.ModelBaker;
import me.zombii.beryllium.client.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.common.BerylliumConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(BlockLoader.class)
public class MixinBlockLoader {

    @Inject(method = "injectIntoQueue", at = @At("TAIL"))
    private static void inject(Queue<Runnable> queue, CallbackInfo ci) {
        if (BerylliumConfig.INSTANCE.enableBerylliumRendering) {
            queue.addLast(() -> {
                Map<String, Pair<String, String>> models = new HashMap<>();
                GameAssetLoader.forEachAsset("models", ".json", (p, f) -> {
                    String id = BerylliumModelLoader.registerBerylliumBlockModelID(p, f.readString());
                    if (id != null) {
                        models.put(id, Pair.of(p, f.readString()));
                    }
                });

                for (String id : BerylliumModelLoader.blockIdsToLoad) {
                    Pair<String, String> pair = models.get(id);
                    BerylliumModelLoader.loadBerylliumModel(pair.left(), pair.right());
                }
            });


            queue.addLast(ModelBaker::collectAndBake);
        }
    }

}
