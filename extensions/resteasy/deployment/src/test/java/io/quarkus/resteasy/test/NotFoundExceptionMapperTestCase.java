package io.quarkus.resteasy.test;

import static org.hamcrest.Matchers.is;

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
    public void testHtmlResourceNotFound() {
        // test the default exception mapper provided in dev mode : displays HTML by default
        RestAssured.when().get("/not_found")
                .then()
                .statusCode(404)
                .body(Matchers.containsString("<div class=\"component-name\"><h1>Resource Not Found</h1>"));
    }

    @Test
    public void testJsonResourceNotFound() {
        // test the default exception mapper provided in dev mode : displays json when accept is application/json
        RestAssured.given().accept(ContentType.JSON)
                .when().get("/not_found")
                .then()
                .statusCode(404)
                .body("errorMessage", is("Resource Not Found"))
                .body("existingResourcesDetails[0].basePath", is("/"));
    }

}
