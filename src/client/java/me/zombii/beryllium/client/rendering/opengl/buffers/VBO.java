package me.zombii.beryllium.client.rendering.opengl.buffers;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;

public class VBO {

    private final int vbo;
    private boolean isBound;
    private final int size;

    public VBO(int size, boolean isStatic) {
        this.size = size;

        this.vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, size, isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void write(int offset, ByteBuffer buffer) {
        if (offset + buffer.remaining() > size)
            throw new IllegalArgumentException("Too much data for VBO buffer!");

        if (!isBound)
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferSubData(GL31.GL_ARRAY_BUFFER, offset, buffer);
        if (!isBound)
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void bind() {
        if (isBound) throw new IllegalStateException("VBO is already bound!");
        isBound = true;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
    }

    public void unbind() {
        if (!isBound) throw new IllegalStateException("VBO is not bound!");
        isBound = false;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void dispose() {
        if (isBound) this.unbind();

        GL15.glDeleteBuffers(vbo);
    }

    public int getHandle() {
        return vbo;
    }

}
