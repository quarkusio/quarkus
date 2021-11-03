package io.quarkus.it.main;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class ResourcesITCase extends ResourcesTestCase {

    @Test
    public void excludedNative() {
        RestAssured.when()
                .get("/resources/test-resources/file.adoc")
                .then()
                .statusCode(404);

        RestAssured.when()
                .get("/resources/test-resources/excluded/unwanted.txt")
                .then()
                .statusCode(404);

        RestAssured.when()
                .get("/resources/META-INF/quarkus-native-resources.txt")
                .then()
                .statusCode(404);
    }

}
