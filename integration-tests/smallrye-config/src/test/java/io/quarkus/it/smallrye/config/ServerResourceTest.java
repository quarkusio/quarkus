package io.quarkus.it.smallrye.config;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;

@QuarkusTest
class ServerResourceTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(CONTENT_TYPE, APPLICATION_JSON));
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                });
    }

    @Test
    void mapping() {
        given()
                .get("/server")
                .then()
                .statusCode(OK.getStatusCode())
                .body("name", equalTo("server"))
                .body("alias", equalTo("server"))
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

    @Test
    void invalid() {
        given().get("/server/validator/{prefix}", "cloud")
                .then()
                .statusCode(OK.getStatusCode())
                .body("errors", hasSize(7))
                .body("errors", hasItem("cloud.log.days must be less than or equal to 15"))
                .body("errors", hasItem("cloud.cors.origins[1].port must be greater than or equal to 8000"))
                .body("errors", hasItem("cloud.info.name size must be between 0 and 3"))
                .body("errors", hasItem("cloud.info.code must be less than or equal to 3"))
                .body("errors", hasItem("cloud.info.alias[0] size must be between 0 and 3"))
                .body("errors", hasItem("cloud.info.admins.root.username size must be between 0 and 3"))
                .body("errors", hasItem("cloud.proxy.timeout must be less than or equal to 10"));
    }
}
