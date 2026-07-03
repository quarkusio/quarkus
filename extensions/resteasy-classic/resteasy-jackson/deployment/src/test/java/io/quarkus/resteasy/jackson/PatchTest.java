package io.quarkus.resteasy.jackson;

import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class PatchTest {

    @RegisterExtension
    static QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PatchResource.class, PatchMessage.class));

    @Test
    public void testMergePatchWithJacksonPreservesOmittedFields() {
        RestAssured.given()
                .contentType("application/merge-patch+json")
                .body("{\"message\":\"Patched\"}")
                .patch("/patch")
                .then()
                .statusCode(200)
                .body("message", equalTo("Patched"))
                .body("author", equalTo("Alice"));
    }
}
