package io.quarkus.resteasy.test;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class NotFoundExceptionMapperTestCase {
    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RootResource.class));

    @Test
    public void testResourceNotFound() {
        // test the exception mapper provided in dev mode, if no accept, will just return a plain 404
        RestAssured.when().get("/not_found")
                .then()
                .statusCode(404);
    }

    @Test
    public void testHtmlResourceNotFound() {
        // test the exception mapper provided in dev mode, if no accept, will just return a plain 404
        RestAssured.given().accept(ContentType.HTML)
                .when().get("/not_found")
                .then()
                .statusCode(404)
                .contentType(ContentType.HTML)
                .body(Matchers.containsString("<h1 class=\"container\">404 - Resource Not Found</h1>"));
    }

    @Test
    public void testJsonResourceNotFound() {
        // test the default exception mapper provided in dev mode : displays json when accept is application/json
        RestAssured.given().accept(ContentType.JSON)
                .when().get("/not_found")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON);
    }

}
