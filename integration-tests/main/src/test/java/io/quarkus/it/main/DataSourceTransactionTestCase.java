package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class DataSourceTransactionTestCase {

    @Test
    public void testTransactionalAnnotation() {
        RestAssured.when().get("/datasource/txninterceptor0").then()
                .body(is("PASSED"));

        RestAssured.when().get("/datasource/txninterceptor1").then()
                .statusCode(500);

        RestAssured.when().get("/datasource/txninterceptor2").then()
                .body(is("PASSED"));
    }

}
