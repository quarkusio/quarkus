package org.jboss.resteasy.reactive.server.multipart;

import jakarta.ws.rs.BadRequestException;

/**
 * Exception thrown when some part of the multipart input cannot be read using the appropriate
 * {@link jakarta.ws.rs.ext.MessageBodyReader}.
 * This exception is useful to application because it can be by an {@link jakarta.ws.rs.ext.ExceptionMapper} in order to
 * customize
 * the input error handling.
 */
public class MultipartPartReadingException extends BadRequestException {

    public MultipartPartReadingException(Throwable cause) {
        super(cause);
    }
}
