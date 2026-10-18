package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ConfigLocationsTest {

    @Test
    void locations() {
        given()
                .get("/config/{name}", "config.properties")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"));

        given()
                .get("/config/{name}", "config.properties.common")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"));
    }

    @Test
    void fileSystemLocation() {
        given()
                .get("/config/{name}", "fs.key1")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("value1"));
    }

    @Test
    void applicationPropertiesProfile() {
        given()
                .get("/config/{name}", "profile.main.properties")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("main"));

        given()
                .get("/config/{name}", "profile.common.properties")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("common"));
    }

    @Test
    void applicationYamlProfile() {
        given()
                .get("/config/{name}", "profile.main.yaml")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("main"));

        given()
                .get("/config/{name}", "profile.common.yaml")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("common"));
    }
}
