package io.quarkus.it.spring.web;

import org.springframework.http.MediaType;

public class HandledResponseEntityException extends RuntimeException {

    private final MediaType contentType;

    public HandledResponseEntityException(String message) {
        this(message, null);
    }

    public HandledResponseEntityException(String message, MediaType contentType) {
        super(message);
        this.contentType = contentType;
    }

    public MediaType getContentType() {
        return contentType;
    }
}
