package io.quarkus.it.mongodb.panache.person;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.mongodb.MongoTestResource;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(MongoTestResource.class)
class PersonResourceTest {
    private static final String ROOT_URL = "/mongo/persons";

    @Test
    void testRecordInPanache() {
        var person1 = new Person();
        person1.firstname = "Loïc";
        person1.lastname = "Mathieu";
        person1.status = Status.ALIVE;
        var person2 = new Person();
        person1.firstname = "Zombie";
        person2.lastname = "Zombie";
        person2.status = Status.DEAD;

        given().body(person1).contentType(ContentType.JSON)
                .when().post(ROOT_URL)
                .then().statusCode(201);
        given().body(person2).contentType(ContentType.JSON)
                .when().post(ROOT_URL)
                .then().statusCode(201);

        when().get(ROOT_URL)
                .then()
                .statusCode(200)
                .body("size()", is(2));
    }

}
