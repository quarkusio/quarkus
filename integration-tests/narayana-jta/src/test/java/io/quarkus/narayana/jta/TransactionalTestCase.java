package io.quarkus.narayana.jta;

import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(TransactionalResource.class)
public class TransactionalTestCase {

    @Test
    public void test() {
        RestAssured.when().get("/status").then().body(is("0"));
    }
}
