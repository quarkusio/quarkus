package io.quarkus.resteasy.test.files;

import static org.hamcrest.CoreMatchers.containsString;

import java.io.File;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.test.RootResource;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test that static files are served even with resources.
 */
public class StaticFileWithResourcesHttpRootTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(RootResource.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/foo"), "application.properties")
                    .addAsResource(new File("src/test/resources/lorem.txt"), "META-INF/resources/lorem.txt")
                    .addAsResource(new File("src/test/resources/index.html"), "META-INF/resources/web/index.html"));

    @Test
    public void test() {

        RestAssured.get("/web/index.html").then()
                .statusCode(200)
                .body(containsString("<h1>Hello</h1>"));

        RestAssured.get("/lorem.txt").then()
                .statusCode(200)
                .body(containsString("Lorem"));

        RestAssured.get("/").then()
                .statusCode(200)
                .body(containsString("Root Resource"));
    }
}
