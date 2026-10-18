package io.quarkus.hibernate.orm;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test for continuous testing with hibernate
 */
@QuarkusTest
public class HibernateET {

    @Test
    public void testImport() {
        RestAssured.when().get("/my-entity/1").then().body(is("MyEntity:TEST ENTITY"));
    }

}
