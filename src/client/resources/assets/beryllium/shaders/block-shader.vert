#version 330 core

uniform mat4 u_projMat;
uniform mat4 u_viewMat;
uniform mat4 u_modelMat;

uniform vec3 sunDirection = vec3(0, 1, 0);

uniform samplerBuffer u_albedoUVBuffer;
#ifdef HAS_EMISSIVE_ATLAS
uniform samplerBuffer u_emissiveUVBuffer;
#endif
#ifdef HAS_NORMAL_ATLAS
uniform samplerBuffer u_normalUVBuffer;
#endif
#ifdef HAS_MATERIAL_ATLAS
uniform samplerBuffer u_materialUVBuffer;
#endif

layout (location = 0) in vec3 a_position;
layout (location = 1) in vec3 a_normal;
layout (location = 2) in uint a_packed;
layout (location = 3) in uint a_packed_2;
layout (location = 4) in vec4 a_tint_color;
layout (location = 5) in uint a_albedoIdx;

out float v_bakedAoValue;
out vec4 v_blockLightColor;
out vec3 v_vertexNormal;
out vec3 v_vertexPosition;
out vec2 v_albedoUV;

float UV_ROTATION = (int(a_packed_2 & 0x3u) * 90) * 0.01745329;
bool UV_FLIP_U = (a_packed_2 & 0x8u) != 0u ? true : false;
bool UV_FLIP_V = (a_packed_2 & 0x4u) != 0u ? true : false;
int LOCAL_DIRECTION = int((a_packed_2 >> 7u) & 7u);

int CORNER_ID = int(a_packed_2 >> 8u) & 0xFF;
bool UV_MAX_U = (CORNER_ID & 2) != 0 ? !UV_FLIP_U : UV_FLIP_U;
bool UV_MAX_V = (CORNER_ID & 1) != 0 ? !UV_FLIP_V : UV_FLIP_V;

vec2 rotateUV(vec2 uv, float rotation, vec2 mid) {
    float angleCos = cos(rotation);
    float angleSin = sin(rotation);
    return vec2(
    angleCos * (uv.x - mid.x) + angleSin * (uv.y - mid.y) + mid.x,
    angleCos * (uv.y - mid.y) - angleSin * (uv.x - mid.x) + mid.y
    );
}

vec2 getRotatedUV(vec4 full, vec2 uv) {
    if (UV_ROTATION == 0) return uv;

    vec2 middle = full.xy + ((full.zw - full.xy) * 0.5);

    return rotateUV(uv, UV_ROTATION, middle);
}

vec4 getBlockLightColor(void) {
    int lightColorRaw = int(a_packed & 0x0FFFu);
    int lightR = (lightColorRaw & 0x0F00) >> 8;
    lightR = (lightR << 4 | lightR);
    int lightG = (lightColorRaw & 0x00F0) >> 4;
    lightG = (lightG << 4 | lightG);
    int lightB = (lightColorRaw & 0x000F);
    lightB = (lightB << 4 | lightB);

    vec4 lightColor = vec4(float(lightR) / 255.0, float(lightG) / 255.0, float(lightB) / 255.0, 1.0);
    return lightColor;
}

float getBakedAOValue(void) {
    int aoLevel = int((a_packed >> 12u) & 3u);
    switch (aoLevel) {
        case 1: return 0.25;
        case 2: return 0.50;
        case 3: return 0.75;
    }
    return 0.0;
}

vec2 getUV(void) {
    return vec2((CORNER_ID >> 1) & 1, CORNER_ID & 1);
}

vec2 getAlbedoUV(void) {
    vec4 uvs = texelFetch(u_albedoUVBuffer, int(a_albedoIdx));

    float uValue = uvs.x;
    float vValue = uvs.y;
    if (UV_MAX_U) {
        uValue = uvs.z;
    }
    if (UV_MAX_V) {
        vValue = uvs.w;
    }
    vec2 uv = vec2(uValue, vValue);
    vec2 outUV = getRotatedUV(uvs, uv);
    return outUV;
}

void main(void) {
    v_vertexPosition = a_position;
    v_vertexNormal = a_normal;
    v_bakedAoValue = getBakedAOValue();
    v_blockLightColor = getBlockLightColor();
    v_albedoUV = getAlbedoUV();

//    gl_Position = u_projMat * u_viewMat * u_modelMat * vec4(a_position, 1.0);
    gl_Position = (u_projMat * u_viewMat) * vec4(a_position, 1.0);
}