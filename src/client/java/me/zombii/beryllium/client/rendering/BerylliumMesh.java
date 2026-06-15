package me.zombii.beryllium.client.rendering;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import me.zombii.beryllium.client.BerylliumAtlases;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.model.loading.BerylliumModelLoader;
import me.zombii.beryllium.client.rendering.model.loading.baking.Tessallator;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.common.BerylliumConfig;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BerylliumMesh {

    int vao;
    int vbo;
    int ebo;

    ByteBuffer vertexBuffer;
    ByteBuffer indexBuffer;
    long vertexBufferPtr;
    long indexBufferPtr;

    int usage;
    int budget;

    // Fix #4: cache config instead of calling getOrLoad() every bind()
    private BerylliumConfig cachedConfig;

    public BerylliumMesh(
            int quadBudget,
            boolean isStatic
    ) {
        this.budget = quadBudget;
        vertexBuffer = MemoryUtil.memAlloc((quadBudget * 4) * Tessallator.VERTEX_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        indexBuffer = MemoryUtil.memAlloc(quadBudget * 4 * 6).order(ByteOrder.LITTLE_ENDIAN);
        vertexBufferPtr = MemoryUtil.memAddress(vertexBuffer);
        indexBufferPtr = MemoryUtil.memAddress(indexBuffer);
        this.usage = isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW;
        this.cachedConfig = BerylliumConfig.getOrLoad();
    }

    private volatile long dumpVertSize = 0;
    private volatile long dumpIndSize = 0;

    public void dump(Tessallator tessallator, boolean resetPos) {
        if (resetPos) {
            this.vertexBuffer.position(0);
            this.indexBuffer.position(0);
        }

        tessallator.getVertices().position(0);
        tessallator.getIndices().position(0);
        long tessVert = MemoryUtil.memAddress(tessallator.getVertices());
        long tessInd = MemoryUtil.memAddress(tessallator.getIndices());

        long vertBytesWritten = (tessallator.getQuadsWritten() * 4L) * Tessallator.VERTEX_SIZE;
        long indBytesWritten = (tessallator.getQuadsWritten() * 6L * 4L);

        dumpVertSize = vertBytesWritten;
        dumpIndSize = indBytesWritten;

        MemoryUtil.memCopy(tessVert, vertexBufferPtr, vertBytesWritten);
        MemoryUtil.memCopy(tessInd, indexBufferPtr, indBytesWritten);

        vertexBuffer.limit(vertexBuffer.capacity());
        indexBuffer.limit(indexBuffer.capacity());

        vertexBuffer.position((int) vertBytesWritten);
        indexBuffer.position((int) indBytesWritten);

        vertexBuffer.flip();
        indexBuffer.flip();

        dirty = true;
    }

    private volatile boolean initialized;
    private volatile boolean resized;

    public void initGL() {
        if (initialized) disposeGLBuffers();
        initialized = true;
        dirty = false;

        initialized = true;

        this.vao = GL30.glGenVertexArrays();
        this.vbo = GL15.glGenBuffers();
        this.ebo = GL15.glGenBuffers();

        GL30.glBindVertexArray(this.vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.vertexBuffer, this.usage);

        GL20.glVertexAttribPointer(0, 3, GL30.GL_HALF_FLOAT, false, Tessallator.VERTEX_SIZE, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 3, GL30.GL_HALF_FLOAT, false, Tessallator.VERTEX_SIZE, 6);
        GL20.glEnableVertexAttribArray(1);
        GL30.glVertexAttribIPointer(2, 1, GL15.GL_UNSIGNED_INT, Tessallator.VERTEX_SIZE, 12);
        GL20.glEnableVertexAttribArray(2);
        GL30.glVertexAttribIPointer(3, 1, GL15.GL_UNSIGNED_INT, Tessallator.VERTEX_SIZE, 16);
        GL20.glEnableVertexAttribArray(3);
        GL30.glVertexAttribIPointer(4, 1, GL15.GL_UNSIGNED_SHORT, Tessallator.VERTEX_SIZE, 20);
        GL20.glEnableVertexAttribArray(4);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer, this.usage);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private int glBudget = 0;

    public void bind() {
        if (dirty) {
            if (!initialized || budget > glBudget) {
                // GL buffers don't exist or are too small — full reinit
                initGL();
                glBudget = budget;
            } else {
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                GL15.nglBufferSubData(GL15.GL_ARRAY_BUFFER, 0, dumpVertSize, vertexBufferPtr);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
                GL15.nglBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, dumpIndSize, indexBufferPtr);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            }
            dirty = false;
        } else if (!initialized) {
            initGL();
            glBudget = budget;
        }

        GL30.glBindVertexArray(this.vao);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);

        int ptr = 22;

        if (cachedConfig.enableEmissiveAtlas) {
            GL30.glVertexAttribIPointer(5, 1, GL15.GL_UNSIGNED_SHORT, Tessallator.VERTEX_SIZE, ptr);
            GL20.glEnableVertexAttribArray(5);
            ptr += 2;
        }
        if (cachedConfig.enableNormalAtlas) {
            GL30.glVertexAttribIPointer(6, 1, GL15.GL_UNSIGNED_SHORT, Tessallator.VERTEX_SIZE, ptr);
            GL20.glEnableVertexAttribArray(6);
            ptr += 2;
        }
        if (cachedConfig.enableMaterialAtlas) {
            GL30.glVertexAttribIPointer(7, 1, GL15.GL_UNSIGNED_SHORT, Tessallator.VERTEX_SIZE, ptr);
            GL20.glEnableVertexAttribArray(7);
        }
    }

    private void disposeJavaBuffers() {
        MemoryUtil.memFree(this.vertexBuffer);
        MemoryUtil.memFree(this.indexBuffer);
    }

    private void disposeGLBuffers() {
        GL30.glDeleteVertexArrays(this.vao);
        GL15.glDeleteBuffers(this.vbo);
        GL15.glDeleteBuffers(this.ebo);
    }

    public void unbind() {
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    int projMatLoc = -1;
    int viewMatLoc = -1;
    int modelMatLoc = -1;
    int albedoAtlasLoc = -1;
    int albedoUVBufferLoc = -1;
    int faceUVBufferLoc = -1;

    boolean initUniforms = true;

    public void render(Camera camera, RenderLayer layer, Matrix4 modelMatrix) {

        bind(); // process dirty/upload first

        if (indexBuffer.limit() == 0) {
            unbind();
            return;
        }

        boolean useDepthBuffer = layer.usesDepthBuffer();

        BerylliumShaderProgram program = layer.getProgram();
        program.bind();

        if (initUniforms) {
            projMatLoc = program.getUniformLocation("u_projMat");
            viewMatLoc = program.getUniformLocation("u_viewMat");
            modelMatLoc = program.getUniformLocation("u_modelMat");
            albedoAtlasLoc = program.getUniformLocation("u_albedoAtlas");
            albedoUVBufferLoc = program.getUniformLocation("u_albedoUVBuffer");
            faceUVBufferLoc = program.getUniformLocation("u_faceUVBuffer");
            initUniforms = false;
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);

        BerylliumAtlases.ALBEDO_ATLAS.bind(0);
        BerylliumAtlases.AlbedoUVBuffer.bind(1);
        BerylliumAtlases.PerFaceUVBuffer.bind(2);

        program.bindUniformMat(projMatLoc, false, camera.projection);
        program.bindUniformMat(viewMatLoc, false, camera.view);
        program.bindUniformMat(modelMatLoc, false, modelMatrix);

        if (albedoAtlasLoc != -1)
            GL20.glUniform1i(albedoAtlasLoc, 0);
        if (albedoUVBufferLoc != -1)
            GL20.glUniform1i(albedoUVBufferLoc, 1);
        if (faceUVBufferLoc != -1)
            GL20.glUniform1i(faceUVBufferLoc, 2);

        bind();
        GL20.glDrawElements(GL20.GL_TRIANGLES, indexBuffer.limit() / 4, GL20.GL_UNSIGNED_INT, 0);
        unbind();

        BerylliumAtlases.ALBEDO_ATLAS.unbind();
        BerylliumAtlases.AlbedoUVBuffer.unbind();
        BerylliumAtlases.PerFaceUVBuffer.unbind();
    }

    private boolean isDisposed;

    public void dispose() {
        isDisposed = true;
        disposeJavaBuffers();
        disposeGLBuffers();
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public boolean isDirty() {
        return dirty;
    }

    private boolean dirty;

    public void resize(int quadBudget) {
        if (this.budget >= quadBudget) {
            return;
        }
        quadBudget += 8;

        this.budget = quadBudget;
        this.vertexBuffer = MemoryUtil.memRealloc(this.vertexBuffer, (quadBudget * 4) * Tessallator.VERTEX_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        this.vertexBuffer.position(0);
        this.indexBuffer = MemoryUtil.memRealloc(this.indexBuffer, quadBudget * 4 * 6).order(ByteOrder.LITTLE_ENDIAN);
        this.indexBuffer.position(0);

        vertexBufferPtr = MemoryUtil.memAddress(vertexBuffer);
        indexBufferPtr = MemoryUtil.memAddress(indexBuffer);
        resized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void clear() {
        vertexBuffer.clear();
        indexBuffer.clear();
        indexBuffer.limit(0);
        vertexBuffer.limit(0);
    }
}