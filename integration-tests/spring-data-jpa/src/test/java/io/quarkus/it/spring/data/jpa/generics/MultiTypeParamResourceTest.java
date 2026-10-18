package io.quarkus.it.spring.data.jpa.generics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MultiTypeParamResourceTest {

    @Test
    public void shouldFindAllTestById() {
        given()
                .when().get("/rest/multi-type-param-fathers/findAllTestById/1")
                .then()
                .statusCode(200).body(containsString("1"));
    }

    @Test
    public void shouldFindSomethingByIdAndName() {
        given()
                .when().get("/rest/multi-type-param-fathers/findSomethingByIdAndName/1/John Doe")
                .then()
                .statusCode(200).body(containsString("40:75.5"));
    }

    @Test
    public void shouldFindChildrenById() {
        given()
                .when().get("/rest/multi-type-param-fathers/findChildrenById/1")
                .then()
                .statusCode(200).body(containsString("3"));
    }
}
