package io.quarkus.it.spring.web;

import javax.servlet.http.HttpServletRequest;

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

    @ExceptionHandler(HandledUnannotatedException.class)
    public void unannotatedException() {

    }

    @ExceptionHandler(HandledResponseEntityException.class)
    public ResponseEntity<Error> handleResponseEntityException(HandledResponseEntityException e,
            HttpServletRequest request) {

        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity
                .status(HttpStatus.PAYMENT_REQUIRED)
                .header("custom-header", "custom-value");

        if (e.getContentType() != null) {
            bodyBuilder.contentType(e.getContentType());
        }

        return bodyBuilder.body(new Error(request.getRequestURI() + ":" + e.getMessage()));
    }

    @ResponseStatus(HttpStatus.EXPECTATION_FAILED)
    @ExceptionHandler(HandledPojoException.class)
    public Error handlePojoExcepton(HandledPojoException e) {
        return new Error(e.getMessage());
    }

    @ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
    @ExceptionHandler(HandledStringException.class)
    public String handleStringException(HandledStringException e) {
        return e.getMessage();
    }
}
