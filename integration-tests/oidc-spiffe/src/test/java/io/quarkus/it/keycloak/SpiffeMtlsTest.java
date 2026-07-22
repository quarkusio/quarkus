package io.quarkus.it.keycloak;

import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

// POC for SPIFFE X.509-SVID mTLS — will evolve with #55412 (TLS registry integration) and #55338 (SPIFFE cert verification)
@QuarkusTest
class SpiffeMtlsTest {

    @TestHTTPResource(value = "/", tls = true)
    URL httpsBaseUrl;

    @Test
    void webclientAuthenticated() {
        var result = RestAssured.given()
                .queryParam("port", httpsBaseUrl.getPort())
                .get("/spiffe/mtls/client/webclient/authenticated")
                .then().statusCode(200)
                .body("actualPrincipal", not(emptyString()))
                .body("expectedPrincipal", not(emptyString()))
                .extract().as(SpiffeMtlsResource.MtlsResult.class);
        assertEquals(result.expectedPrincipal(), result.actualPrincipal());
    }

    @Test
    void webclientUnauthorized() {
        RestAssured.given()
                .queryParam("port", httpsBaseUrl.getPort())
                .get("/spiffe/mtls/client/webclient/unauthorized")
                .then().statusCode(200)
                .body(equalTo("401"));
    }

    @Test
    void restAuthenticated() {
        var result = RestAssured.given()
                .queryParam("port", httpsBaseUrl.getPort())
                .get("/spiffe/mtls/client/rest/authenticated")
                .then().statusCode(200)
                .body("actualPrincipal", not(emptyString()))
                .body("expectedPrincipal", not(emptyString()))
                .extract().as(SpiffeMtlsResource.MtlsResult.class);
        assertEquals(result.expectedPrincipal(), result.actualPrincipal());
    }

    @Test
    void restUnauthorized() {
        RestAssured.given()
                .queryParam("port", httpsBaseUrl.getPort())
                .get("/spiffe/mtls/client/rest/unauthorized")
                .then().statusCode(200)
                .body(equalTo("401"));
    }
}
