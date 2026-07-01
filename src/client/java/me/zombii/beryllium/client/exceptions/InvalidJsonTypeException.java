package me.zombii.beryllium.client.exceptions;

public class InvalidJsonTypeException extends JsonException {

    public InvalidJsonTypeException(String jsonFilePath, String name, String expectedType, String actualDescription, Throwable cause) {
        super(jsonFilePath, "expected a json '" + expectedType + "' but found '" + actualDescription + "' for field '" + name + "'", cause);
    }

    public InvalidJsonTypeException(String jsonFilePath, String name, String expectedType, String actualDescription) {
        super(jsonFilePath, "expected a json '" + expectedType + "' but found '" + actualDescription + "' for field '" + name + "'");
    }
}
