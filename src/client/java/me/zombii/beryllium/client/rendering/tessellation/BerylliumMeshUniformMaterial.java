package me.zombii.beryllium.client.rendering.tessellation;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.entities.player.Player;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.settings.GraphicsSettings;
import finalforeach.cosmicreach.world.Sky;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import org.lwjgl.opengl.GL20;

public class BerylliumMeshUniformMaterial {

    int sunDirectionLoc = -1;
    int ambientWorldColorLoc = -1;
    int ambientSkyColorLoc = -1;
    int camPosLoc = -1;

    Vector3 tmp = new Vector3();

    boolean bindSky;

    public BerylliumMeshUniformMaterial(boolean bindSky) {
        this.bindSky = bindSky;
    }

    public void bind(BerylliumShaderProgram program, Camera camera) {
        if (sunDirectionLoc == -1)
            sunDirectionLoc = program.getUniformLocation("u_sunDirection");
        if (ambientWorldColorLoc == -1)
            ambientWorldColorLoc = program.getUniformLocation("u_ambientWorldColor");
        if (ambientSkyColorLoc == -1)
            ambientSkyColorLoc = program.getUniformLocation("u_ambientSkyColor");
        if (camPosLoc == -1)
            camPosLoc = program.getUniformLocation("u_cameraPos");

        if (bindSky) {
            if (sunDirectionLoc != -1) {
                Sky.currentSky.getSunDirection(tmp);
                GL20.glUniform3f(sunDirectionLoc, tmp.x, tmp.y, tmp.z);
            }
            if (ambientWorldColorLoc != -1) {
                Player player = InGame.getLocalPlayer();
                Color ambientWorldColor = player.hasNightVision ? Color.GRAY : Color.BLACK;
                GL20.glUniform3f(ambientWorldColorLoc, ambientWorldColor.r, ambientWorldColor.g, ambientWorldColor.b);
            }
            if (ambientSkyColorLoc != -1) {
                Color ambientSkyColor = Sky.currentSky.currentSkyColor;
                GL20.glUniform3f(ambientSkyColorLoc, ambientSkyColor.r, ambientSkyColor.g, ambientSkyColor.b);
            }
            program.bindUniformFloat("u_fogDensity", Sky.currentSky.fogDensity);
            program.bindUniform3f("u_playerLightColor", InGame.getUILightingTint());
            program.bindUniformFloat("u_renderDistanceInChunks", GraphicsSettings.renderDistanceInChunks.getValue());
        } else {
            if (sunDirectionLoc != -1) {
                GL20.glUniform3f(sunDirectionLoc, 0, 1, 0);
            }
            if (ambientWorldColorLoc != -1) {
                GL20.glUniform3f(ambientWorldColorLoc, 1, 1, 1);
            }
            if (ambientSkyColorLoc != -1) {
                GL20.glUniform3f(ambientSkyColorLoc, 1, 1, 1);
            }
            program.bindUniformFloat("u_fogDensity", 0);
            program.bindUniform3f("u_playerLightColor", Color.BLACK);
        }
        if (camPosLoc != -1) GL20.glUniform3f(camPosLoc, camera.position.x, camera.position.y, camera.position.z);
    }

    public int getAmbientSkyColorLoc() {
        return ambientSkyColorLoc;
    }

    public int getSunDirectionLoc() {
        return sunDirectionLoc;
    }

    public int getAmbientWorldColorLoc() {
        return ambientWorldColorLoc;
    }
}
