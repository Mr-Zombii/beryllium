package me.zombii.beryllium.client.rendering.util;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import finalforeach.cosmicreach.entities.player.Player;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.rendering.TextureBuffer;
import finalforeach.cosmicreach.rendering.shaders.GameShader;
import finalforeach.cosmicreach.settings.GraphicsSettings;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.util.assets.GameTexture;
import finalforeach.cosmicreach.world.Sky;

public class NullCRShader extends GameShader {

    public static final NullCRShader INSTANCE = new NullCRShader();

    public NullCRShader() {
        super(Identifier.of(""), Identifier.of(""));
    }

    @Override
    public boolean reload() {
        return false;
    }

    public void bindOptionalFloat(String uniformName, float value) {
    }

    public void bindOptionalBool(String uniformName, boolean b) {
    }

    public void bindOptionalInt(String uniformName, int value) {
    }

    public int bindOptionalTextureBuffer(String uniformName, TextureBuffer texBuf, int texNum) {
        return -1;
    }

    public int bindOptionalTexture(String uniformName, GameTexture tex, int texNum) {
        return -1;
    }

    public int bindOptionalTexture(String uniformName, Texture tex, int texNum) {
        return -1;
    }

    public void bindOptionalUniform3f(String uniformName, Vector3 vec3) {
    }

    public void bindOptionalUniform3f(int location, Vector3 vec3) {
    }

    public void bindOptionalUniform3f(String uniformName, Color color) {
    }

    public void bindOptionalUniform3f(String uniformName, float x, float y, float z) {
    }

    public void bindOptionalUniform2f(String uniformName, float x, float y) {
    }

    public void bindOptionalUniform3f(int location, float x, float y, float z) {
    }

    public void bindOptionalMatrix4(String uniformName, Matrix4 mat4) {
    }

    public void bind(Camera worldCamera) {
    }

    public void unbind() {
    }

    public void bindOptionalUniform4f(String uniformName, Color color) {
    }

    public void bindOptionalUniform4f(String uniformName, float a, float b, float c, float d) {
    }

}
