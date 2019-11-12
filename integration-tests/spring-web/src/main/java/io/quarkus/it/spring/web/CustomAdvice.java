package io.quarkus.it.spring.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomAdvice {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RuntimeException.class)
    public void handleRuntimeException() {

    }

    @ExceptionHandler(UnannotatedException.class)
    public void unannotatedException() {

    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Error> handleIllegalStateException(IllegalStateException e,
            HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new Error(request.getRequestURI() + ":" + e.getMessage()));
    }

    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(IllegalArgumentException.class)
    public Error handleGreetingIllegalArgumentException(IllegalArgumentException e) {
        return new Error(e.getMessage());
    }
}
