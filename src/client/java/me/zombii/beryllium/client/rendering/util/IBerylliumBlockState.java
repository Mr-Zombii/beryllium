package me.zombii.beryllium.client.rendering.util;

import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.tessellation.TintProvider;
import me.zombii.beryllium.client.rendering.tessellation.minitess.BlockTessallator;

public interface IBerylliumBlockState {

    void setTessallator(BlockTessallator tess);
    BlockTessallator getTessallator();

    TintProvider.TintFunction getTintFunction();
    void setTintFunction(TintProvider.TintFunction tf);

    BerylliumModel getModel();
    BakedBerylliumModel getBakedModel();

}
