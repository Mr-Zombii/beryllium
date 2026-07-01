package me.zombii.beryllium.client.exceptions;

public class MissingJsonObjectException extends JsonException {

    public MissingJsonObjectException(String jsonFilePath, String fieldType, String fieldName, Throwable cause) {
        super(jsonFilePath, "expected a json " + fieldType + " field '"+ fieldName +"' but it was missing or null", cause);
    }

    public MissingJsonObjectException(String jsonFilePath, String objectName) {
        super(jsonFilePath, "expected a json object '"+ objectName +"' but it was missing or null");
    }
}
