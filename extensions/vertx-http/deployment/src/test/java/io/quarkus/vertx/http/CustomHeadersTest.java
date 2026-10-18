package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.ext.web.Router;

/**
 * Tests the quarkus.http.header configuration feature.
 */
public class CustomHeadersTest {

    private static final String APP_PROPS = """
            quarkus.http.header.myheader.value=global-value
            quarkus.http.header.myheader.path=/*
            quarkus.http.header.apionly.value=api-value
            quarkus.http.header.apionly.path=/api/*
            quarkus.http.header.getonly.value=get-value
            quarkus.http.header.getonly.path=/*
            quarkus.http.header.getonly.methods=GET
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(Routes.class));

    @Test
    public void testGlobalHeaderOnAllPaths() {
        given().get("/hello")
                .then()
                .statusCode(200)
                .header("myheader", is("global-value"));

        given().get("/api/data")
                .then()
                .statusCode(200)
                .header("myheader", is("global-value"));
    }

    @Test
    public void testPathSpecificHeader() {
        given().get("/api/data")
                .then()
                .statusCode(200)
                .header("apionly", is("api-value"));

        // Should NOT be present on non-api paths
        given().get("/hello")
                .then()
                .statusCode(200)
                .header("apionly", is(nullValue()));
    }

    @Test
    public void testMethodSpecificHeader() {
        given().get("/hello")
                .then()
                .statusCode(200)
                .header("getonly", is("get-value"));

        // POST should NOT get the GET-only header
        given().post("/hello")
                .then()
                .statusCode(200)
                .header("getonly", is(nullValue()));
    }

    @ApplicationScoped
    static class Routes {
        public void register(@Observes Router router) {
            router.route("/hello").handler(rc -> rc.response().end("hello"));
            router.route("/api/data").handler(rc -> rc.response().end("data"));
        }
    }
}
