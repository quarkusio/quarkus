package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class TransactionTestCase {

    @Test
    public void testTransaction() {
        RestAssured.when().get("/txn").then()
                .body(is("true"));
    }

}
