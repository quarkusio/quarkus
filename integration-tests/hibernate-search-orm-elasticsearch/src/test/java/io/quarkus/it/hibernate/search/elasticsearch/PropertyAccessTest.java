package io.quarkus.it.hibernate.search.elasticsearch;

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
    public void testPrivateFieldAccessLazyInitialization() {
        when().get("/test/property-access/private-field-lazy-init").then()
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
    public void testTransientMethodAccess() {
        when().get("/test/property-access/transient-method").then()
                .statusCode(200)
                .body(is("OK"));
    }
}
