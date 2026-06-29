package me.zombii.beryllium.client.rendering.world.chunk;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.entities.player.Player;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.settings.GraphicsSettings;
import finalforeach.cosmicreach.world.Sky;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.client.rendering.tessellation.BerylliumMesh;
import me.zombii.beryllium.client.rendering.tessellation.Tessallator;

public class ChunkMesh extends BerylliumMesh {

    private final LayeredChunkMesh parent;

    private final Vector3 sunDirection = new Vector3();

    public ChunkMesh(
            LayeredChunkMesh parent
    ) {
        super(128, true);
        this.parent = parent;
    }

    public LayeredChunkMesh getParent() {
        return parent;
    }

    @Override
    public void dump(Tessallator tessallator, boolean resetPos) {
        super.dump(tessallator, resetPos);
    }

    @Override
    public void render(Camera camera, BerylliumShaderProgram program, Matrix4 modelMatrix) {
        Sky sky = Sky.currentSky;

        sky.getSunDirection(sunDirection);
        program.bindUniform3f("u_sunDirection", sunDirection);
        program.bindUniform3f("u_ambientSkyColor", sky.currentAmbientColor);

        Player player = InGame.getLocalPlayer();
        program.bindUniform3f("u_ambientWorldColor", player.hasNightVision ? Color.GRAY : Color.BLACK);
        program.bindUniformFloat("u_fogDensity", sky.fogDensity);

        program.bindUniformFloat("u_renderDistanceInChunks", GraphicsSettings.renderDistanceInChunks.getValue());
        program.bindUniform3f("u_playerLightColor", InGame.getUILightingTint());

        super.render(camera, program, modelMatrix);
    }
}