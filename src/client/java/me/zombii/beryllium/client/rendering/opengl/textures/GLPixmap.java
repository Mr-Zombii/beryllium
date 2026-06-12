package me.zombii.beryllium.client.rendering.opengl.textures;

import org.lwjgl.opengl.*;

import java.awt.image.BufferedImage;

public class GLPixmap extends PixelMap implements IGLTexture {

    private int tex;
    private boolean isBound;

    public GLPixmap(int width, int height) {
        super(width, height);

    }

    public GLPixmap(int width, int height, int texId) {
        super(width, height);
        this.tex = texId;
        this.initialized = true;
    }

    private boolean initialized;

    public void initGL() {
        if (initialized) throw new IllegalStateException("GL Texture already initialized!");
        initialized = true;

        this.tex = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);

        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, getWidth(), getHeight(), 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void upload() {
        if (!initialized) initGL();

        int[] pixels = getPixels();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, getWidth(), getHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void download() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, getPixels());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    private int boundUnit = -1;

    @Override
    public int bind(int unit) {
        if (isBound) throw new IllegalStateException("Texture is already bound!");
        isBound = true;
        GL15.glActiveTexture(boundUnit = GL20.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL20.GL_TEXTURE_2D, tex);
        return unit;
    }


    @Override
    public void unbind() {
        if (!isBound) throw new IllegalStateException("Texture is not bound!");
        isBound = false;
        GL15.glActiveTexture(boundUnit);
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, 0);
        boundUnit = -1;
    }

    @Override
    public boolean isBound() {
        return isBound;
    }

    @Override
    public int getHandle() {
        return this.tex;
    }

    @Override
    public void dispose() {
        if (isBound) this.unbind();
        GL11.glDeleteTextures(tex);
    }

    public static GLPixmap fromBufferedImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        GLPixmap pixelMap = new GLPixmap(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = bufferedImage.getRGB(x, y);
                pixelMap.setPixel(x, y, color);
            }
        }
        return pixelMap;
    }

}
