package me.zombii.beryllium.client.rendering.opengl.buffers;

import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class SSBO {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final int ssbo;
    private final int size;
    private final int index;

    public SSBO(int size, int flags) {
        this.size = size;
        this.ssbo = GL15.glGenBuffers();
        this.index = COUNTER.getAndIncrement();

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        GL44.glBufferStorage(GL45.GL_SHADER_STORAGE_BUFFER, size, flags);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void write(int offset, ByteBuffer buffer) {
        if (offset + buffer.remaining() > size)
            throw new IllegalArgumentException("Too much data for SSBO buffer!");

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        GL15.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, offset, buffer);
        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    public void read(int offset, int size, ByteBuffer buffer) {
        if (buffer.remaining() < size) throw new IllegalArgumentException("Buffer not enough to read requested SSBO data!");

        GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
        ByteBuffer bufData = GL44.glMapBufferRange(GL45.GL_SHADER_STORAGE_BUFFER, offset, size, GL44.GL_READ_ONLY);
        if (bufData == null) throw new IllegalArgumentException("Buffer failed to map! GL ERROR: " + GL11.glGetError());
        MemoryUtil.memCopy(bufData, buffer);
        GL15.glBindBuffer(GL45.GL_SHADER_STORAGE_BUFFER, 0);
    }

    private boolean isBound = false;

    public void bind(
            int programID,
            int shaderStorageBlockIndex
    ) {
        isBound = true;
        GL43.glShaderStorageBlockBinding(programID, shaderStorageBlockIndex, index);
        GL31.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, index, ssbo);
    }

    public void bind(
            int programID,
            int shaderStorageBlockIndex,
            int offset,
            int size
    ) {
        isBound = true;
        GL43.glShaderStorageBlockBinding(programID, shaderStorageBlockIndex, index);
        GL31.glBindBufferRange(GL43.GL_SHADER_STORAGE_BUFFER, index, ssbo, offset, size);
    }

    public void bind(
            int programID,
            String blockName
    ) {
        int idx = GL43.glGetProgramResourceIndex(programID, GL43.GL_SHADER_STORAGE_BLOCK, blockName);
        if (idx == GL31.GL_INVALID_INDEX) {
            throw new IllegalArgumentException(blockName + " is not a valid SSBO block name!");
        }
        bind(programID, idx);
    }

    public void bind(
            int programID,
            String blockName,
            int offset,
            int size
    ) {
        int idx = GL43.glGetProgramResourceIndex(programID, GL43.GL_SHADER_STORAGE_BLOCK, blockName);
        if (idx == GL31.GL_INVALID_INDEX) {
            throw new IllegalArgumentException(blockName + " is not a valid SSBO block name!");
        }
        bind(programID, idx, offset, size);
    }

    public void unbind() {
        if (!isBound) throw new IllegalStateException("SSBO was not bound at least once.");
        GL31.glBindBufferBase(GL43.GL_SHADER_STORAGE_BLOCK, index, 0);
    }

    public void dispose() {
        if (isBound) unbind();
        GL15.glDeleteBuffers(ssbo);
    }

    public int getIndex() {
        return index;
    }

    public int getHandle() {
        return ssbo;
    }

    public boolean isBound() {
        return isBound;
    }
}
