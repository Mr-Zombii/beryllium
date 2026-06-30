package me.zombii.beryllium.client.exceptions;

import me.zombii.beryllium.client.model.BerylliumModel;

public class ModelException extends RuntimeException {

    public ModelException(BerylliumModel model, String message) {
        super("Exception for model \"" + model.getName() + "\": " + message);
    }

    public ModelException(BerylliumModel model, String message, Throwable cause) {
      super("Exception for model \"" + model.getName() + "\": " + message, cause);
    }

    public ModelException(BerylliumModel model, Throwable cause) {
      super("Exception for model \"" + model.getName() + "\": " + (cause == null ? "" : cause.toString()), cause);
    }

    public ModelException(String modelName, String message) {
        super("Exception for model \"" + modelName + "\": " + message);
    }

    public ModelException(String modelName, String message, Throwable cause) {
        super("Exception for model \"" + modelName + "\": " + message, cause);
    }

    public ModelException(String modelName, Throwable cause) {
        super("Exception for model \"" + modelName + "\": " + (cause == null ? "" : cause.toString()), cause);
    }
}
