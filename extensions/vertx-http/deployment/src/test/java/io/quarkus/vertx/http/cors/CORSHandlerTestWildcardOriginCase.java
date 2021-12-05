package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class CORSHandlerTestWildcardOriginCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanRegisteringRoute.class)
                    .addAsResource("conf/cors-config-wildcard-origins.properties", "application.properties"));

    @Test
    @DisplayName("Returns true 'Access-Control-Allow-Credentials' header on matching origin")
    void corsMatchingOrigin() {
        String origin = "http://custom.origin.quarkus";
        String methods = "GET,POST";
        String headers = "X-Custom";
        given().header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    @DisplayName("Returns false 'Access-Control-Allow-Credentials' header on matching origin")
    void corsNotMatchingOrigin() {
        String origin = "http://non.matching.origin.quarkus";
        String methods = "GET,POST";
        String headers = "X-Custom";
        given().header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Credentials", "false");
    }

    @Test
    @DisplayName("Returns false 'Access-Control-Allow-Credentials' header on matching origin '*'")
    void corsMatchingOriginWithWildcard() {
        String origin = "*";
        String methods = "GET,POST";
        String headers = "X-Custom";
        given().header("Origin", origin)
                .header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers)
                .when()
                .options("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Credentials", "false");
    }
}
