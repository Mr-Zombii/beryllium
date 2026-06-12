package me.zombii.beryllium.client.rendering.opengl.shader;

import com.badlogic.gdx.math.Matrix4;
import dev.puzzleshq.puzzleloader.cosmic.game.util.IndependentAssetLoader;
import finalforeach.cosmicreach.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class BerylliumShaderProgram {

    private final Identifier vertexShader;
    private final Identifier fragmentShader;
    private final String prependVertexSource;
    private final String prependFragmentSource;

    private String vertexSource;
    private String fragmentSource;

    public BerylliumShaderProgram(
            Identifier vertexShader,
            Identifier fragmentShader
    ) {
        this(null, null, vertexShader, fragmentShader);
    }

    public BerylliumShaderProgram(
            String prependVertexSource,
            String prependFragmentSource,
            Identifier vertexShader,
            Identifier fragmentShader
    ) {
        this.prependVertexSource = prependVertexSource;
        this.prependFragmentSource = prependFragmentSource;
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
    }

    private int programID;
    private int vertexShaderID;
    private int fragmentShaderID;

    private static int loadShader(int type, String source) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(GL20.glGetShaderInfoLog(id));
        }
        return id;
    }

    public void fetchAndCompile() {
        this.vertexSource = IndependentAssetLoader.loadAsset(vertexShader).getString();
        this.fragmentSource = IndependentAssetLoader.loadAsset(fragmentShader).getString();

        if (this.prependVertexSource != null) {
            this.vertexSource = this.prependVertexSource + this.vertexSource;
        }
        if (this.prependFragmentSource != null) {
            this.fragmentSource = this.prependFragmentSource + this.fragmentSource;
        }

        if (programID != -1) {
            dispose();
        }

        programID = GL20.glCreateProgram();
        vertexShaderID = loadShader(GL20.GL_VERTEX_SHADER, vertexSource);
        fragmentShaderID = loadShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

        GL20.glAttachShader(programID, vertexShaderID);
        GL20.glAttachShader(programID, fragmentShaderID);
        GL20.glLinkProgram(programID);

        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(GL20.glGetProgramInfoLog(programID));
        }
        GL20.glValidateProgram(programID);
        if (GL20.glGetProgrami(programID, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(GL20.glGetProgramInfoLog(programID));
        }
    }

    public int getUniformLocation(String uniformName) {
        return GL20.glGetUniformLocation(programID, uniformName);
    }

    public void dispose() {
        GL20.glDetachShader(programID, vertexShaderID);
        GL20.glDetachShader(programID, fragmentShaderID);

        GL20.glDeleteShader(vertexShaderID);
        GL20.glDeleteShader(fragmentShaderID);
        GL20.glDeleteProgram(programID);
    }

    public void bind() {
        GL20.glUseProgram(programID);
    }

    public boolean bindUniformMat(int loc, boolean transpose, Matrix4 matrix4) {
        if (loc == -1) return false;
        GL20.glUniformMatrix4fv(loc, transpose, matrix4.val);
        return transpose;
    }

}
