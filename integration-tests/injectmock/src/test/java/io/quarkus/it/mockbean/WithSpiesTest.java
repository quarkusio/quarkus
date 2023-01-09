package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.inject.Named;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;

@QuarkusTest
class WithSpiesTest {

    @InjectSpy
    CapitalizerService capitalizerService;

    @InjectSpy
    MessageService messageService;

    @InjectSpy
    SuffixService suffixService;

    @InjectSpy
    @Named("first")
    DummyService firstDummyService;

    @InjectSpy
    @Named("second")
    DummyService secondDummyService;

    @Test
    @DisplayName("Verify default Greeting values are returned from Spied objects")
    public void testGreet() {
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("HELLO"));
        Mockito.verify(capitalizerService, Mockito.times(1)).capitalize(Mockito.eq("hello"));
        Mockito.verify(messageService, Mockito.times(1)).getMessage();
        Mockito.verify(suffixService, Mockito.times(1)).getSuffix();
    }

    @Test
    @DisplayName("Verify default Dummy values are returned from Spied objects")
    public void testDummy() {
        given()
                .when().get("/dummy")
                .then()
                .statusCode(200)
                .body(is("first/second"));
        Mockito.verify(firstDummyService, Mockito.times(1)).returnDummyValue();
        Mockito.verify(secondDummyService, Mockito.times(1)).returnDummyValue();
    }

    @Test
    @DisplayName("Verify we can override default Greeting values are returned from Spied objects")
    public void testOverrideGreet() {
        Mockito.when(messageService.getMessage()).thenReturn("hi");
        Mockito.when(suffixService.getSuffix()).thenReturn("!");
        given()
                .when().get("/greeting")
                .then()
                .statusCode(200)
                .body(is("HI!"));
    }

    @Test
    @DisplayName("Verify can override default Dummy values are returned from Spied objects")
    public void testOverrideDummy() {
        Mockito.when(firstDummyService.returnDummyValue()).thenReturn("1");
        Mockito.when(secondDummyService.returnDummyValue()).thenReturn("2");
        given()
                .when().get("/dummy")
                .then()
                .statusCode(200)
                .body(is("1/2"));
    }
}
