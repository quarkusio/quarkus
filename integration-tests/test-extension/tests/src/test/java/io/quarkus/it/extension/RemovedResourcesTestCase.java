package io.quarkus.it.extension;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RemovedResourcesTestCase {

    @Test
    public void removedResourceContextClassLoader() {
        given()
                .param("resource", "COMMON_NET_MESSAGES")
                .param("classLoaderKind", "CONTEXT_CLASS_LOADER")
                .when().get("/core/removed-resource")
                .then()
                .body(not(containsString("invalidAddress")));
    }
}
