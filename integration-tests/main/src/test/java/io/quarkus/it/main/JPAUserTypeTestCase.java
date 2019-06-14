package io.quarkus.it.main;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JPAUserTypeTestCase {

    @Test
    public void testCustomUserTypes() {
        RestAssured.when().get("/jpa/custom-user-types").then()
                .statusCode(200);
    }

}
