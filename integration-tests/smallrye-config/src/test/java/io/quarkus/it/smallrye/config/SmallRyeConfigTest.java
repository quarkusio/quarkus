package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SmallRyeConfigTest {
    @Test
    void mpConfigProperties() {
        given()
                .get("/config/{name}", "mp.config.server.name")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("localhost"));

        given()
                .get("/config/{name}", "mp.config.server.port")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("9090"));
    }

    @Test
    void dotenv() {
        given()
                .get("/config/{name}", "dotenv.server.name")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("localhost"));

        given()
                .get("/config/{name}", "dotenv.server.port")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("9090"));
    }

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
    void externalConfig() {
        given()
                .get("/config/{name}", "config.dir.property")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"));

        given()
                .get("/config/{name}", "config.dir.property.yaml")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"));
    }

    @Test
    void userConfig() {
        given()
                .get("/config/{name}", "user.config.prop")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"))
                .body("sourceName", equalTo("UserConfigSource"));

        given()
                .get("/config/{name}", "user.config.provider.prop")
                .then()
                .statusCode(OK.getStatusCode())
                .body("value", equalTo("1234"))
                .body("sourceName", equalTo("UserConfigSource"));
    }
}
