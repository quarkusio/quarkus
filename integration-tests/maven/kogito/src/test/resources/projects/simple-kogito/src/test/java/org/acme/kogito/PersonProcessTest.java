package org.acme.kogito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.kogito.Application;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class PersonProcessTest {

    @Inject
    Application application;

    @Test
    public void testAdult() {
        given()
               .body("{\"person\": {\"name\":\"John Quark\", \"age\": 20}}")
               .contentType(ContentType.JSON)
          .when()
               .post("/persons")
          .then()
             .statusCode(200)
             .body("person.adult", is(true));
    }

    @Test
    public void testChild() {
        given()
               .body("{\"person\": {\"name\":\"Jenny Quark\", \"age\": 15}}")
               .contentType(ContentType.JSON)
          .when()
               .post("/persons")
          .then()
             .statusCode(200)
             .body("person.adult", is(false));
    }
}
