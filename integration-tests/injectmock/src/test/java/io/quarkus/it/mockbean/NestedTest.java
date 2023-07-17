package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class NestedTest {

    @InjectMock
    MessageService messageService;

    @Nested
    public class ActualTest {

        @InjectMock
        SuffixService suffixService;

        @Test
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
        public void testGreetAgain() {
            Mockito.when(messageService.getMessage()).thenReturn("yolo");
            Mockito.when(suffixService.getSuffix()).thenReturn("!!!");

            given()
                    .when().get("/greeting")
                    .then()
                    .statusCode(200)
                    .body(is("YOLO!!!"));
        }
    }
}
