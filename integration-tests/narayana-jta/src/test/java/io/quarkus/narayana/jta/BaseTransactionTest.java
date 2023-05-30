package io.quarkus.narayana.jta;

import static org.hamcrest.core.Is.is;

import io.restassured.RestAssured;

public class BaseTransactionTest {

    public void runTest() {
        RestAssured.when().get("/status").then().body(is("0"));
    }
}
