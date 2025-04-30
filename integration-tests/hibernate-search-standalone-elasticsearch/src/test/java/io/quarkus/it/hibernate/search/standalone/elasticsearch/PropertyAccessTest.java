package io.quarkus.it.hibernate.search.standalone.elasticsearch;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PropertyAccessTest {

    @Test
    public void testPrivateFieldAccess() {
        when().get("/test/property-access/private-field").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testPublicFieldAccess() {
        when().get("/test/property-access/public-field").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testMethodAccess() {
        when().get("/test/property-access/method").then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    public void testRecordFieldAccess() {
        when().get("/test/property-access/record-field").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
