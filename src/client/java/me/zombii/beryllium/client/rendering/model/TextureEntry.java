package me.zombii.beryllium.client.rendering.model;

import finalforeach.cosmicreach.util.Identifier;
import me.zombii.beryllium.client.BerylliumClient;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class TextureEntry {
    private final String name;
    private Identifier albedoTexturePath = BerylliumClient.MISSING_TEXTURE_PATH;
    private Identifier emissiveTexturePath = BerylliumClient.MISSING_TEXTURE_EMISSIVE_PATH;

    private Identifier normalMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_NORMAL_MAP_PATH;
    private Identifier roughnessMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_ROUGHNESS_MAP_PATH;
    private Identifier metalnessMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_METALNESS_MAP_PATH;
    private Identifier aoMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_AO_MAP_PATH;
    private Identifier depthMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_DEPTH_MAP_PATH;

    public TextureEntry(@NonNull String name) {
        this.name = Objects.requireNonNull(name, "Texture name must not be null!");
    }

    public TextureEntry setAlbedoTexturePath(Identifier albedoTexturePath) {
        if (albedoTexturePath == null) {
            this.albedoTexturePath = BerylliumClient.MISSING_TEXTURE_PATH;
            return this;
        }

        this.albedoTexturePath = albedoTexturePath;
        if (this.emissiveTexturePath == BerylliumClient.MISSING_TEXTURE_EMISSIVE_PATH) {
            this.emissiveTexturePath = BerylliumClient.DEFAULT_TEXTURE_EMISSIVE_PATH;
        }
        return this;
    }

    public TextureEntry setEmissiveTexturePath(Identifier emissiveTexturePath) {
        if (emissiveTexturePath == null) {
            this.emissiveTexturePath = BerylliumClient.DEFAULT_TEXTURE_EMISSIVE_PATH;
            return this;
        }

        this.emissiveTexturePath = emissiveTexturePath;
        return this;
    }

    public TextureEntry setAoMapTexturePath(Identifier aoMapTexturePath) {
        if (aoMapTexturePath == null) {
            this.aoMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_AO_MAP_PATH;
            return this;
        }
        this.aoMapTexturePath = aoMapTexturePath;
        return this;
    }

    public TextureEntry setDepthMapTexturePath(Identifier depthMapTexturePath) {
        if (depthMapTexturePath == null) {
            this.depthMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_DEPTH_MAP_PATH;
            return this;
        }
        this.depthMapTexturePath = depthMapTexturePath;
        return this;
    }

    public TextureEntry setMetalnessMapTexturePath(Identifier metalnessMapTexturePath) {
        if (metalnessMapTexturePath == null) {
            this.metalnessMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_METALNESS_MAP_PATH;
            return this;
        }
        this.metalnessMapTexturePath = metalnessMapTexturePath;
        return this;
    }

    public TextureEntry setNormalMapTexturePath(Identifier normalMapTexturePath) {
        if (normalMapTexturePath == null) {
            this.normalMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_NORMAL_MAP_PATH;
            return this;
        }
        this.normalMapTexturePath = normalMapTexturePath;
        return this;
    }

    public TextureEntry setRoughnessMapTexturePath(Identifier roughnessMapTexturePath) {
        if (roughnessMapTexturePath == null) {
            this.roughnessMapTexturePath = BerylliumClient.DEFAULT_TEXTURE_ROUGHNESS_MAP_PATH;
            return this;
        }
        this.roughnessMapTexturePath = roughnessMapTexturePath;
        return this;
    }

    @NonNull
    public Identifier getAlbedoTexturePath() {
        return albedoTexturePath;
    }

    @NonNull
    public Identifier getEmissiveTexturePath() {
        return emissiveTexturePath;
    }

    @NonNull
    public Identifier getAoMapTexturePath() {
        return aoMapTexturePath;
    }

    @NonNull
    public Identifier getDepthMapTexturePath() {
        return depthMapTexturePath;
    }

    @NonNull
    public Identifier getMetalnessMapTexturePath() {
        return metalnessMapTexturePath;
    }

    @NonNull
    public Identifier getNormalMapTexturePath() {
        return normalMapTexturePath;
    }

    @NonNull
    public Identifier getRoughnessMapTexturePath() {
        return roughnessMapTexturePath;
    }

    @NonNull
    public String getAtlasMaterialID() {
        return aoMapTexturePath.getNamespace() + "_" +
                depthMapTexturePath.getNamespace() + "_" +
                metalnessMapTexturePath.getNamespace() + "_" +
                roughnessMapTexturePath.getNamespace() + ":" +
                aoMapTexturePath.getName() + "_" +
                depthMapTexturePath.getName() + "_" +
                metalnessMapTexturePath.getName() + "_" +
                roughnessMapTexturePath.getName();
    }

    public String getName() {
        return name;
    }
}