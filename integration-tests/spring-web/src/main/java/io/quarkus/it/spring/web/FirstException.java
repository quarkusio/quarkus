package io.quarkus.it.spring.web;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class FirstException extends Exception {
    public FirstException(String message) {
        super(message);
    }
}
