#version 330 core

uniform mat4 u_projMat;
uniform mat4 u_viewMat;
uniform mat4 u_modelMat;

uniform vec3 sunDirection = vec3(0, 1, 0);

uniform samplerBuffer u_faceUVBuffer;
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
layout (location = 3) in uint a_packedIndices;
layout (location = 4) in uint a_uvRotation;
layout (location = 5) in uint a_albedoIdx;
//layout (location = 6) in uint a_emissiveIdx;
//layout (location = 7) in uint a_normalIdx;
//layout (location = 8) in uint a_materialIdx;

out float v_bakedAoValue;
out vec4 v_blockLightColor;
out vec3 v_vertexNormal;
out vec3 v_vertexPosition;
out vec2 v_albedoUV;
out vec4 v_tintColor;

int CORNER_ID = int(a_packed & 3u);
int FACE_ID = int(a_packed >> 2u) & 7;
int AO_LEVEL_PACKED = int(a_packed >> 5u) & 3;
int LIGHT_COLOR_PACKED = int(a_packed >> 7u) & 0xFFF;

uint TINT_COLOR_PACKED = (a_packedIndices >> 16u) & 0xFFFFu;
int FACE_UV_IDX = int(a_packedIndices & 0xFFFFu);

vec4 getTintColor(void) {
    uint r_bits = (TINT_COLOR_PACKED >> 11u) & 0x1Fu;
    uint g_bits = (TINT_COLOR_PACKED >> 5u) & 0x3Fu;
    uint b_bits = TINT_COLOR_PACKED & 0x1Fu;

    vec3 rgb = vec3(
        float(r_bits) / 31,
        float(g_bits) / 63,
        float(b_bits) / 31
    );

    return vec4(rgb, 1);
}

vec4 getBlockLightColor(void) {
    int lightR = (LIGHT_COLOR_PACKED & 0x0F00) >> 8;
    lightR = (lightR << 4 | lightR);
    int lightG = (LIGHT_COLOR_PACKED & 0x00F0) >> 4;
    lightG = (lightG << 4 | lightG);
    int lightB = (LIGHT_COLOR_PACKED & 0x000F);
    lightB = (lightB << 4 | lightB);

    vec4 lightColor = vec4(float(lightR) / 255.0, float(lightG) / 255.0, float(lightB) / 255.0, 1.0);
    return lightColor;
}

float getBakedAOValue(void) {
    switch (AO_LEVEL_PACKED) {
        case 1: return 0.25;
        case 2: return 0.50;
        case 3: return 0.75;
    }
    return 0.0;
}

vec2 getUV(void) {
    return vec2((CORNER_ID >> 1) & 1, CORNER_ID & 1);
}

float FACE_UV_ROTATION = a_uvRotation * 0.01745329;

float CORRECTIVE_UV_ROTATION = FACE_ID == 1 ? 4.712389 : (FACE_ID == 0 ? 0 : 0);
bool CORRECTIVE_UV_FLIP_U = bool(FACE_ID == 4);
bool CORRECTIVE_UV_FLIP_V = bool(FACE_ID == 1 || FACE_ID == 2 || FACE_ID == 4 || FACE_ID == 5);

bool UV_MAX_U = (CORNER_ID & 2) != 0 ? true : false;
bool UV_MAX_V = (CORNER_ID & 1) != 0 ? true : false;

vec4 ALBEDO_UV_RANGE = texelFetch(u_albedoUVBuffer, int(a_albedoIdx));
vec2 ALBEDO_UV_MIN = ALBEDO_UV_RANGE.xy;
vec2 ALBEDO_UV_SIZE = ALBEDO_UV_RANGE.zw - ALBEDO_UV_MIN;

vec4 FACE_UV_RANGE = texelFetch(u_faceUVBuffer, FACE_UV_IDX);
vec2 FACE_UV_MIN = FACE_UV_RANGE.xy;
vec2 FACE_UV_SIZE = FACE_UV_RANGE.zw - FACE_UV_MIN;

vec2 rotateUV(vec2 uv, float rotation, vec2 mid) {
    float angleCos = cos(rotation);
    float angleSin = sin(rotation);
    return vec2(
    angleCos * (uv.x - mid.x) + angleSin * (uv.y - mid.y) + mid.x,
    angleCos * (uv.y - mid.y) - angleSin * (uv.x - mid.x) + mid.y
    );
}

// I have given up on this function for today.
vec2 getAlbedoUV() {
    return vec2(0, 0);
}

void main(void) {
    v_vertexPosition = a_position;
    v_vertexNormal = a_normal;
    v_bakedAoValue = getBakedAOValue();
    v_blockLightColor = getBlockLightColor();
    v_albedoUV = getAlbedoUV();
    v_tintColor = getTintColor();

    gl_Position = (u_projMat * u_viewMat) * vec4(a_position, 1.0);
}