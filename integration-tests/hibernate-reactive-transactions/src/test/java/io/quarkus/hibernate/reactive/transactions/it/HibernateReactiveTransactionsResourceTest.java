package io.quarkus.hibernate.reactive.transactions.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class HibernateReactiveTransactionsResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/quarkus-reactive-transactions")
                .then()
                .statusCode(200)
                .body(is("Hello quarkus-reactive-transactions"));
    }
}
