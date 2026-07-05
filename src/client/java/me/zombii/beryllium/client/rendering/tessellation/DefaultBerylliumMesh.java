package me.zombii.beryllium.client.rendering.tessellation;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import me.zombii.beryllium.client.BerylliumAtlases;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.common.BerylliumConfig;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DefaultBerylliumMesh {
    protected static final long startTime = System.currentTimeMillis();

    int vao = -1;
    int vbo = -1;
    int ebo = -1;

    ByteBuffer vertexBuffer;
    ByteBuffer indexBuffer;
    long vertexBufferPtr;
    long indexBufferPtr;

    int usage;
    int budget;

    public DefaultBerylliumMesh(
            int quadBudget,
            boolean isStatic
    ) {
        this.budget = quadBudget;
        vertexBuffer = MemoryUtil.memAlloc((quadBudget * 4) * Tessallator.VERTEX_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        indexBuffer = MemoryUtil.memAlloc(quadBudget * 4 * 6).order(ByteOrder.LITTLE_ENDIAN);
        vertexBufferPtr = MemoryUtil.memAddress(vertexBuffer);
        indexBufferPtr = MemoryUtil.memAddress(indexBuffer);
        this.usage = isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW;
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

        empty = false;
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
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) budget * Tessallator.VERTEX_SIZE * 4, this.usage);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, this.vertexBuffer);

        GL30.glVertexAttribIPointer(0, 2, GL30.GL_UNSIGNED_INT, Tessallator.VERTEX_SIZE, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glVertexAttribIPointer(1, 2, GL30.GL_UNSIGNED_INT, Tessallator.VERTEX_SIZE, 8);
        GL20.glEnableVertexAttribArray(1);
        GL30.glVertexAttribIPointer(2, 2, GL30.GL_UNSIGNED_INT, Tessallator.VERTEX_SIZE, 16);
        GL20.glEnableVertexAttribArray(2);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, (long) budget * 6 * 4, this.usage);
        GL15.glBufferSubData(GL15.GL_ELEMENT_ARRAY_BUFFER, 0, this.indexBuffer);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
        resized = false;
    }

    private int glBudget = 0;

    public void updateDirty() {
        if (dirty) {
            if (!initialized || budget > glBudget) {
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
    }

    public void bind() {
        GL30.glBindVertexArray(this.vao);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
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
    int emissiveUVBufferLoc = -1;
    int emissiveAtlasLoc = -1;
    int normalUVBufferLoc = -1;
    int normalAtlasLoc = -1;

    boolean initUniforms = true;


    public void render(Camera camera, BerylliumShaderProgram program, Matrix4 modelMatrix) {
        if (indexBuffer.limit() == 0) {
            return;
        }

        if (initUniforms) {
            projMatLoc = program.getUniformLocation("u_projMat");
            viewMatLoc = program.getUniformLocation("u_viewMat");
            modelMatLoc = program.getUniformLocation("u_modelMat");
            albedoAtlasLoc = program.getUniformLocation("u_albedoAtlas");
            albedoUVBufferLoc = program.getUniformLocation("u_albedoUVBuffer");
            faceUVBufferLoc = program.getUniformLocation("u_faceUVBuffer");
            emissiveUVBufferLoc = program.getUniformLocation("u_emissiveUVBuffer");
            emissiveAtlasLoc = program.getUniformLocation("u_emissiveAtlas");
            normalUVBufferLoc = program.getUniformLocation("u_normalUVBuffer");
            normalAtlasLoc = program.getUniformLocation("u_normalAtlas");
            initUniforms = false;
        }

        int unit = 0;
        int albedoAtlas = -1;
        int albedoBuffer = -1;
        int faceBuffer = -1;
        int emissiveAtlas = -1;
        int emissiveBuffer = -1;
        int normalAtlas = -1;
        int normalBuffer = -1;

        albedoAtlas = BerylliumAtlases.ALBEDO_ATLAS.bind(unit++);
        albedoBuffer = BerylliumAtlases.AlbedoUVBuffer.bind(unit++);
        if (BerylliumConfig.INSTANCE.enableEmissiveAtlas) {
            emissiveAtlas = BerylliumAtlases.EMISSIVE_ATLAS.bind(unit++);
            emissiveBuffer = BerylliumAtlases.EmissiveUVBuffer.bind(unit++);
        }
        if (BerylliumConfig.INSTANCE.enableNormalAtlas) {
            normalAtlas = BerylliumAtlases.NORMAL_ATLAS.bind(unit++);
            normalBuffer = BerylliumAtlases.NormalUVBuffer.bind(unit++);
        }
        faceBuffer = BerylliumAtlases.PerFaceUVBuffer.bind(unit++);

        program.bindUniformMat(projMatLoc, false, camera.projection);
        program.bindUniformMat(viewMatLoc, false, camera.view);
        program.bindUniformMat(modelMatLoc, false, modelMatrix);

        program.bindUniformFloat("u_time", (float)(System.currentTimeMillis() - startTime) / 1000.0F);

        if (albedoAtlasLoc != -1) GL20.glUniform1i(albedoAtlasLoc, albedoAtlas);
        if (albedoUVBufferLoc != -1) GL20.glUniform1i(albedoUVBufferLoc, albedoBuffer);
        if (faceUVBufferLoc != -1) GL20.glUniform1i(faceUVBufferLoc, faceBuffer);
        if (emissiveAtlasLoc != -1) GL20.glUniform1i(emissiveAtlasLoc, emissiveAtlas);
        if (emissiveUVBufferLoc != -1) GL20.glUniform1i(emissiveUVBufferLoc, emissiveBuffer);
        if (normalAtlasLoc != -1) GL20.glUniform1i(normalAtlasLoc, normalAtlas);
        if (normalUVBufferLoc != -1) GL20.glUniform1i(normalUVBufferLoc, normalBuffer);

        bind();
        GL20.glDrawElements(GL20.GL_TRIANGLES, indexBuffer.limit() / 4, GL20.GL_UNSIGNED_INT, 0);
        unbind();

        BerylliumAtlases.ALBEDO_ATLAS.unbind();
        BerylliumAtlases.AlbedoUVBuffer.unbind();
        BerylliumAtlases.PerFaceUVBuffer.unbind();
        if (BerylliumConfig.INSTANCE.enableEmissiveAtlas) {
            BerylliumAtlases.EMISSIVE_ATLAS.unbind();
            BerylliumAtlases.EmissiveUVBuffer.unbind();
        }
        if (BerylliumConfig.INSTANCE.enableNormalAtlas) {
            BerylliumAtlases.NORMAL_ATLAS.unbind();
            BerylliumAtlases.NormalUVBuffer.unbind();
        }
    }
    public void render(Camera camera, BerylliumShaderProgram program, boolean useDepthBuffer, Matrix4 modelMatrix) {
        boolean wasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        if (useDepthBuffer) {
            if (!wasEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        else if (wasEnabled) GL11.glDisable(GL11.GL_DEPTH_TEST);

        render(camera, program, modelMatrix);

        if (wasEnabled) {
            if (!useDepthBuffer) GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else if (useDepthBuffer) GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private boolean isDisposed;

    public void dispose() {
        if (isDisposed) return;
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
        if (isDisposed)
            throw new IllegalStateException("Tried to resize disposed mesh!");

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

    boolean empty = false;

    public void clear() {
        empty = true;
        vertexBuffer.clear();
        indexBuffer.clear();
        indexBuffer.limit(0);
        vertexBuffer.limit(0);
    }

    public boolean isEmpty() {
        return empty;
    }

    public int getVao() {
        return vao;
    }

    public int getVbo() {
        return vbo;
    }

    public int getEbo() {
        return ebo;
    }
}