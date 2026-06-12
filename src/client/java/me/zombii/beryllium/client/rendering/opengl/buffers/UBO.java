package me.zombii.beryllium.client.rendering.opengl.buffers;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class UBO {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int ubo;
    private final int size;
    private final int index;

    public UBO(int size, boolean isStatic) {
        this.size = size;
        this.ubo = GL15.glGenBuffers();
        this.index = COUNTER.getAndIncrement();

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo);
        GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, size, isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    public void write(int offset, ByteBuffer buffer) {
        if (offset + buffer.remaining() > size)
            throw new IllegalArgumentException("Too much data for UBO buffer!");

        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, offset, buffer);
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);
    }

    private boolean isBound = false;

    public void bind(
            int programID,
            int uniformBlockIndex
    ) {
        isBound = true;
        GL31.glUniformBlockBinding(programID, uniformBlockIndex, index);
        GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, index, ubo);
    }

    public void bind(
            int programID,
            int uniformBlockIndex,
            int offset,
            int size
    ) {
        isBound = true;
        GL31.glUniformBlockBinding(programID, uniformBlockIndex, index);
        GL31.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, index, ubo, offset, size);
    }

    public void bind(
            int programID,
            String blockName
    ) {
        int idx = GL31.glGetUniformBlockIndex(programID, blockName);
        if (idx == GL31.GL_INVALID_INDEX) {
            throw new IllegalArgumentException(blockName + " is not a valid UBO block name!");
        }
        bind(programID, idx);
    }

    public void bind(
            int programID,
            String blockName,
            int offset,
            int size
    ) {
        int idx = GL31.glGetUniformBlockIndex(programID, blockName);
        if (idx == GL31.GL_INVALID_INDEX) {
            throw new IllegalArgumentException(blockName + " is not a valid UBO block name!");
        }
        bind(programID, idx, offset, size);
    }

    public void unbind() {
        if (!isBound) throw new IllegalStateException("UBO was not bound at least once.");
        GL31.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, index, 0);
    }

    public void dispose() {
        if (isBound) unbind();
        GL15.glDeleteBuffers(ubo);
    }

    public int getIndex() {
        return index;
    }

    public int getHandle() {
        return ubo;
    }

    public boolean isBound() {
        return isBound;
    }
}
