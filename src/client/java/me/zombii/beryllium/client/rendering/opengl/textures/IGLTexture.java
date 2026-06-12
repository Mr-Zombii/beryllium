package me.zombii.beryllium.client.rendering.opengl.textures;

public interface IGLTexture {

    int getHandle();
    int bind(int unit);
    void unbind();
    boolean isBound();
    void dispose();

}
