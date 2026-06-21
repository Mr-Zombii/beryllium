package me.zombii.beryllium.client.rendering.opengl.buffers;

import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

public class TBO {

    private int tbo;
    private final int tex;
    private int size;
    private final int internalFormat;
    private boolean isBound;
    private final int drawType;

    public TBO(int sizeInBytes, int internalFormat, boolean isStatic) {
        this.tbo = GL15.glGenBuffers();
        this.tex = GL11.glGenTextures();
        this.size = sizeInBytes;
        this.internalFormat = internalFormat;
        this.drawType = isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW;

        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, tbo);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, sizeInBytes, this.drawType);
//        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);

        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, tex);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, internalFormat, tbo);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
    }

    public void resize(int bytes) {
        this.size = bytes;

        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, tbo);
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, size, this.drawType);
//        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);

        // rebind because it could fail without this on AMD Drivers
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, tex);
        GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, internalFormat, tbo);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
    }

    public void write(int offset, ByteBuffer buffer) {
        if (offset + buffer.remaining() > size)
            throw new IllegalArgumentException("Too much data for TBO buffer!");

        if (!isBound)
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, tbo);
        GL15.glBufferSubData(GL31.GL_TEXTURE_BUFFER, offset, buffer);
        if (!isBound)
            GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
    }

    private int boundUnit = -1;

    public int bind(int unit) {
        if (isBound) throw new IllegalStateException("TBO is already bound!");
        isBound = true;
        GL13.glActiveTexture(boundUnit = GL20.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, tex);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, tbo);
        return unit;
    }

    public void unbind() {
        if (!isBound) throw new IllegalStateException("TBO is not bound!");
        isBound = false;
        GL13.glActiveTexture(boundUnit);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, 0);
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, 0);
        boundUnit = -1;
    }

    public void dispose() {
        if (isBound) this.unbind();

        GL15.glDeleteBuffers(tbo);
        GL11.glDeleteTextures(tex);
    }

    public int getInternalFormat() {
        return internalFormat;
    }

    public boolean isBound() {
        return isBound;
    }

    public int getTBOHandle() {
        return tbo;
    }

    public int getTexHandle() {
        return tex;
    }

    public int getSize() {
        return size;
    }
}
