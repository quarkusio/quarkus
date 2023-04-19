package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.is

@QuarkusTest
class SharedResourceTest {

    @Test
    void testReturnAsIs() {
        given()
            .body("""{ "message": "will not be used" }""")
            .contentType(ContentType.JSON)
            .when()
            .post("/shared")
            .then()
            .statusCode(200)
            .body(is("""{"message": "canned+canned"}"""))
    }

    @Test
    void testApplicationSuppliedProviderIsPreferred() {
        given()
            .body("""{ "message": "will not be used" }""")
            .contentType(ContentType.TEXT)
            .accept(ContentType.TEXT)
            .when()
            .post("/shared")
            .then()
            .statusCode(200)
            .body(is("""{"message": "app+app"}"""))
    }
}
