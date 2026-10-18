package org.jboss.resteasy.reactive.common;

/**
 * Marker exception that indicates to RESTEasy Reactive that the target should be preserved.
 * This is very useful for providing good contextual error messages
 */
public class PreserveTargetException extends RuntimeException {

    public PreserveTargetException(Throwable cause) {
        super(cause);
    }
}
