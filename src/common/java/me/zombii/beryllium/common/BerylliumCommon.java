package me.zombii.beryllium.common;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.puzzleshq.puzzleloader.loader.mod.entrypoint.common.ModInit;

public class BerylliumCommon implements ModInit {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String NAMESPACE = "beryllium";
    public static final Json JSON = new Json();
    public static final JsonReader READER = new JsonReader();
    public static final JsonValue.PrettyPrintSettings SETTINGS = new JsonValue.PrettyPrintSettings();

    @Override
    public void onInit() {
        // create or load instance and save to create dir and file
        BerylliumConfig.INSTANCE.save();
    }

}
