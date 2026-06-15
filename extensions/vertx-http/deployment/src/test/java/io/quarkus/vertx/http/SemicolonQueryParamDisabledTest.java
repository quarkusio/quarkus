package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

/**
 * Tests that semicolons are treated as literal characters when
 * {@code quarkus.http.use-semicolon-as-query-param-delimiter} is set to {@code false}.
 */
@Disabled("Settings not present in Vert.x 5. It needs to be investigated.")
public class SemicolonQueryParamDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SemicolonQueryParamDefaultTest.Routes.class))
            .withRuntimeConfiguration("""
                    quarkus.http.use-semicolon-as-query-param-delimiter=false
                    """);

    @Test
    public void testSemicolonIsLiteralWhenDisabled() {
        given()
                .urlEncodingEnabled(false)
                .get("/echo?a=1;b=2")
                .then()
                .statusCode(200)
                .body(is("a=1;b=2|b=null"));
    }

    @Test
    public void testAmpersandStillWorksAsDelimiter() {
        given().urlEncodingEnabled(false)
                .get("/echo?a=1&b=2")
                .then()
                .statusCode(200)
                .body(is("a=1|b=2"));
    }

    @ApplicationScoped
    static class Routes {
        public void register(@Observes Router router) {
            router.route("/echo").handler(rc -> {
                String a = rc.queryParams().get("a");
                String b = rc.queryParams().get("b");
                rc.response().end("a=" + a + "|b=" + b);
            });
        }
    }
}
