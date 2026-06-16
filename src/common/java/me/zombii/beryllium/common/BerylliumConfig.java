package me.zombii.beryllium.common;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BerylliumConfig {

    public static final File CONFIG_LOCATION = new File(".beryllium/config.json");
    public static BerylliumConfig INSTANCE = getOrLoad();

    public boolean debugMode = true;
    public boolean enableEmissiveAtlas = false;
    public boolean enableNormalAtlas = false;
    public boolean enableMaterialAtlas = false;

    public BerylliumConfig() {}

    private static BerylliumConfig getOrLoad() {
        if (CONFIG_LOCATION.exists()) {
            try {
                BerylliumConfig config;
                FileReader fr = new FileReader(BerylliumConfig.CONFIG_LOCATION);
                config = BerylliumCommon.GSON.fromJson(fr, BerylliumConfig.class);
                fr.close();
                return config;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new BerylliumConfig();
    }

    public void save() {
        if (!CONFIG_LOCATION.exists()) {
            if (!CONFIG_LOCATION.getParentFile().exists()) {
                CONFIG_LOCATION.getParentFile().mkdirs();
            }
            try {
                CONFIG_LOCATION.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String json = BerylliumCommon.GSON.toJson(this);
        try {
            FileWriter fw = new FileWriter(CONFIG_LOCATION);
            fw.write(json);
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
