package io.quarkus.spring.web.resteasy.classic.test;

import static io.restassured.RestAssured.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.quarkus.test.QuarkusUnitTest;

public class ResponseStatusAndExceptionHandlerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExceptionController.class, RestExceptionHandler.class));

    @Test
    public void testRootResource() {
        when().get("/exception").then().statusCode(400);
    }

    @Test
    public void testIllegalResource() {
        when().get("/exception/illegalArgument").then().statusCode(500);
    }

    @RestController
    @RequestMapping("/exception")
    public static class ExceptionController {

        @GetMapping
        @ResponseStatus(HttpStatus.OK)
        public String throwException() {
            RuntimeException exception = new RuntimeException();
            exception.setStackTrace(new StackTraceElement[0]);
            throw exception;
        }

        @GetMapping("/illegalArgument")
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        public String throwIllegalException() {
            IllegalArgumentException exception = new IllegalArgumentException();
            exception.setStackTrace(new StackTraceElement[0]);
            throw exception;
        }
    }

    @RestControllerAdvice
    public static class RestExceptionHandler {

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<Object> handleException(Exception ex) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Object> handleException(Exception ex, HttpServletRequest request, HttpServletResponse response) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            request.setAttribute("javax.servlet.error.status_code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
