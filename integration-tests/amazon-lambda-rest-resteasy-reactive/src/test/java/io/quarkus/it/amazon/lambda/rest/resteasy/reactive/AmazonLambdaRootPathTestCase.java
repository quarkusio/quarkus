package io.quarkus.it.amazon.lambda.rest.resteasy.reactive;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;

/**
 * With a root path, REST Assured must target the mock event server with the same base path it would use
 * for the HTTP server, so tests written against the resource paths keep working.
 */
@QuarkusTest
@TestProfile(RootPathProfile.class)
public class AmazonLambdaRootPathTestCase {

    @Test
    public void testResourcePathsAreRelativeToTheRootPath() {
        assertEquals("svc", RestAssured.basePath);
        given()
                .when()
                .get("/hello/context")
                .then()
                .statusCode(204);
    }

    @Test
    public void testAbsolutePath() {
        String basePath = RestAssured.basePath;
        try {
            RestAssured.basePath = "/";
            given()
                    .when()
                    .get("/svc/hello/context")
                    .then()
                    .statusCode(204);
            given()
                    .when()
                    .get("/hello/context")
                    .then()
                    .statusCode(404);
        } finally {
            RestAssured.basePath = basePath;
        }
    }
}
