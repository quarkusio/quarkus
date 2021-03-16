package io.quarkus.it.envers;

import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class JPAEnversTest {

    @Test
    public void testProjectAtRevisionJpa() {
        RestAssured.when().get("/jpa-envers-test/project/1").then()
                .body(containsString("Quarkus"));
    }

}
