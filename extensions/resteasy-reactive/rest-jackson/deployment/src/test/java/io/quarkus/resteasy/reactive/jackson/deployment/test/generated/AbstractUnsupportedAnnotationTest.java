package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

public abstract class AbstractUnsupportedAnnotationTest {

    // --- @JsonAutoDetect ---

    @Test
    public void testAutoDetectFieldVisibility() {
        // With fieldVisibility=ANY and getterVisibility=NONE, fields are serialized directly
        RestAssured.get("/unsupported/auto-detect")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .body("visibleField", Matchers.is("hello"))
                .body("count", Matchers.is(42));
    }
}
