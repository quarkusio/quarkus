package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CORSRegexWildcardTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanRegisteringRoute.class)
                    .addAsResource("conf/cors-regex-wildcard.properties", "application.properties"));

    @Test
    public void corsRegexValidOriginTest() {
        given().header("Origin", "https://asdf.domain.com")
                .when()
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "https://asdf.domain.com")
                .header("Access-Control-Allow-Credentials", "false");
    }
}
