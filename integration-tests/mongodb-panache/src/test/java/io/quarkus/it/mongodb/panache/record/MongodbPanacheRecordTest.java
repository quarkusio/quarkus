package io.quarkus.it.mongodb.panache.record;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class MongodbPanacheRecordTest {

    private static final String ROOT_URL = "/persons/record";

    @Test
    void testRecordInPanache() {
        var person1 = new PersonRecord("Loïc", "Mathieu", Status.ALIVE);
        var person2 = new PersonRecord("Zombie", "Zombie", Status.DEAD);

        given().body(person1).contentType(ContentType.JSON)
                .when().post(ROOT_URL)
                .then().statusCode(204);
        given().body(person2).contentType(ContentType.JSON)
                .when().post(ROOT_URL)
                .then().statusCode(204);

        when().get(ROOT_URL)
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }
}
