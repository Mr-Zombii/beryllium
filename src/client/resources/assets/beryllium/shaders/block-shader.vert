#version 420 core

uniform mat4 u_projMat;
uniform mat4 u_viewMat;
uniform mat4 u_modelMat;

uniform usamplerBuffer u_faceUVBuffer;
uniform usamplerBuffer u_albedoUVBuffer;

layout (location = 0) in uvec2 a_packedA;
layout (location = 1) in uvec2 a_packedB;
layout (location = 2) in uvec2 a_packedC;
layout (location = 3) in uvec2 a_animData;

vec3 position = vec3(unpackHalf2x16(a_packedA.y), unpackHalf2x16(a_packedA.x >> 16u).x).yxz;
vec3 normals = unpackSnorm4x8(((a_packedA.x & 0xFFFFu) << 8u) | ((a_packedB.y >> 24u) & 0xFFu)).zyx;
int CORNER_ID = int(a_packedB.y >> 22u) & 0x3;
int UV_ROTATION = int(a_packedB.y >> 20u) & 0x3;
int AO_LEVEL = int(a_packedB.y >> 18u) & 0x3;

int BLOCK_LIGHT_LEVELS_PACKED = int(a_packedB.y >> 2u) & 0xFFFF;
int VERTEX_TINT_PACKED = int(((a_packedB.y & 0x3u) << 14u) | ((a_packedB.x >> 18u) & 0x3FFFu));

int FACE_UV_IDX = int((a_packedB.x >> 2u) & 0xFFFFu);
int ALBEDO_UV_IDX = int(a_packedC.y >> 16u) & 0xFFFF;
int EMISSIVE_UV_IDX = int(a_packedC.y) & 0xFFFF;
int NORMAL_UV_IDX = int(a_packedC.x >> 16u) & 0xFFFF;
int MATERIAL_UV_IDX = int(a_packedC.x) & 0xFFFF;

vec2 frame_duration = unpackSnorm2x16(a_animData.x);

int ALBEDO_FRAME_COUNT = int(a_animData.y >> 16u);
float ALBEDO_FRAME_DURATION = frame_duration.y;

int EMISSIVE_FRAME_COUNT = int(a_animData.y & 0xFFFFu);
float EMISSIVE_FRAME_DURATION = frame_duration.x;

out float v_bakedAoValue;
out vec4 v_blockLightColor;
out float v_skyLight;
out vec3 v_vertexNormal;
out vec3 v_vertexPosition;
out vec2 v_albedoUV;
flat out int v_albedoFrameCount;
flat out float v_albedoFrameDuration;

#ifdef HAS_EMISSIVE_ATLAS
uniform usamplerBuffer u_emissiveUVBuffer;
out vec2 v_emissiveUV;
flat out int v_emissiveFrameCount;
flat out float v_emissiveFrameDuration;
#endif

#ifdef HAS_NORMAL_ATLAS
uniform usamplerBuffer u_normalUVBuffer;
out vec2 v_normalUV;
#endif

#ifdef HAS_MATERIAL_ATLAS
uniform usamplerBuffer u_materialUVBuffer;
out vec2 v_materialUV;
#endif

out vec4 v_tintColor;
out vec3 v_worldPos;

vec4 getTintColor(void) {
    uint r_bits = (VERTEX_TINT_PACKED >> 11u) & 0x1Fu;
    uint g_bits = (VERTEX_TINT_PACKED >> 5u) & 0x3Fu;
    uint b_bits = VERTEX_TINT_PACKED & 0x1Fu;

    vec3 rgb = vec3(
        float(r_bits) / 31,
        float(g_bits) / 63,
        float(b_bits) / 31
    );

    return vec4(rgb, 1);
}

vec4 getBlockLightColor(void) {
    int lightR = (BLOCK_LIGHT_LEVELS_PACKED & 0xF000) >> 12;
    lightR = (lightR << 4 | lightR);
    int lightG = (BLOCK_LIGHT_LEVELS_PACKED & 0x0F00) >> 8;
    lightG = (lightG << 4 | lightG);
    int lightB = (BLOCK_LIGHT_LEVELS_PACKED & 0x00F0) >> 4;
    lightB = (lightB << 4 | lightB);
    int lightA = (BLOCK_LIGHT_LEVELS_PACKED & 0x000F);
    lightA = (lightA << 4 | lightA);

    return vec4(float(lightR) / 255.0, float(lightG) / 255.0, float(lightB) / 255.0, float(lightA) / 255.0);
}

float getBakedAOValue(void) {
    return (float(AO_LEVEL) / 4) + .25f;
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

vec2 getUV(uvec2 offs) {
    vec2 faceMin = vec2(offs + FACE_UV_MIN) / 1024;
    vec2 faceMax = vec2(offs + FACE_UV_MAX) / 1024;

    int vert_id = CORNER_ID;
    vec2 uv = vec2(0.0, 0.0);

    uv = createRotatedUv(uint(vert_id), faceMin, faceMax, UV_ROTATION);
    return uv;
}

vec2 getAlbedoUV(void) { return getUV(texelFetch(u_albedoUVBuffer, ALBEDO_UV_IDX).xy); }
#ifdef HAS_EMISSIVE_ATLAS
vec2 getEmissiveUV(void) { return getUV(texelFetch(u_emissiveUVBuffer, EMISSIVE_UV_IDX).xy); }
#endif
#ifdef HAS_NORMAL_ATLAS
vec2 getNormalUV(void) { return getUV(texelFetch(u_normalUVBuffer, NORMAL_UV_IDX).xy); }
#endif

void main(void) {
    v_vertexPosition = position;
    v_vertexNormal = normals;
    v_bakedAoValue = getBakedAOValue();
    v_blockLightColor = getBlockLightColor();
    v_albedoUV = getAlbedoUV();
    v_albedoFrameCount = ALBEDO_FRAME_COUNT;
    v_albedoFrameDuration = ALBEDO_FRAME_DURATION;

    v_tintColor = getTintColor();
    #ifdef HAS_EMISSIVE_ATLAS
    v_emissiveUV = getEmissiveUV();
    v_emissiveFrameCount = EMISSIVE_FRAME_COUNT;
    v_emissiveFrameDuration = EMISSIVE_FRAME_DURATION;
    #endif
    #ifdef HAS_NORMAL_ATLAS
    v_normalUV = getNormalUV();
    #endif
    v_worldPos = (u_modelMat * vec4(position, 1.0)).xyz;

//    gl_Position = (u_projMat * u_viewMat) * vec4(a_position, 1.0);
    gl_Position = (u_projMat * u_viewMat * u_modelMat) * vec4(position, 1.0);
}