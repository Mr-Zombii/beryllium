#version 330 core

uniform mat4 u_projMat;
uniform mat4 u_viewMat;
uniform mat4 u_modelMat;

uniform usamplerBuffer u_faceUVBuffer;
uniform usamplerBuffer u_albedoUVBuffer;

layout (location = 0) in vec3 a_position;
layout (location = 1) in vec3 a_normal;
layout (location = 2) in uint a_packed;
layout (location = 3) in uint a_packedIndices;
layout (location = 4) in uint a_albedoIdx;

#ifdef HAS_EMISSIVE_ATLAS
uniform usamplerBuffer u_emissiveUVBuffer;
layout (location = 5) in uint a_emissiveIdx;
#endif

#ifdef HAS_NORMAL_ATLAS
uniform usamplerBuffer u_normalUVBuffer;
layout (location = 6) in uint a_normalIdx;
#endif

#ifdef HAS_MATERIAL_ATLAS
uniform usamplerBuffer u_materialUVBuffer;
layout (location = 7) in uint a_materialIdx;
#endif

out float v_bakedAoValue;
out vec4 v_blockLightColor;
out float v_skyLight;
out vec3 v_vertexNormal;
out vec3 v_vertexPosition;
out vec2 v_albedoUV;
out vec2 v_emissiveUV;
out vec4 v_tintColor;
out vec3 v_worldPos;

int CORNER_ID = int(a_packed & 3u);
int UV_ROTATION = int(a_packed >> 2u) & 3;
int AO_LEVEL_PACKED = int(a_packed >> 4u) & 3;
int LIGHT_COLOR_PACKED = int(a_packed >> 6u) & 0xFFF;
int SKY_LIGHT = int(a_packed >> 18u) & 0xF;

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
//    lightR = (lightR << 4 | lightR);
    int lightG = (LIGHT_COLOR_PACKED & 0x00F0) >> 4;
//    lightG = (lightG << 4 | lightG);
    int lightB = (LIGHT_COLOR_PACKED & 0x000F);
//    lightB = (lightB << 4 | lightB);

    vec4 lightColor = vec4(float(lightR) / 255.0, float(lightG) / 255.0, float(lightB) / 255.0, 1.0);
    return lightColor;
}

float getBakedAOValue(void) {
    return (float(AO_LEVEL_PACKED) / 4) + .25f;
//    switch (AO_LEVEL_PACKED) {
//        case 1: return 0.25;
//        case 2: return 0.50;
//        case 3: return 0.75;
//    }
//    return 0.0;
}

vec2 getUV(void) {
    return vec2((CORNER_ID >> 1) & 1, CORNER_ID & 1);
}

uvec4 FACE_UV_RANGE = texelFetch(u_faceUVBuffer, FACE_UV_IDX);
uvec2 FACE_UV_MIN = FACE_UV_RANGE.xy;
uvec2 FACE_UV_MAX = FACE_UV_RANGE.zw;
uvec2 FACE_UV_SIZE = FACE_UV_RANGE.zw - FACE_UV_MIN;

/*
    rotationStyle is CCW

    rotation:
        0 = normal
        1 = 90°
        2 = 180°
        3 = 270°
*/
vec2 createRotatedUv(uint corner, vec2 min, vec2 max, int rotation) {
    if (rotation == 0) {
        if (corner == 3u) return min;
        if (corner == 2u) return vec2(min.x, max.y);
        if (corner == 1u) return vec2(max.x, min.y);
        return max;
    }

    if (rotation == 3) {
        if (corner == 3u) return vec2(max.x, min.y);
        if (corner == 2u) return min;
        if (corner == 1u) return max;
        return vec2(min.x, max.y);
    }

    if (rotation == 2) {
        if (corner == 3u) return max;
        if (corner == 2u) return vec2(max.x, min.y);
        if (corner == 1u) return vec2(min.x, max.y);
        return min;
    }

    if (corner == 3u) return vec2(min.x, max.y);
    if (corner == 2u) return max;
    if (corner == 1u) return min;
    return vec2(max.x, min.y);
}

vec2 getAlbedoUV(void) {
    uvec2 ALBEDO_UV_OFFS = texelFetch(u_albedoUVBuffer, int(a_albedoIdx)).xy;
    vec2 faceMin = vec2(ALBEDO_UV_OFFS + FACE_UV_MIN) / 1024;
    vec2 faceMax = vec2(ALBEDO_UV_OFFS + FACE_UV_MAX) / 1024;

    int vert_id = CORNER_ID;
    vec2 uv = vec2(0.0, 0.0);

    uv = createRotatedUv(uint(vert_id), faceMin, faceMax, UV_ROTATION);
    return uv;
}

#ifdef HAS_EMISSIVE_ATLAS
    vec2 getEmissiveUV(void) {
        uvec2 EMISSIVE_UV_OFFS = texelFetch(u_emissiveUVBuffer, int(a_emissiveIdx)).xy;
        vec2 faceMin = vec2(EMISSIVE_UV_OFFS + FACE_UV_MIN) / 1024;
        vec2 faceMax = vec2(EMISSIVE_UV_OFFS + FACE_UV_MAX) / 1024;

        int vert_id = CORNER_ID;
        vec2 uv = vec2(0.0, 0.0);

        uv = createRotatedUv(uint(vert_id), faceMin, faceMax, UV_ROTATION);
        return uv;
    }
#endif

void main(void) {
    v_vertexPosition = a_position;
    v_vertexNormal = a_normal;
    v_bakedAoValue = getBakedAOValue();
    v_blockLightColor = getBlockLightColor();
    v_albedoUV = getAlbedoUV();
    v_tintColor = getTintColor();
    v_skyLight = float(SKY_LIGHT) / 15.0;
    #ifdef HAS_EMISSIVE_ATLAS
    v_emissiveUV = getEmissiveUV();
    #endif
    v_worldPos = (u_modelMat * vec4(a_position, 1.0)).xyz;

//    gl_Position = (u_projMat * u_viewMat) * vec4(a_position, 1.0);
    gl_Position = (u_projMat * u_viewMat * u_modelMat) * vec4(a_position, 1.0);
}