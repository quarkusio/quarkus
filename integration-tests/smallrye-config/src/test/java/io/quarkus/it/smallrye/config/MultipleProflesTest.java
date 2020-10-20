package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class MultipleProflesTest {
    @Test
    void properties() {
        given()
                .get("/config/{name}", "profile.property.shared")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("main"));

        given()
                .get("/config/{name}", "profile.property.common")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("common"));

        given()
                .get("/config/{name}", "profile.property.main")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("main"));

        given()
                .get("/config/{name}", "no.profile")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("any"));
    }

    @Test
    void activeProfiles() {
        given().contentType(ContentType.TEXT).get("/profiles")
                .then()
                .statusCode(OK.getStatusCode())
                .body(is((ProfileManager.getActiveProfiles()
                        .stream()
                        .map(profile -> {
                            switch (profile) {
                                case "common":
                                    return 1;
                                case "test":
                                    return 2;
                                case "prod":
                                    return 4;
                                default:
                                    return 0;
                            }
                        })
                        .reduce((integer, integer2) -> integer | integer2)
                        .orElse(0) | 8) + ""));
    }
}
