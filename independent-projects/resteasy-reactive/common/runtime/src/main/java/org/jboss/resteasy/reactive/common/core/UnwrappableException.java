package org.jboss.resteasy.reactive.common.core;

import jakarta.ws.rs.ProcessingException;

public class UnwrappableException extends ProcessingException {
    public UnwrappableException(Throwable cause) {
        super(cause);
    }
}
