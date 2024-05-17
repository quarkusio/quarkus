package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test reflection around JPA entities
 */
@QuarkusTest
public class JPABootstrapTestCase {

    @Test
    public void testJpaBootstrap() throws Exception {
        RestAssured.when().get("/jpa/testbootstrap").then()
                .body(is("OK"));
    }

}
