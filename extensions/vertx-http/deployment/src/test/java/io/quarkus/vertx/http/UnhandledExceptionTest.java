package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static io.restassured.config.HeaderConfig.headerConfig;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.hamcrest.Matcher;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class UnhandledExceptionTest {

    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_JSON = "text/json";
    private static final String TEXT_HTML = "text/html";
    private static final String APPLICATION_XML = "application/xml";
    private static final String TEXT_XML = "text/xml";
    private static final String APPLICATION_XHTML = "application/xhtml+xml";
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BeanRegisteringRouteThatThrowsException.class));

    @Test
    public void testNoAccept() {
        given().get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(APPLICATION_JSON) // Default to JSON
                .body(jsonBodyMatcher());
    }

    @Test
    public void testAcceptUnsupported() {
        given()
                .accept(APPLICATION_OCTET_STREAM)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(APPLICATION_JSON) // Default to JSON
                .body(jsonBodyMatcher());
    }

    @Test
    public void testAcceptApplicationJson() {
        given()
                .accept(APPLICATION_JSON)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(APPLICATION_JSON)
                .body(jsonBodyMatcher());
    }

    @Test
    public void testAcceptTextJson() {
        given()
                .accept(TEXT_JSON)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_JSON)
                .body(jsonBodyMatcher());
    }

    @Test
    public void testAcceptTextHtml() {
        given()
                .accept(TEXT_HTML)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML)
                .body(htmlBodyMatcher());
    }

    @Test
    public void testAcceptTextXml() {
        given()
                .accept(TEXT_XML)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML) // Not quite what they want, but better than nothing
                .body(htmlBodyMatcher());
    }

    @Test
    public void testAcceptApplicationXml() {
        given()
                .accept(APPLICATION_XML)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML) // Not quite what they want, but better than nothing
                .body(htmlBodyMatcher());
    }

    @Test
    public void testAcceptXHtml() {
        given()
                .accept(APPLICATION_XHTML)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML) // Not quite what they want, but better than nothing
                .body(htmlBodyMatcher());
    }

    @Test
    public void testAcceptWildcard() {
        given()
                .accept("text/*")
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_JSON)
                .body(jsonBodyMatcher());
    }

    @Test
    public void testAcceptParameter() {
        // We don't support accept parameters: they will be ignored.
        given()
                .accept("text/html;q=0.8")
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML)
                .body(htmlBodyMatcher());
    }

    @Test
    public void testMultipleAcceptHeaders() {
        RestAssuredConfig multipleAcceptHeadersConfig = RestAssured.config()
                .headerConfig(headerConfig().mergeHeadersWithName("Accept"));
        given()
                .config(multipleAcceptHeadersConfig)
                .accept(APPLICATION_JSON)
                .accept(TEXT_HTML)
                .accept(APPLICATION_OCTET_STREAM)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(APPLICATION_JSON)
                .body(jsonBodyMatcher());

        given()
                .config(multipleAcceptHeadersConfig)
                .accept(TEXT_HTML)
                .accept(APPLICATION_JSON)
                .accept(APPLICATION_OCTET_STREAM)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML)
                .body(htmlBodyMatcher());

        given()
                .config(multipleAcceptHeadersConfig)
                .accept(APPLICATION_OCTET_STREAM)
                .accept(TEXT_HTML)
                .accept(APPLICATION_JSON)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                // Ideally we'd like TEXT_HTML here, but due to some strange behavior of
                // io.vertx.ext.web.ParsedHeaderValues.findBestUserAcceptedIn,
                // we get this.
                .contentType(APPLICATION_JSON)
                .body(jsonBodyMatcher());

        given()
                .config(multipleAcceptHeadersConfig)
                .accept(APPLICATION_OCTET_STREAM)
                .accept(APPLICATION_JSON)
                .accept(TEXT_HTML)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(APPLICATION_JSON)
                .body(jsonBodyMatcher());
    }

    @Test
    public void testCompositeAcceptHeaders() {
        given()
                .accept(APPLICATION_JSON + ", " + TEXT_HTML + ", " + APPLICATION_OCTET_STREAM)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(APPLICATION_JSON)
                .body(jsonBodyMatcher());

        given()
                .accept(TEXT_HTML + ", " + APPLICATION_JSON + ", " + APPLICATION_OCTET_STREAM)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML)
                .body(htmlBodyMatcher());

        given()
                .accept(APPLICATION_OCTET_STREAM + ", " + TEXT_HTML + ", " + APPLICATION_JSON)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(TEXT_HTML)
                .body(htmlBodyMatcher());

        given()
                .accept(APPLICATION_OCTET_STREAM + ", " + APPLICATION_JSON + ", " + TEXT_HTML)
                .get("/unhandled-exception")
                .then()
                .statusCode(500)
                .contentType(APPLICATION_JSON)
                .body(jsonBodyMatcher());
    }

    private Matcher<String> jsonBodyMatcher() {
        return allOf(
                containsString("\"details\":\"Error id"),
                containsString("\"stack\":\"java.lang.RuntimeException: Simulated failure"),
                containsString("at " + BeanRegisteringRouteThatThrowsException.class.getName() + "$1.handle"));
    }

    private Matcher<String> htmlBodyMatcher() {
        return allOf(
                containsString("<!doctype html>"),
                containsString("<title>Internal Server Error"),
                containsString("java.lang.RuntimeException: Simulated failure"),
                containsString("at " + BeanRegisteringRouteThatThrowsException.class.getName() + "$1.handle"));
    }

    @ApplicationScoped
    static class BeanRegisteringRouteThatThrowsException {

        public void register(@Observes Router router) {
            router.route("/unhandled-exception").handler(new Handler<RoutingContext>() {
                @Override
                public void handle(RoutingContext event) {
                    throw new RuntimeException("Simulated failure");
                }
            });
        }

    }

}
