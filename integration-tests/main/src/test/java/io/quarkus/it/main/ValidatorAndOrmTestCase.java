package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ValidatorAndOrmTestCase {

    @Test
    public void triggerValidatorAndHibernateORMTests() {
        RestAssured.when().get("/validator-and-orm/store").then()
                .body(is("passed"));
        RestAssured.when().get("/validator-and-orm/load").then()
                .body(is("passed"));
    }

}
