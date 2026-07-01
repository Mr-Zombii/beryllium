package me.zombii.beryllium.client.exceptions;

public class InvalidJsonArrayTypeException extends JsonException {

    public InvalidJsonArrayTypeException(String jsonFilePath, String name, String expectedType, String actualDescription, Throwable cause) {
        super(jsonFilePath, "expected a json array element of type '" + expectedType + "' but found '" + actualDescription + "' for array '" + name + "'", cause);
    }
    public InvalidJsonArrayTypeException(String jsonFilePath, String name, String expectedType, String actualDescription) {
        super(jsonFilePath, "expected a json array element of type '" + expectedType + "' but found '" + actualDescription + "' for array '" + name + "'");
    }
}
