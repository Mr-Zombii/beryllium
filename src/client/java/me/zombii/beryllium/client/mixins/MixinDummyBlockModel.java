package me.zombii.beryllium.client.mixins;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import finalforeach.cosmicreach.rendering.blockmodels.DummyBlockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DummyBlockModel.class)
public class MixinDummyBlockModel {

    @Shadow
    private Array<BoundingBox> boundingBoxes;

    /**
     * @author Mr-Zombii
     * @reason
     */
    @Overwrite
    public void getAllBoundingBoxes(Array<BoundingBox> boundingBoxes, int bx, int by, int bz) {
        boundingBoxes.size = 0;

        Object[] list = this.boundingBoxes.items;
        for (int i = 0; i < list.length; i++) {
            BoundingBox mbb = (BoundingBox) list[i];
            if (mbb == null) continue;
            BoundingBox bb;
            if (boundingBoxes.items.length > boundingBoxes.size) {
                bb = boundingBoxes.items[boundingBoxes.size];
                if (bb == null) {
                    bb = new BoundingBox();
                }
            } else {
                bb = new BoundingBox();
            }

            bb.min.set(mbb.min);
            bb.max.set(mbb.max);
            bb.min.add(bx, by, bz);
            bb.max.add(bx, by, bz);
            bb.update();
            boundingBoxes.add(bb);
        }
    }

}
