package io.quarkus.it.jpa.preferredhibernatetypesoverride;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OverriddenPreferredTypesTest {

    @Test
    void shouldSaveEntityWithOverriddenTypes() {
        given().when().get("/jpa-test/overridden-preferred-types/test-successful-persistence").then()
                .body(is("OK"))
                .statusCode(200);
    }

    @Test
    void shouldOverrideTypes() {
        given().when().get("/jpa-test/overridden-preferred-types/test-successful-override").then()
                .body(is("OK"))
                .statusCode(200);
    }
}
