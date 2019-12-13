package io.quarkus.it.spring.web;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class UnhandledAnnotatedException extends Exception {
    public UnhandledAnnotatedException(String message) {
        super(message);
    }
}
