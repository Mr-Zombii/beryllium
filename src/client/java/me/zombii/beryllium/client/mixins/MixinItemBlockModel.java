package me.zombii.beryllium.client.mixins;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.items.ItemStack;
import finalforeach.cosmicreach.rendering.items.ItemModelBlock;
import finalforeach.cosmicreach.rendering.meshes.IGameMesh;
import finalforeach.cosmicreach.rendering.shaders.GameShader;
import me.zombii.beryllium.client.rendering.BerylliumMesh;
import me.zombii.beryllium.client.rendering.BerylliumMeshUniformMaterial;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.model.loading.baking.BakedBerylliumModel;
import me.zombii.beryllium.client.rendering.model.loading.baking.BakedFace;
import me.zombii.beryllium.client.rendering.model.loading.baking.ModelBaker;
import me.zombii.beryllium.client.rendering.model.loading.baking.Tessallator;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.client.rendering.util.NullCRShader;
import me.zombii.beryllium.client.rendering.util.NullMesh;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelBlock.class)
public class MixinItemBlockModel {

    @Shadow
    IGameMesh mesh;
    @Shadow
    GameShader shader;

    @Unique
    private BerylliumMesh beryllium$mesh;

    @Unique
    private RenderLayer beryllium$renderLayer;

    @Unique
    private BerylliumMeshUniformMaterial beryllium$material;

    @Unique
    private static final Tessallator beryllium$tess = new Tessallator(BakedBerylliumModel.MAX_FACES_PER_MODEL);

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onConstruct(BlockState blockState, CallbackInfo ci) {
        BakedBerylliumModel model = ModelBaker.get(blockState.modelName);
        beryllium$renderLayer = model.getModel().getRenderLayer();

        beryllium$mesh = new BerylliumMesh(6, true);
        model.addVertices(
                beryllium$tess,
                Tessallator.EMPTY_SKY_LIGHT, Tessallator.EMPTY_SHORTS,
                Tessallator.EMPTY_AO, BakedFace.ALL_FACES_SHOWING,
                (d) -> (short) -1, 0, 0, 0
        );

        beryllium$mesh.resize(beryllium$tess.getQuadsWritten());
        beryllium$mesh.dump(beryllium$tess, true);
        beryllium$tess.reset();
        this.mesh = NullMesh.INSTANCE;
        this.beryllium$material = new BerylliumMeshUniformMaterial(false);
        this.shader = NullCRShader.INSTANCE;
    }


    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lfinalforeach/cosmicreach/rendering/meshes/IGameMesh;render(Lcom/badlogic/gdx/graphics/glutils/ShaderProgram;I)V"))
    private void onRender(
            Vector3 worldPosition, Camera camera, Matrix4 modelMat,
            boolean useAmbientLighting, boolean applyFog, ItemStack itemStack,
            Color slotColor, CallbackInfo ci
    ) {
        if (beryllium$mesh.isDirty()) beryllium$mesh.updateDirty();
        BerylliumShaderProgram program = beryllium$renderLayer.getProgram();
        program.bind();

        GL11.glDepthMask(false);
        this.beryllium$material.bind(program, camera);
        beryllium$mesh.render(camera, beryllium$renderLayer.getProgram(), modelMat);
        GL11.glDepthMask(true);
    }

    @Inject(method = "dispose", at = @At("TAIL"))
    private void onDispose(CallbackInfo ci) {
        beryllium$mesh.dispose();
    }

}
