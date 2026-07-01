package me.zombii.beryllium.client.exceptions;

public class JsonException extends RuntimeException {

    public JsonException(String jsonFilePath, String message) {
        super("Exception for json file \"" + jsonFilePath + "\" " + message);
    }

    public JsonException(String jsonFilePath, String message, Throwable cause) {
        super("Exception for json file \"" + jsonFilePath + "\" " + message, cause);
    }

    public JsonException(String jsonFilePath, Throwable cause) {
        super("Exception for json file \"" + jsonFilePath + "\"", cause);
    }
}
