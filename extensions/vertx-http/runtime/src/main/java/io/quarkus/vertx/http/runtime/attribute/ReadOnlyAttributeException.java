package io.quarkus.vertx.http.runtime.attribute;

/**
 * An exception that is thrown when an attribute is read only
 *
 */
public class ReadOnlyAttributeException extends Exception {

    public ReadOnlyAttributeException() {
    }

    public ReadOnlyAttributeException(final String attributeName, final String newValue) {
        super("Could not set " + attributeName + " to " + newValue);
    }

}
