package me.zombii.beryllium.client.mixins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.TickRunner;
import finalforeach.cosmicreach.entities.GameEntity;
import finalforeach.cosmicreach.entities.ItemGameEntity;
import finalforeach.cosmicreach.items.Item;
import finalforeach.cosmicreach.items.ItemBlock;
import finalforeach.cosmicreach.singletons.GameSingletons;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.rendering.BerylliumMesh;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.model.BerylliumModel;
import me.zombii.beryllium.client.rendering.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.model.loading.baking.*;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.common.BerylliumCommon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemGameEntity.class)
public abstract class MixinItemGameEntity extends GameEntity {

    @Shadow
    public abstract Item getItem();

    @Shadow
    private float renderSize;
    @Shadow
    private float randomHoverOffsetTime;
    private BerylliumMesh mesh;
    private RenderLayer renderLayer;
    private Tessallator tessallator;
    private BakedBerylliumModel model;

    public MixinItemGameEntity(String entityTypeId) {
        super(entityTypeId);
    }

    private final Matrix4 matrix4 = new Matrix4();

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(Camera worldCamera, CallbackInfo ci) {
        if (model == null) {
            renderLayer = RenderLayers.LAYER_REGISTRY.get(Identifier.of(BerylliumCommon.NAMESPACE, "opaque-block-render-layer"));
            tessallator = new Tessallator(6);
            mesh = new BerylliumMesh(6, true);
            BerylliumModel mmodel = BerylliumModelLoader.getModel(Identifier.of("base:models/blocks/model_c4.json"));
            model = ModelBaker.get(mmodel);
            model.addVertices(tessallator, (short)0, BakedFace.ALL_FACES_SHOWING);
//            for (BakedQuad face : BakedQuad.FACES) {
//                tessallator.addQuad(
//                        face,
//                        (short)0, (short)0, (short)0,
//                        (short)0, (short)0, (byte)0,
//                        new byte[4]
//                );
//            }
            mesh.dump(tessallator);
            tessallator.dispose();
            mesh.initGL();
        }

        if (GameSingletons.isHost()) this.age += Gdx.graphics.getDeltaTime();

//        matrix4.idt();
//        matrix4.translate(position.x, position.y, position.z);

        if (this.getItem() instanceof ItemBlock) {
            float cx = worldCamera.position.x;
            float cy = worldCamera.position.y;
            float cz = worldCamera.position.z;
            tmpRenderPos.set(this.lastRenderPosition);
            TickRunner.INSTANCE.partTickSlerp(tmpRenderPos, this.position);
            this.lastRenderPosition.set(tmpRenderPos);
            tmpModelMatrix.idt();
            renderSize = 100;
            tmpModelMatrix.scl(this.renderSize);
            tmpModelMatrix.translate(-0.5F, -0.5F, -0.5F);
            tmpModelMatrix.translate(0.0F, this.renderSize / 2.0F + this.renderSize / 2.0F, 0.0F);
            worldCamera.position.sub(tmpRenderPos);
            worldCamera.update();
            mesh.render(worldCamera, renderLayer, tmpModelMatrix);
            worldCamera.position.set(cx, cy, cz);
            worldCamera.update();
        }
        ci.cancel();
    }

}
