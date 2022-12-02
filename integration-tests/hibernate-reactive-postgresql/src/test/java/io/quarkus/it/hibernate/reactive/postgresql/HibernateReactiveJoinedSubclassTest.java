package io.quarkus.it.hibernate.reactive.postgresql;

import static org.hamcrest.Matchers.emptyOrNullString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@TestHTTPEndpoint(HibernateReactiveTestEndpointJoinedSubclass.class)
public class HibernateReactiveJoinedSubclassTest {

    @Test
    public void deleteBookQuery() {
        RestAssured.when()
                .post("/prepareDb")
                .then()
                .statusCode(204);

        RestAssured.when()
                .delete("/deleteBook/6")
                .then()
                .statusCode(204)
                .body(emptyOrNullString());
    }
}
