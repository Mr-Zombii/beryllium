#version 330 core

uniform sampler2D u_albedoAtlas;
#ifdef HAS_EMISSIVE_ATLAS
uniform sampler2D u_emissiveAtlas;
#endif
#ifdef HAS_NORMAL_ATLAS
uniform sampler2D u_normalAtlas;
#endif
#ifdef HAS_MATERIAL_ATLAS
uniform sampler2D u_materialAtlas;
#endif

in float v_backedAoValue;
in vec4 v_blockLightColor;
in vec3 v_vertexNormal;
in vec3 v_vertexPosition;
in vec2 v_albedoUV;

out vec4 fragColor;

void main(void) {
//    fragColor = vec4(1, 1, 1, 1);
//    fragColor = vec4((v_vertexNormal + 1.0) * 0.5, 1);
//    fragColor = vec4(v_albedoUV, 0, 1);
    fragColor = texture(u_albedoAtlas, v_albedoUV);
}