package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class CORSHandlerTestWildcardOriginCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BeanRegisteringRoute.class)
                    .addAsResource("conf/cors-config-wildcard-origins.properties", "application.properties"));

    @Test
    @DisplayName("Returns true 'Access-Control-Allow-Credentials' header on matching origin")
    void corsMatchingOrigin() {
        String origin = "http://custom.origin.quarkus";
        String methods = "GET";
        String headers = "X-Custom";
        given().header("Origin", origin).header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers).when().options("/test").then().statusCode(200)
                .header("Access-Control-Allow-Origin", origin).header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET,OPTIONS,POST");
    }

    @Test
    @DisplayName("Returns false 'Access-Control-Allow-Credentials' header on matching origin")
    void corsNotMatchingOrigin() {
        String origin = "http://non.matching.origin.quarkus";
        String methods = "POST";
        String headers = "X-Custom";
        given().header("Origin", origin).header("Access-Control-Request-Method", methods)
                .header("Access-Control-Request-Headers", headers).when().options("/test").then().statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void corsSameOriginRequest() {
        String origin = "http://localhost:8081";
        given().header("Origin", origin).get("/test").then().statusCode(200).header("Access-Control-Allow-Origin",
                origin);
    }

    @Test
    void corsInvalidSameOriginRequest1() {
        String origin = "http";
        given().header("Origin", origin).get("/test").then().statusCode(403).header("Access-Control-Allow-Origin",
                nullValue());
    }

    @Test
    void corsInvalidSameOriginRequest2() {
        String origin = "http://local";
        given().header("Origin", origin).get("/test").then().statusCode(403).header("Access-Control-Allow-Origin",
                nullValue());
    }

    @Test
    void corsInvalidSameOriginRequest3() {
        String origin = "http://localhost";
        given().header("Origin", origin).get("/test").then().statusCode(403).header("Access-Control-Allow-Origin",
                nullValue());
    }

    @Test
    void corsInvalidSameOriginRequest4() {
        String origin = "http://localhost:9999";
        given().header("Origin", origin).get("/test").then().statusCode(403).header("Access-Control-Allow-Origin",
                nullValue());
    }

    @Test
    void corsInvalidSameOriginRequest5() {
        String origin = "https://localhost:8483";
        given().header("Origin", origin).get("/test").then().statusCode(403).header("Access-Control-Allow-Origin",
                nullValue());
    }

}
