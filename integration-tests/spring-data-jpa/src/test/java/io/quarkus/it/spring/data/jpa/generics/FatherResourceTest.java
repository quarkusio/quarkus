package io.quarkus.it.spring.data.jpa.generics;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class FatherResourceTest {

    @Test
    public void shouldReturnEveParents() {
        given()
                .when().get("/rest/fathers/Eve")
                .then()
                .statusCode(200).body(containsString("3"));
    }

}
