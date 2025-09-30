package io.quarkus.spring.web.resteasy.reactive.test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class ResponseStatusAndExceptionHandlerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ExceptionController.class, RestExceptionHandler.class));

    @Test
    public void testRestControllerAdvice() {
        when().get("/exception").then().statusCode(400);
    }

    @Test
    public void testResponseStatusOnException() {
        when().get("/exception2").then().statusCode(202);
    }

    @Test
    public void testExceptionHandlingWithHttpRequest() {
        when().get("/exception3").then().statusCode(400)
                .body(containsString("Request GET /exception3 failed")).header("X-Error-Reason", is("IllegalArgument"));
    }

    @RestController
    public static class ExceptionController {

        public static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

        @GetMapping("/exception")
        @ResponseStatus(HttpStatus.OK)
        public String throwRuntimeException() {
            RuntimeException runtimeException = new RuntimeException();
            runtimeException.setStackTrace(EMPTY_STACK_TRACE);
            throw runtimeException;
        }

        @GetMapping("/exception2")
        public String throwMyException() {
            MyException myException = new MyException();
            myException.setStackTrace(EMPTY_STACK_TRACE);
            throw myException;
        }

        @GetMapping("/exception3")
        public String throwIllegalArgumentException() {
            IllegalArgumentException illegalArgumentException = new IllegalArgumentException();
            illegalArgumentException.setStackTrace(EMPTY_STACK_TRACE);
            throw illegalArgumentException;
        }
    }

    @RestControllerAdvice
    public static class RestExceptionHandler {

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<Object> handleException(Exception ex) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Object> handleBadRequestException(Exception ex, HttpServerRequest request,
                HttpServerResponse response) {
            String body = String.format(
                    "Request %s %s failed",
                    request.method().name(), request.uri());
            response.putHeader("X-Error-Reason", "IllegalArgument");
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    public static class MyException extends RuntimeException {

    }
}
