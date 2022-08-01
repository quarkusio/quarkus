package org.jboss.resteasy.reactive.common.core;

import javax.ws.rs.ProcessingException;

public class UnwrappableException extends ProcessingException {
    public UnwrappableException(Throwable cause) {
        super(cause);
    }
}