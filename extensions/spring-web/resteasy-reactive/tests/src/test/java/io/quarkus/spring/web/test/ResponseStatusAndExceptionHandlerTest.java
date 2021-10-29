package io.quarkus.spring.web.test;

import static io.restassured.RestAssured.when;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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

public class ResponseStatusAndExceptionHandlerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ExceptionController.class, RestExceptionHandler.class));

    @Test
    public void testRestControllerAdvice() {
        when().get("/exception").then().statusCode(400);
    }

    @Test
    public void testResponseStatusOnException() {
        when().get("/exception2").then().statusCode(202);
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
    }

    @RestControllerAdvice
    public static class RestExceptionHandler {

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<Object> handleException(Exception ex) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @ResponseStatus(HttpStatus.ACCEPTED)
    public static class MyException extends RuntimeException {

    }
}
