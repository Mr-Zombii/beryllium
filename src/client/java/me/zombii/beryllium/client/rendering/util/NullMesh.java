package me.zombii.beryllium.client.rendering.util;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.utils.FloatArray;
import finalforeach.cosmicreach.rendering.meshes.IGameMesh;

public class NullMesh implements IGameMesh {

    public static final NullMesh INSTANCE = new NullMesh();

    @Override
    public void bind(ShaderProgram var1) {

    }

    @Override
    public int getNumVertices() {
        return 0;
    }

    @Override
    public void unbind(ShaderProgram var1) {

    }

    @Override
    public void render(ShaderProgram var1, int var2) {

    }

    @Override
    public VertexData getVertices() {
        return null;
    }

    @Override
    public void setAutoBind(boolean var1) {

    }

    @Override
    public int getNumMaxVertices() {
        return 0;
    }

    @Override
    public int getVertexSizeInFloats() {
        return 0;
    }

    @Override
    public void setVertices(float[] var1) {

    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void setVertices(FloatArray var1) {

    }

    @Override
    public void dispose() {

    }
}
