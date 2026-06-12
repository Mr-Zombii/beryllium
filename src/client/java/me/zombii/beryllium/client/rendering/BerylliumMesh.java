package me.zombii.beryllium.client.rendering;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import me.zombii.beryllium.client.BerylliumAtlases;
import me.zombii.beryllium.client.rendering.layers.RenderLayer;
import me.zombii.beryllium.client.rendering.model.loading.baking.Tessallator;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class BerylliumMesh {

    int vao;
    int vbo;
    int ebo;

    ByteBuffer vertexBuffer;
    ByteBuffer indexBuffer;

    int usage;

    public BerylliumMesh(
            int quadBudget,
            boolean isStatic
    ) {
        vertexBuffer = MemoryUtil.memAlloc((quadBudget * 4) * Tessallator.VERTEX_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        indexBuffer = MemoryUtil.memAlloc(quadBudget * 4 * 6).order(ByteOrder.LITTLE_ENDIAN);
        this.usage = isStatic ? GL15.GL_STATIC_DRAW : GL15.GL_DYNAMIC_DRAW;
    }

    public void dump(Tessallator tessallator) {
        byte[] vertexBytes = new byte[(tessallator.getQuadsWritten() * 4) * Tessallator.VERTEX_SIZE];
        byte[] indexBytes = new byte[(tessallator.getQuadsWritten() * 6 * 4)];
        tessallator.getVertices().get(0, vertexBytes);
        tessallator.getIndices().get(0, indexBytes);

        this.indexBuffer.put(indexBytes);
        this.vertexBuffer.put(vertexBytes);
        vertexBuffer.flip();
        indexBuffer.flip();
    }

    private boolean initialized;

    public void initGL() {
        if (initialized) return;
        initialized = true;

        this.vao = GL30.glGenVertexArrays();
        this.vbo = GL15.glGenBuffers();
        this.ebo = GL15.glGenBuffers();

        GL30.glBindVertexArray(this.vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.vertexBuffer, this.usage);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.indexBuffer, this.usage);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void bind() {
        GL30.glBindVertexArray(this.vao);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);

        GL20.glVertexAttribPointer(0, 3, GL15.GL_FLOAT, false, Tessallator.VERTEX_SIZE, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 3, GL15.GL_FLOAT, false, Tessallator.VERTEX_SIZE, 12);
        GL20.glEnableVertexAttribArray(1);
        GL30.glVertexAttribIPointer(2, 1, GL15.GL_UNSIGNED_SHORT, Tessallator.VERTEX_SIZE, 24);
        GL20.glEnableVertexAttribArray(2);
        GL30.glVertexAttribIPointer(3, 1, GL15.GL_UNSIGNED_SHORT, Tessallator.VERTEX_SIZE, 26);
        GL20.glEnableVertexAttribArray(3);
        GL30.glVertexAttribPointer(4, 4, GL15.GL_UNSIGNED_BYTE, true, Tessallator.VERTEX_SIZE, 28);
        GL20.glEnableVertexAttribArray(4);
        GL30.glVertexAttribIPointer(5, 1, GL15.GL_UNSIGNED_SHORT, Tessallator.VERTEX_SIZE, 32);
        GL20.glEnableVertexAttribArray(5);
    }

    public void unbind() {
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void render(Camera camera, RenderLayer layer, Matrix4 modelMatrix) {
        boolean useDepthBuffer = layer.usesDepthBuffer();

        BerylliumShaderProgram program = layer.getProgram();
        program.bind();

        boolean depthWasEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        if (useDepthBuffer) GL11.glEnable(GL11.GL_DEPTH_TEST);
        else GL11.glDisable(GL11.GL_DEPTH_TEST);

        BerylliumAtlases.ALBEDO_ATLAS.bind(0);
        BerylliumAtlases.AlbedoUVBuffer.bind(1);

        int projMatLoc = program.getUniformLocation("u_projMat");
        int viewMatLoc = program.getUniformLocation("u_viewMat");
        int modelMatLoc = program.getUniformLocation("u_modelMat");

        program.bindUniformMat(projMatLoc, false, camera.projection);
        program.bindUniformMat(viewMatLoc, false, camera.view);
        program.bindUniformMat(modelMatLoc, false, modelMatrix);

        int albedoAtlasLoc = program.getUniformLocation("u_albedoAtlas");
        int albedoUVBufferLoc = program.getUniformLocation("u_albedoUVBuffer");

        if (albedoAtlasLoc != -1)
            GL20.glUniform1i(albedoAtlasLoc, 0);
        if (albedoUVBufferLoc != -1)
            GL20.glUniform1i(albedoUVBufferLoc, 1);

        bind();
        GL20.glDrawElements(GL20.GL_TRIANGLES, indexBuffer.capacity(), GL20.GL_UNSIGNED_INT, 0);
        unbind();

        BerylliumAtlases.ALBEDO_ATLAS.unbind();
        BerylliumAtlases.AlbedoUVBuffer.unbind();

        if (depthWasEnabled) GL11.glEnable(GL11.GL_DEPTH_TEST);
        else GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    public void dispose() {
        GL30.glDeleteVertexArrays(this.vao);
        GL15.glDeleteBuffers(this.vbo);
        GL15.glDeleteBuffers(this.ebo);

        MemoryUtil.memFree(this.vertexBuffer);
        MemoryUtil.memFree(this.indexBuffer);
    }

}
