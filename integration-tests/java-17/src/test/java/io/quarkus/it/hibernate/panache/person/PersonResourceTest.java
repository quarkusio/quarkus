package io.quarkus.it.hibernate.panache.person;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.mongodb.panache.person.Person;
import io.quarkus.it.mongodb.panache.person.Status;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
class PersonResourceTest {
    private static final String ROOT_URL = "/hibernate/persons";

    @Test
    @TestTransaction
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

    @Test
    @TestTransaction
    void testHqlPanacheProject() {
        when().get(ROOT_URL + "/hql-project")
                .then().statusCode(200)
                .body("size()", is(2));
    }
}
