package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class DatasourceTestCase {

    @Test
    public void testDataSource() {
        RestAssured.when().get("/datasource").then()
                .body(is("10"));
    }

    @Test
    public void testDataSourceTransactions() {
        RestAssured.when().get("/datasource/txn").then()
                .body(is("PASSED"));
    }

}
