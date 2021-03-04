package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ServerResourceTest {
    @Test
    void mapping() {
        given()
                .get("/server")
                .then()
                .statusCode(OK.getStatusCode())
                .body("host", equalTo("localhost"))
                .body("port", equalTo(8080))
                .body("threads", equalTo(200))
                .body("form.form.loginPage", equalTo("login.html"))
                .body("form.form.errorPage", equalTo("error.html"))
                .body("form.form.landingPage", equalTo("index.html"))
                .body("ssl.port", equalTo(8443))
                .body("ssl.certificate", equalTo("certificate"))
                .body("cors.methods[0]", equalTo("GET"))
                .body("cors.methods[1]", equalTo("POST"))
                .body("cors.origins[0].host", equalTo("some-server"))
                .body("cors.origins[0].port", equalTo(9000))
                .body("cors.origins[1].host", equalTo("another-server"))
                .body("cors.origins[1].port", equalTo(8000))
                .body("log.enabled", equalTo(false))
                .body("log.suffix", equalTo(".log"))
                .body("log.rotate", equalTo(true))
                .body("log.pattern", equalTo("COMMON"))
                .body("log.period", equalTo("P1D"));
    }

    @Test
    void properties() {
        given()
                .get("/server/properties")
                .then()
                .statusCode(OK.getStatusCode())
                .body("host", equalTo("localhost"))
                .body("port", equalTo(8080));
    }
}
