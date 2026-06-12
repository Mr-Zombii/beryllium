package me.zombii.beryllium.client;

import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.rendering.opengl.buffers.TBO;
import me.zombii.beryllium.client.rendering.opengl.textures.atlas.GLAtlas;
import me.zombii.beryllium.common.BerylliumCommon;
import me.zombii.beryllium.common.BerylliumConfig;

public class BerylliumAtlases {

    public static GLAtlas ALBEDO_ATLAS;
    public static GLAtlas EMISSIVE_ATLAS;
    public static GLAtlas NORMAL_ATLAS;
    public static GLAtlas MATERIAL_ATLAS;
    public static TBO AlbedoUVBuffer;
    public static TBO EmissiveUVBuffer;
    public static TBO NormalUVBuffer;
    public static TBO MaterialUVBuffer;

    public static TBO PerFaceUVBuffer;

    public static void initAtlases() {
        BerylliumConfig config = BerylliumConfig.getOrLoad();

        ALBEDO_ATLAS = new GLAtlas(Identifier.of(BerylliumCommon.NAMESPACE, "albedo-atlas"), 1024, 1024);

        if (config.enableEmissiveAtlas)
            BerylliumAtlases.EMISSIVE_ATLAS = new GLAtlas(Identifier.of(BerylliumCommon.NAMESPACE, "albedo-atlas"), 1024, 1024);
        if (config.enableNormalAtlas)
            BerylliumAtlases.NORMAL_ATLAS = new GLAtlas(Identifier.of(BerylliumCommon.NAMESPACE, "normal-atlas"), 1024, 1024);
        if (config.enableMaterialAtlas)
            BerylliumAtlases.MATERIAL_ATLAS = new GLAtlas(Identifier.of(BerylliumCommon.NAMESPACE, "material-atlas"), 1024, 1024);
    }

}
