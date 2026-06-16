package me.zombii.beryllium.client.mixins;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import dev.puzzleshq.puzzleloader.loader.util.ReflectionUtil;
import finalforeach.cosmicreach.TickRunner;
import finalforeach.cosmicreach.blocks.BlockState;
import finalforeach.cosmicreach.entities.GameEntity;
import finalforeach.cosmicreach.entities.ItemGameEntity;
import finalforeach.cosmicreach.items.Item;
import finalforeach.cosmicreach.items.ItemBlock;
import finalforeach.cosmicreach.items.ItemStack;
import finalforeach.cosmicreach.singletons.GameSingletons;
import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.rendering.BerylliumMesh;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.layers.RenderLayers;
import me.zombii.beryllium.client.rendering.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.model.loading.baking.*;
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
    @Shadow
    private ItemStack itemStack;
    private BerylliumMesh mesh;
    private RenderLayer renderLayer;
    private BakedBerylliumModel model;

    public MixinItemGameEntity(String entityTypeId) {
        super(entityTypeId);
    }

    private final Matrix4 matrix4 = new Matrix4();
    private final short[] emptyShorts = new short[]{15, 15, 15, 15, 15, 15};
    private final byte[] emptyBytes = new byte[6 * 4];

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(Camera worldCamera, CallbackInfo ci) {
        if (this.getItem() instanceof ItemBlock itemBlock) {
            if (GameSingletons.isHost()) this.age += Gdx.graphics.getDeltaTime();

            if (mesh == null && currentChunk != null) {
                renderLayer = RenderLayers.LAYER_REGISTRY.get(Identifier.of(BerylliumCommon.NAMESPACE, "opaque-block-render-layer"));
                Tessallator tessallator = new Tessallator(100);
                mesh = new BerylliumMesh(100, true);
//            model = ModelBaker.get(BerylliumModelLoader.getModel(Identifier.of("base:models/blocks/machines/pistons/model_piston_head.json")));
//            model = ModelBaker.get(BerylliumModelLoader.getModel(Identifier.of("base:models/blocks/industrial_decor/aluminium_handrail.json")));
//            model = ModelBaker.get(BerylliumModelLoader.getModel(Identifier.of("base:models/blocks/storage/cardboard_box.json")));
//            model = ModelBaker.get(BerylliumModelLoader.getModel(Identifier.of("base:models/blocks/model_c4.json")));
                BlockState state = itemBlock.getBlockState();
                try {
                    Identifier modelId = Identifier.of((String) ReflectionUtil.getField(state, "modelName").get(state));
                    model = ModelBaker.get(BerylliumModelLoader.getModel(modelId));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
//                short color = (short) 0xF800;
                model.addVertices(
                        tessallator,
                        emptyShorts, emptyShorts, emptyBytes,
                        BakedFace.ALL_FACES_SHOWING,
                        (i) -> (short) 0,
                        0, 0, 0
                );
                mesh.dump(tessallator, true);
                tessallator.dispose();
                mesh.initGL();
//                mesh = new BerylliumMesh(1, true);
//                Chunk chunk = zone.getChunkAtChunkCoords(currentChunk.chunkX, currentChunk.chunkY, currentChunk.chunkZ);
//                ChunkMeshingGroup.meshChunk(chunk, mesh);
//                mesh.initGL();

                if (mesh == null) throw new IllegalStateException("Failed to initialize BerylliumMesh");
            }

            if (mesh == null) {
                ci.cancel();
                return;
            }

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
            mesh.render(worldCamera, renderLayer, tmpModelMatrix, true);
            worldCamera.position.set(cx, cy, cz);
            worldCamera.update();
            ci.cancel();
        }
    }

}
