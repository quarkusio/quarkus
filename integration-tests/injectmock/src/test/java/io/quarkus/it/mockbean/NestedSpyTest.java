package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

@QuarkusTest
public class NestedSpyTest {

    @InjectSpy
    MessageService messageService;

    @Nested
    public class ActualTest {

        @InjectSpy
        SuffixService suffixService;

        @Test
        @Order(1)
        public void testGreet() {
            Mockito.when(messageService.getMessage()).thenReturn("hi");
            Mockito.when(suffixService.getSuffix()).thenReturn("!");

            given()
                    .when().get("/greeting")
                    .then()
                    .statusCode(200)
                    .body(is("HI!"));
        }

        @Test
        @Order(2)
        public void testGreetAgain() {
            Mockito.when(messageService.getMessage()).thenReturn("yolo");
            Mockito.when(suffixService.getSuffix()).thenReturn("!!!");

            given()
                    .when().get("/greeting")
                    .then()
                    .statusCode(200)
                    .body(is("YOLO!!!"));
        }

        @Test
        @Order(3)
        public void testNoSpy() {
            given()
                    .when().get("/greeting")
                    .then()
                    .statusCode(200)
                    .body(is("HELLO"));
        }
    }
}
