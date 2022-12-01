package io.quarkus.resteasy.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

public class NotFoundExceptionMapperTestCase {
    private static final String META_INF_RESOURCES = "META-INF/resources/";

    @RegisterExtension
    static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(RootResource.class)
                    .addAsResource(new StringAsset("index content"), META_INF_RESOURCES + "index.html"));

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
                .body(containsString("\"/index.html")) // check that index.html is displayed
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

    @Test
    public void shouldDisplayNewAddedFileIn404ErrorPage() {
        String CONTENT = "html content";
        test.addResourceFile(META_INF_RESOURCES + "index2.html", CONTENT);

        RestAssured.get("/index2.html")
                .then()
                .statusCode(200)
                .body(containsString(CONTENT)); // check that index2.html is live reloaded

        RestAssured.given()
                .accept(ContentType.HTML)
                .when()
                .get("/api")
                .then() // try to load unknown path
                .statusCode(404)
                .body(containsString("index2.html")); // check that index2.html is displayed
    }

    @Test
    public void shouldNotDisplayDeletedFileIn404ErrorPage() {
        String TEST_CONTENT = "test html content";
        test.addResourceFile(META_INF_RESOURCES + "test.html", TEST_CONTENT);

        RestAssured
                .get("/test.html")
                .then()
                .statusCode(200)
                .body(containsString(TEST_CONTENT)); // check that test.html is live reloaded

        test.deleteResourceFile(META_INF_RESOURCES + "test.html"); // delete test.html file

        RestAssured
                .given()
                .accept(ContentType.HTML)
                .when()
                .get("/test.html")
                .then() // try to load static file
                .statusCode(404)
                .body(not(containsString("test.html"))); // check that test.html is not displayed

    }
}
