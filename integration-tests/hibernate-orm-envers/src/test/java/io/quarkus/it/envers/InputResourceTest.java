package io.quarkus.it.envers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class InputResourceTest {

    @Test
    void test() {
        given().contentType(ContentType.JSON).accept(ContentType.TEXT)
                .when().post("/jpa-envers-test/input")
                .then()
                .statusCode(200)
                .body(equalTo("in"));
    }
}
