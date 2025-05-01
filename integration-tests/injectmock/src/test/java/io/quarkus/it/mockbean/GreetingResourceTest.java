package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class GreetingResourceTest {

    @InjectMock
    MessageService messageService;

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

    @Test
    public void testMocksNotSet() {
        // when mocks are not configured, they return the Mockito default response
        Assertions.assertNull(messageService.getMessage());
        Assertions.assertNull(suffixService.getSuffix());

        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("NULLNULL"));
    }
}
