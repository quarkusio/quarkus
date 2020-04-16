package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.is;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ContextPathTestCase {

    private static final String CONTEXT_PATH = "/foo";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestServlet.class, TestGreeter.class)
                    .addAsResource(new StringAsset("index"), "META-INF/resources/index.html")
                    .addAsResource(new StringAsset("quarkus.servlet.context-path=" + CONTEXT_PATH), "application.properties"));

    @Test
    public void testServlet() {
        RestAssured.when().get("/foo/test").then()
                .statusCode(200)
                .body(is("test servlet"));
    }

    @Test
    public void testNoSlash() {
        RestAssured.given().redirects().follow(false).when().get("/foo").then()
                .statusCode(302);
        RestAssured.when().get("/foo").then()
                .statusCode(200)
                .body(is("index"));
    }
}
