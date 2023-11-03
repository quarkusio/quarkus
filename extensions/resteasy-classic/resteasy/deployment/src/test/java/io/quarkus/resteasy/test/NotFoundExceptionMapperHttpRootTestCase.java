package io.quarkus.resteasy.test;

import static org.hamcrest.CoreMatchers.containsString;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class NotFoundExceptionMapperHttpRootTestCase {
    private static final String META_INF_RESOURCES = "META-INF/resources/";

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RootResource.class)
                    .addAsResource(new StringAsset("quarkus.http.root-path=/abc"), "application.properties")
                    .addAsResource(new StringAsset("index content"), META_INF_RESOURCES + "index.html"));

    @Test
    public void testHtmlResourceNotFound() {
        // test the exception mapper provided in dev mode, if no accept, will just return a plain 404
        RestAssured.given().accept(ContentType.HTML)
                .when().get("/abc/not_found")
                .then()
                .statusCode(404)
                .contentType(ContentType.HTML)
                .body(containsString("\"/abc/index.html")) // check that index.html is displayed
                .body(Matchers.containsString("<h1 class=\"container\">404 - Resource Not Found</h1>"));
    }
}
