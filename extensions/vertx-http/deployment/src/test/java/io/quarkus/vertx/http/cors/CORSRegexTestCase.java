package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class CORSRegexTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BeanRegisteringRoute.class)
                    .addAsResource("conf/cors-regex.properties", "application.properties"));

    @Test
    public void corsRegexValidOriginTest() {
        given().header("Origin", "https://asdf.domain.com")
                .when()
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "https://asdf.domain.com");
    }

    @Test
    public void corsRegexInvalidOriginTest() {
        given().header("Origin", "https://asdfdomain.com")
                .when()
                .get("/test").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue());
    }
}
