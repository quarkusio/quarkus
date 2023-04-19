package io.quarkus.it.resteasy.reactive.groovy

import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import org.junit.jupiter.api.Test

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.is

@QuarkusTest
class ReactiveClientTest {

    @Test
    void testGetCountryByName() {
        given()
            .when()
            .get("/country/name/foo")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body(
                    '$.size()',
                    is(1),
                    "[0].capital",
                    is("foo-capital")
            )
    }
}
