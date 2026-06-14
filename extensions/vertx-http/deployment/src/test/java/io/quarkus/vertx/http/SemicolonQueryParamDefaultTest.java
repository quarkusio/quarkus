package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

/**
 * Tests that semicolons are treated as literal characters by default, i.e., when
 * {@code quarkus.http.use-semicolon-as-query-param-delimiter} is not configured
 * (defaults to {@code false}).
 */
public class SemicolonQueryParamDefaultTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Routes.class));

    @Test
    public void testSemicolonIsLiteralByDefault() {
        given()
                .urlEncodingEnabled(false)
                .get("/echo?a=1;b=2")
                .then()
                .statusCode(200)
                .body(is("a=1;b=2|b=null"));
    }

    @Test
    public void testAmpersandWorksAsDelimiterByDefault() {
        given()
                .urlEncodingEnabled(false)
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
