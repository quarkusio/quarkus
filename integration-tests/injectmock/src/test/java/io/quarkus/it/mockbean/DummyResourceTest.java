package io.quarkus.it.mockbean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
class DummyResourceTest {

    @InjectMock
    @Named("first")
    DummyService firstDummyService;

    @BeforeEach
    void setUp() {
        when(firstDummyService.returnDummyValue()).thenReturn("1");
    }

    @Test
    void testDummy() {
        given()
                .when().get("/dummy")
                .then()
                .statusCode(200)
                .body(is("1/second"));
    }

    @Test
    void testDummyAgain() {
        given()
                .when().get("/dummy")
                .then()
                .statusCode(200)
                .body(is("1/second"));
    }

    @Test
    void testWithOverrideInMethod() {
        when(firstDummyService.returnDummyValue()).thenReturn("fst");
        given()
                .when().get("/dummy")
                .then()
                .statusCode(200)
                .body(is("fst/second"));
    }
}
