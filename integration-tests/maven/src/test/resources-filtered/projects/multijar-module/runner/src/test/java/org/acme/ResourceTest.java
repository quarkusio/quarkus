package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.UUID;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import org.acme.testlib.TestMessage;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ResourceTest {

    @Inject
    Service service;

    @Inject
    Printer printer;

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("bonjour acme other mock-service"));
    }

    @Test
    public void testPrintMessage() {
        // this is testing the TestMessage class can be loaded
        printer.println(new TestMessage("test message"));
    }
}
