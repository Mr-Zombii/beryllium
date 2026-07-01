package me.zombii.beryllium.client.exceptions;

public class InvalidJsonArraySizeException extends JsonException {

    public InvalidJsonArraySizeException(String jsonFilePath, String name, int expectedSize, int actualSize, Throwable cause) {
        super(jsonFilePath, "expected a json array of size '" + expectedSize + "' but found array of size '" + actualSize + "' for field '" + name + "'", cause);
    }

    public InvalidJsonArraySizeException(String jsonFilePath, String name, int expectedSize, int actualSize) {
        super(jsonFilePath, "expected a json array of size '" + expectedSize + "' but found array of size '" + actualSize + "' for field '" + name + "'");
    }
}
