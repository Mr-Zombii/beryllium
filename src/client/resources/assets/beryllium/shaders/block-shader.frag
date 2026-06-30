#version 420 core

layout (binding = 2) uniform sampler2D u_albedoAtlas;
#ifdef HAS_EMISSIVE_ATLAS
layout (binding = 3) uniform sampler2D u_emissiveAtlas;
#endif
#ifdef HAS_NORMAL_ATLAS
layout (binding = 4) uniform sampler2D u_normalAtlas;
#endif
#ifdef HAS_MATERIAL_ATLAS
layout (binding = 5) uniform sampler2D u_materialAtlas;
#endif

uniform vec3 u_sunDirection = vec3(0, 1, 0);
uniform vec3 u_ambientWorldColor = vec3(1, 1, 1);
uniform vec3 u_ambientSkyColor = vec3(1, 1, 1);
uniform vec3 u_cameraPos;
uniform float u_fogDensity;
uniform float u_renderDistanceInChunks;
uniform vec3 u_playerLightColor;

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

vec3 getPlayerLight(vec3 currentColor, vec3 worldPos, vec3 cameraPos) {
    float dist = distance(worldPos, cameraPos);

    // minimum amount of light surrounding the camera with a small radius
    float radius = 8.0;
    float minLightAmount = 0.25;

    float lightFactor = clamp(1.0 - (dist / radius), 0.0, 1.0);
    // Smooth falloff
    lightFactor = lightFactor * lightFactor;

    vec3 lightDir = vec3(1.0);
    float l = length(u_playerLightColor);
    if (l > 0.0) {
        lightDir = u_playerLightColor / l;
    }

    float brightness = max(u_playerLightColor.r, max(u_playerLightColor.g, u_playerLightColor.b));
    vec3 lightColor = mix(vec3(1.0), lightDir, pow(brightness, 0.1));

    vec3 playerLight = lightColor * lightFactor * minLightAmount;

    return max(currentColor, playerLight);
}

vec3 getFogColor(vec3 fogBaseColor, vec3 blocklight, float fogDensity, vec3 worldPos, vec3 cameraPosition)
{
    float worldDistance = length(worldPos - cameraPosition);

    float blocklightFactor = exp(-pow(worldDistance * fogDensity/2, 0.4));
    return mix(fogBaseColor, max(fogBaseColor, blocklight.rgb), blocklightFactor);
}

vec3 applyFog(vec3 fogBaseColor, vec3 inputColor, float fogDensity, vec3 worldPos, vec3 cameraPosition)
{
    if(fogDensity == 0)
    {
        return inputColor;
    }

    float fogDistance = length(worldPos.xz - cameraPosition.xz) + 0.5*length(worldPos.y - cameraPosition.y);
    float fadeOutDistance = length(worldPos.xz - cameraPosition.xz);
    float fadeOutMaxDistance = (u_renderDistanceInChunks - 1) * 16;
    float fadeOutFactor = clamp((fadeOutMaxDistance - fadeOutDistance)/256.0, 0.01, 1.0);
    fogDistance *= 1 / fadeOutFactor;

    vec3 worldDirection = normalize(worldPos - cameraPosition);

    // Higher areas are less affected by fog.
    float higherDot = 0;//dot(worldDirection, vec3(0, 1, 0));
    float higherDistanceFactor = (higherDot + 1) / 2;
    higherDistanceFactor = pow(higherDistanceFactor, 0.75) * 0.04 / fogDensity;
    fogDistance = (fogDistance * (1-clamp(higherDistanceFactor, 0, 1)));

    float fogSpread = 1;

    float fogFactor = 1 - exp(-pow(fogDistance * fogDensity, fogSpread));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    float noonFactor = abs(dot(u_sunDirection, vec3(0, 1, 0)));
    float fogDirectionFactor = fogFactor * (1+dot(u_sunDirection, worldDirection))/2.0;
    fogDirectionFactor = clamp(fogDirectionFactor + ((higherDot + 0.5) / 2.0), 0.0, 1.0);
    fogDirectionFactor = 1 - ((1 - noonFactor) * (1 - fogDirectionFactor));

    // Higher areas are less affected by fog.
    float higherFactor = max(dot(worldDirection, vec3(0, 1, 0)), 0);
    higherFactor = pow(higherFactor, 0.75);
    fogFactor = (fogFactor * (1-higherFactor*0.5));

    vec3 fogColor = fogBaseColor * fogDirectionFactor;
    return mix(inputColor.rgb, fogColor, fogFactor);
}

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

void main(void) {
    #if USE_NORMAL_AS_ALBEDO == 0
    vec4 albedoColor = tintColor(texture(u_albedoAtlas, v_albedoUV));
//    albedoColor = vec4(v_albedoFrameDuration, 0.0, 0.0, 1.0);
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
    lightTint = getPlayerLight(lightTint, v_worldPos, u_cameraPos);

    fragColor = vec4(albedoColor.rgb * lightTint, albedoColor.a);

    #ifdef HAS_EMISSIVE_ATLAS
    vec4 emissiveColor = tintColor(texture(u_emissiveAtlas, v_emissiveUV));
    fragColor.rgb = max(fragColor.rgb, emissiveColor.rgb * emissiveColor.a);
    #endif

    fragColor.rgb *= v_bakedAoValue;

    vec3 fogColor = u_ambientSkyColor;
    fogColor = getFogColor(fogColor, v_blockLightColor.rgb, u_fogDensity, v_worldPos, u_cameraPos);
    fragColor.rgb = applyFog(fogColor, fragColor.rgb, u_fogDensity, v_worldPos, u_cameraPos);

    fragColor.rgb = max(fragColor.rgb, albedoColor.rgb * u_ambientWorldColor);
}