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

uniform vec3 u_sunDirection = vec3(0, 1, 0);
uniform vec3 u_ambientWorldColor = vec3(1, 1, 1);
uniform vec3 u_ambientSkyColor = vec3(1, 1, 1);
uniform vec3 u_cameraPos;

in float v_bakedAoValue;
in vec4 v_blockLightColor;
in vec3 v_vertexNormal;
in vec3 v_vertexPosition;
in vec2 v_albedoUV;
in vec4 v_tintColor;
#ifdef HAS_EMISSIVE_ATLAS
in vec2 v_emissiveUV;
#endif
#ifdef HAS_NORMAL_ATLAS
in vec2 v_normalUV;
#endif
in vec3 v_worldPos;
out vec4 fragColor;

vec4 tintColor(vec4 c) {
    float threshold = 0.01;
    if (
    abs(c.r - c.g) < threshold &&
    abs(c.g - c.b) < threshold
    ) {
        c.r *= v_tintColor.r;
        c.g *= v_tintColor.g;
        c.b *= v_tintColor.b;
    }
    return c;
}

#define USE_NORMAL_AS_ALBEDO 0
#define USE_BLOCK_LIGHT 1
#define USE_SKY_LIGHT 1

void renderMode0(void) {
//    vec4 newNormal = normalize(texture(u_normalAtlas, v_normalUV) * 2 - 1);
//    newNormal.w = 1;

    //    fragColor = vec4(1, 1, 1, 1);
    //    fragColor = vec4((v_vertexNormal + 1.0) * 0.5, 1);
    //    fragColor = vec4(v_albedoUV, 0, 1);
    #if USE_NORMAL_AS_ALBEDO == 0
    vec4 albedoColor = tintColor(texture(u_albedoAtlas, v_albedoUV));
    #else
    vec4 albedoColor = vec4((v_vertexNormal + 1.0) * 0.5, 1);
    #endif
    if (albedoColor.a == 0) discard;

    float noonDot = dot(u_sunDirection, v_vertexNormal);
    noonDot = sign(noonDot) * sqrt(abs(noonDot));
    vec3 ambientBlockColor = u_ambientSkyColor * max(noonDot, 0.5);

    // https://www.desmos.com/calculator/rlhvpsykrx
    #if USE_SKY_LIGHT == 0
        float skyLight = 0;
    #else
        float skyLight = v_blockLightColor.a;
    #endif


    #if USE_BLOCK_LIGHT == 0
        vec3 lightTint = skyLight * ambientBlockColor;
    #else
        vec3 it =  pow(15* v_blockLightColor.rgb / 25.0, vec3(2));
        vec3 t = 30.0/(1.0 + exp(-15.0 * it)) - 15;
        vec3 lightTint = max(t / 15, skyLight * ambientBlockColor);
    #endif

    fragColor = vec4(albedoColor.rgb * lightTint, albedoColor.a);

    #ifdef HAS_EMISSIVE_ATLAS
    vec4 emissiveColor = tintColor(texture(u_emissiveAtlas, v_emissiveUV));
    fragColor.rgb = max(fragColor.rgb, emissiveColor.rgb * emissiveColor.a);
    #endif

    fragColor.rgb = max(fragColor.rgb, albedoColor.rgb * u_ambientWorldColor);
//    fragColor.rgb = max(fragColor.rgb, albedoColor.rgb * u_ambientWorldColor);

//    fragColor.rgb *= v_bakedAoValue;
//    fragColor.rgb = max(fragColor.rgb, albedoColor.rgb);
}

void renderMode1(void) {
    fragColor.rgba = vec4(vec3(v_blockLightColor.a), 1);
    fragColor.rgba *= vec4(vec3(v_bakedAoValue), 1);
}

void renderMode2(void) {
    fragColor = vec4((v_vertexNormal + 1.0) * 0.5, 1);
}

void renderMode3(void) {
    fragColor.rgba = vec4(vec3(v_bakedAoValue), 1);
}

#define RENDER_MODE 0

void main(void) {
    switch (RENDER_MODE) {
        case 0: {
            renderMode0();
            return;
        }
        case 1: {
            renderMode1();
            return;
        }
        case 2: {
            renderMode2();
            return;
        }
        case 3: {
            renderMode3();
            return;
        }
    }
}