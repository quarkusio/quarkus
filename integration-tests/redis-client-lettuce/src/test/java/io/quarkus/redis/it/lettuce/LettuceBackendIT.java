package io.quarkus.redis.it.lettuce;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
class LettuceBackendIT extends LettuceBackendTest {

    @Override
    String getKey(String k) {
        return "native-" + k;
    }

    // Native only: in JVM mode the reactive API fails with a NoClassDefFoundError, not with the substituted factory
    @Test
    @DisabledOnIntegrationTest(forArtifactTypes = { DisabledOnIntegrationTest.ArtifactType.JAR,
            DisabledOnIntegrationTest.ArtifactType.CONTAINER })
    public void reactiveApiIsUnavailable() {
        RestAssured.given()
                .when()
                .get("/lettuce/connection/reactive-api")
                .then()
                .statusCode(200)
                .body(CoreMatchers.containsString("The Lettuce reactive API requires Project Reactor"));
    }
}
