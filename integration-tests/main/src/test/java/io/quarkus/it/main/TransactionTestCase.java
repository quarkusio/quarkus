package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.it.transaction.TransactionResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(TransactionResource.class)
public class TransactionTestCase {

    @Test
    public void testTransaction() {
        RestAssured.when().get().then()
                .body(is("true"));
    }

}
