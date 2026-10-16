package io.quarkus.it.hibernate.validator.config;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Reaching any of these endpoints proves the application started successfully with a mix of
 * unconstrained, root-constrained, and deeply nested-constrained config mappings.
 */
@QuarkusTest
public class ConfigMappingValidationTest {

    @Test
    public void notValidated() {
        RestAssured.given()
                .when().get("/config-mapping-validation/not-validated")
                .then()
                .statusCode(200)
                .body(is("not-validated-value"));
    }

    @Test
    public void rootValidated() {
        RestAssured.given()
                .when().get("/config-mapping-validation/root-validated")
                .then()
                .statusCode(200)
                .body(is("root-validated-value"));
    }

    @Test
    public void nestedValidated() {
        RestAssured.given()
                .when().get("/config-mapping-validation/nested-validated")
                .then()
                .statusCode(200)
                .body(is("nested-level2-value"));
    }
}
