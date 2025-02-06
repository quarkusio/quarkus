package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

class AutoBadRequestTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(AutoBadRequestResource.class));

    @Test
    void testInOpenApi() {
        RestAssured.given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("paths.'/auto'.post.responses.400.description", Matchers.is("Bad Request"))
                .and()
                .body("paths.'/auto'.put.responses.400.description", Matchers.is("Bad Request"));
    }

    @Test
    void testProvidedInOpenApi() {
        RestAssured.given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("paths.'/auto/provided'.post.responses.400.description", Matchers.is("Invalid bean supplied"))
                .and()
                .body("paths.'/auto/provided'.put.responses.400.description", Matchers.is("Invalid bean supplied"));
    }

    @Test
    void testNobodyInOpenApi() {
        RestAssured.given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("paths.'/auto/nobody'.post.responses.400", Matchers.is(Matchers.emptyOrNullString()))
                .and()
                .body("paths.'/auto/nobody'.put.responses.400", Matchers.is(Matchers.emptyOrNullString()));
    }

    @Test
    void testStringInOpenApi() {
        RestAssured.given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("paths.'/auto/string'.post.responses.400", Matchers.is(Matchers.emptyOrNullString()))
                .and()
                .body("paths.'/auto/string'.put.responses.400", Matchers.is(Matchers.emptyOrNullString()));
    }

    @Test
    void testFileInOpenApi() {
        RestAssured.given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("paths.'/auto/file'.post.responses.400", Matchers.is(Matchers.emptyOrNullString()))
                .and()
                .body("paths.'/auto/file'.put.responses.400", Matchers.is(Matchers.emptyOrNullString()));
    }

    @Test
    void testMultipartInOpenApi() {
        RestAssured.given().header("Accept", "application/json")
                .when()
                .get("/q/openapi")
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(200)
                .body("paths.'/auto/multipart'.post.responses.400", Matchers.is(Matchers.emptyOrNullString()))
                .and()
                .body("paths.'/auto/multipart'.put.responses.400", Matchers.is(Matchers.emptyOrNullString()));
    }
}
