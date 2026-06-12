package me.zombii.beryllium.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.common.ModInit;

public class BerylliumCommon implements ModInit {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String NAMESPACE = "beryllium";

    @Override
    public void onInit() {
        // create or load instance and save to create dir and file
        BerylliumConfig.getOrLoad().save();
    }

}
