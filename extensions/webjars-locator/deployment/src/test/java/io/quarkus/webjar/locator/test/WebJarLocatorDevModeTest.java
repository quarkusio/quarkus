package io.quarkus.webjar.locator.test;

import static org.hamcrest.core.Is.is;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class WebJarLocatorDevModeTest {
    private static final String META_INF_RESOURCES = "META-INF/resources/";

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(PostResource.class)
                    .addAsResource(new StringAsset("<html>Hello!<html>"), META_INF_RESOURCES + "/index.html")
                    .addAsResource(new StringAsset("Test"), META_INF_RESOURCES + "/some/path/test.txt"));

    @Test
    public void testDevMode() {
        // Test Endpoint
        RestAssured.given().body("Stuart")
                .when()
                .post("/post")
                .then()
                .body(Matchers.equalTo("Hello: Stuart"));
        // Test normal files
        RestAssured.get("/").then()
                .statusCode(200)
                .body(is("<html>Hello!<html>"));

        RestAssured.get("/index.html").then()
                .statusCode(200)
                .body(is("<html>Hello!<html>"));

        RestAssured.get("/some/path/test.txt").then()
                .statusCode(200)
                .body(is("Test"));

        // Test Existing Web Jars
        RestAssured.get("/webjars/jquery/jquery.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/min/moment.min.js").then()
                .statusCode(200);

        // Test using version in url of existing Web Jar
        RestAssured.get("/webjars/jquery/3.5.1/jquery.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/2.24.0/min/moment.min.js").then()
                .statusCode(200);

        // Test non-existing Web Jar
        RestAssured.get("/webjars/bootstrap/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/bootstrap/4.3.1/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/momentjs/2.25.0/min/moment.min.js").then()
                .statusCode(404);

        // Test webjar that does not have a version in the jar path
        RestAssured.get("/webjars/dcjs/dc.min.js").then()
                .statusCode(200);

        // Change a source file
        test.modifySourceFile(PostResource.class, s -> s.replace("Hello:", "Hi:"));

        // Test modified endpoint
        RestAssured.given().body("Stuart")
                .when()
                .post("/post")
                .then()
                .body(Matchers.equalTo("Hi: Stuart"));

        // Test normal files
        RestAssured.get("/").then()
                .statusCode(200)
                .body(is("<html>Hello!<html>"));

        RestAssured.get("/index.html").then()
                .statusCode(200)
                .body(is("<html>Hello!<html>"));

        RestAssured.get("/some/path/test.txt").then()
                .statusCode(200)
                .body(is("Test"));

        // Test Existing Web Jars
        RestAssured.get("/webjars/jquery/jquery.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/min/moment.min.js").then()
                .statusCode(200);

        // Test using version in url of existing Web Jar
        RestAssured.get("/webjars/jquery/3.5.1/jquery.min.js").then()
                .statusCode(200);
        RestAssured.get("/webjars/momentjs/2.24.0/min/moment.min.js").then()
                .statusCode(200);

        // Test non-existing Web Jar
        RestAssured.get("/webjars/bootstrap/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/bootstrap/4.3.1/js/bootstrap.min.js").then()
                .statusCode(404);
        RestAssured.get("/webjars/momentjs/2.25.0/min/moment.min.js").then()
                .statusCode(404);
    }
}
