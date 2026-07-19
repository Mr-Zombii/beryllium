package me.zombii.beryllium.client.model.loading;

import dev.puzzleshq.puzzleloader.cosmic.game.blockloader.loading.ISidedModelLoader;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.rendering.blockmodels.BlockModel;
import finalforeach.cosmicreach.rendering.blockmodels.IBlockModelInstantiator;
import me.zombii.beryllium.client.model.BerylliumModel;
import me.zombii.beryllium.client.model.parts.CubePart;
import me.zombii.beryllium.client.model.parts.PartFace;
import me.zombii.beryllium.client.model.parts.PartGroup;
import me.zombii.beryllium.client.model.parts.TextureEntry;

public class NewBlockModelInstantiator implements IBlockModelInstantiator {

    @Override
    public BlockModel getInstance(String modelName, float[] rotation) {
        return ISidedModelLoader.getInstance().loadModel(modelName, rotation);
    }

    @Override
    public void createGeneratedModelInstance(BlockState blockState, BlockModel originalModel, String parentModelName, String genModelName, float[] rotation) {
        String modelName = NewClientModelLoader.CACHE.entrySet().stream().filter((e) -> e.getKey().equals(blockState.modelName)).findFirst().get().getKey();

        BerylliumModel baseModel = BerylliumModelLoader.getModel(modelName);
        BerylliumModel parentModel = VanillaBlockModelLoader.loadVanillaBlockModel(parentModelName);

        BerylliumModel model = new BerylliumModel(genModelName);
        for (TextureEntry value : baseModel.getTextureMap().values()) {
            TextureEntry newEntry = new TextureEntry(value);
            model.addTexture(newEntry);
        }
        for (PartGroup group : parentModel) {
            PartGroup newGroup = model.getOrCreateGroup(group.getName());
            newGroup.setPivot(group.getPivot());
            newGroup.setRotation(group.getRotation());
            newGroup.setParentName(group.getParentName());
            for (CubePart part : group) {
                CubePart newPart = newGroup.newPart(part.getPos(), part.getSize())
                        .setPivot(part.getPivot())
                        .setRotation(part.getRotation())
                        .setInflate(part.getInflate())
                        .setCanCollide(part.canCollide());
                PartFace[] oldFaces = part.getFaces();
                PartFace[] newFaces = newPart.getFaces();
                for (int i = 0; i < 6; i++) {
                    if (oldFaces[i] == null) {
                        newFaces[i] = null;
                        continue;
                    }
                    PartFace oldFace = oldFaces[i];
                    PartFace newFace = new PartFace(oldFace);
                    newFaces[i] = newFace;
                }
            }
        }
        model.setTransparent(originalModel.isTransparent);
        model.setRenderLayer(baseModel.getRenderLayerId());

        String modelJson = "{\"parent\": \"" + parentModelName + "\"}";
        BlockModel genModel = ISidedModelLoader.getInstance().loadModel(genModelName, modelJson, rotation);
        genModel.cullsSelf = originalModel.cullsSelf;
        genModel.isTransparent = originalModel.isTransparent;
        BerylliumModelLoader.unregister(genModelName);
        BerylliumModelLoader.register(model);

        blockState.setBlockModel(genModelName);
    }

}
