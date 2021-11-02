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
import org.springframework.web.bind.annotation.RequestMapping;
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
    public void testRootResource() {
        when().get("/exception").then().statusCode(400);
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
    }

    @RestControllerAdvice
    public static class RestExceptionHandler {

        @ExceptionHandler(RuntimeException.class)
        public ResponseEntity<Object> handleException(Exception ex) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
