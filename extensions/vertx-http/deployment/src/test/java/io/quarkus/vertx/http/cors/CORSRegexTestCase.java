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
                .header("Access-Control-Allow-Origin", "https://asdf.domain.com")
                .header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    public void corsRegexValidOrigin2Test() {
        given().header("Origin", "https://abc-123.app.mydomain.com")
                .when()
                .get("/test").then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "https://abc-123.app.mydomain.com")
                .header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    public void corsRegexInvalidOriginTest() {
        given().header("Origin", "https://asdfdomain.com")
                .when()
                .get("/test").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue())
                .header("Access-Control-Allow-Credentials", nullValue());
    }

    @Test
    public void corsRegexInvalidOrigin2Test() {
        given().header("Origin", "https://abc-123app.mydomain.com")
                .when()
                .get("/test").then()
                .statusCode(403)
                .header("Access-Control-Allow-Origin", nullValue())
                .header("Access-Control-Allow-Credentials", nullValue());
    }
}
