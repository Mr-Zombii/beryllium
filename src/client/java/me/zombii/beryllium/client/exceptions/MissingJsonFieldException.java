package me.zombii.beryllium.client.exceptions;

public class MissingJsonFieldException extends JsonException {

    public MissingJsonFieldException(String jsonFilePath, String fieldType, String fieldName, Throwable cause) {
        super(jsonFilePath, "expected a json " + fieldType + " field '"+ fieldName +"' but it was missing or null", cause);
    }

    public MissingJsonFieldException(String jsonFilePath, String fieldType, String fieldName) {
        super(jsonFilePath, "expected a json '" + fieldType + "' field '"+ fieldName +"' but it was missing or null");
    }
}
