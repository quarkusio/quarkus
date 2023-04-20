package io.quarkus.it.envers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class OutputResourceTest {

    @Test
    void test() {
        given().accept(ContentType.JSON)
                .when()
                .get("/jpa-envers-test/output")
                .then()
                .statusCode(200)
                .body("data", equalTo("out"));
    }
}
