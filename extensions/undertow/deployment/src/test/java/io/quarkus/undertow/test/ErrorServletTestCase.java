package io.quarkus.undertow.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class ErrorServletTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ErrorServlet.class));

    @Test
    public void testHtmlError() {
        RestAssured.when().get("/error").then()
                .statusCode(500)
                .body(containsString("<h1 class=\"container\">Internal Server Error</h1>"))
                .body(containsString("<div id=\"stacktrace\">"));
    }

    @Test
    public void testJsonError() {
        RestAssured.given().accept(ContentType.JSON)
                .when().get("/error").then()
                .statusCode(500)
                .body("details", startsWith("Error id"))
                .body("stack", startsWith("java.lang.RuntimeException: Error !!!"));
    }
}
