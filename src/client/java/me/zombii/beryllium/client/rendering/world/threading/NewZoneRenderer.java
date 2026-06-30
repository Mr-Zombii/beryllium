package me.zombii.beryllium.client.rendering.world.threading;

import com.badlogic.gdx.graphics.Camera;
import finalforeach.cosmicreach.rendering.IWorldRenderingMeshGenThread;
import finalforeach.cosmicreach.rendering.IZoneRenderer;
import finalforeach.cosmicreach.singletons.GameSingletons;
import finalforeach.cosmicreach.world.Chunk;
import finalforeach.cosmicreach.world.Region;
import finalforeach.cosmicreach.world.Zone;

public class NewZoneRenderer implements IZoneRenderer {

    @Override
    public void prepare(Zone zone, Camera worldCamera) {
        GameSingletons.meshGenThread.meshChunks(this);
    }

    @Override
    public void render(Zone var1, Camera var2) {
    }

    @Override
    public void removeRegion(Region var1) {

    }

    @Override
    public void unload() {

    }

    @Override
    public String getName() {
        return "NewZoneRenderer";
    }

    @Override
    public void onChunkFlaggedForRemeshing(Chunk chunk) {
        if (chunk.isGenerated()) {
            IWorldRenderingMeshGenThread meshGenThread = GameSingletons.meshGenThread;
            synchronized (meshGenThread.getAddChunkLock()) {
                meshGenThread.addChunk(chunk);
            }
        }
    }

    @Override
    public void removeChunk(Chunk chunk) {

    }

    @Override
    public void addChunk(Chunk chunk) {
        chunk.initMeshGroup(NewMeshGroup::new);
    }

    @Override
    public void onChunkMeshed(Chunk chunk) {

    }

    @Override
    public void dispose() {

    }

}
