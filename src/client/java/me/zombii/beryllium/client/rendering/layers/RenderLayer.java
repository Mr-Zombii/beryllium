package me.zombii.beryllium.client.rendering.layers;

import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.BerylliumClient;
import me.zombii.beryllium.client.rendering.opengl.shader.BerylliumShaderProgram;
import me.zombii.beryllium.common.BerylliumConfig;

public class RenderLayer {

    private final Identifier vertexShader;
    private final Identifier fragmentShader;
    private final Identifier id;
    private final boolean usesDepthBuffer;
    private final int sortOrder;
    private final BerylliumShaderProgram program;

    public RenderLayer(
            Identifier vertexShader,
            Identifier fragmentShader,
            Identifier id,
            boolean usesDepthBuffer,
            int sortOrder
    ) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.id = id;
        this.usesDepthBuffer = usesDepthBuffer;
        this.sortOrder = sortOrder;

        BerylliumConfig config = BerylliumConfig.INSTANCE;

        String prependCode = "";
        if (config.enableEmissiveAtlas)
            prependCode += "#define HAS_EMISSIVE_ATLAS\n";
        if (config.enableNormalAtlas)
            prependCode += "#define HAS_NORMAL_ATLAS\n";
        if (config.enableMaterialAtlas)
            prependCode += "#define HAS_MATERIAL_ATLAS\n";
        prependCode += "#define ATLAS_SIZE " + BerylliumClient.ATLAS_SIZE;

        this.program = new BerylliumShaderProgram(
                prependCode,
                prependCode,
                vertexShader,
                fragmentShader
        );
    }

    public Identifier getVertexShader() {
        return vertexShader;
    }

    public Identifier getFragmentShader() {
        return fragmentShader;
    }

    public Identifier getId() {
        return id;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean usesDepthBuffer() {
        return usesDepthBuffer;
    }

    public BerylliumShaderProgram getProgram() {
        return program;
    }
}
