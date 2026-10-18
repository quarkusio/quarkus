package io.quarkus.arc;

import jakarta.enterprise.inject.InjectionException;

/**
 * Indicates that an inactive bean was injected (or dynamically looked up).
 */
public class InactiveBeanException extends InjectionException {
    public InactiveBeanException(String message) {
        super(message);
    }
}
