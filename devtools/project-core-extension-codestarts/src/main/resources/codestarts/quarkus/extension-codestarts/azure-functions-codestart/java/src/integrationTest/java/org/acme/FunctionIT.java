package org.acme;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusIntegrationTest
public class FunctionIT {

    @Test
    public void testIt() {
        given()
                .when().queryParam("name", "Bill")
                .get("/api/HttpExample")
                .then()
                .statusCode(200)
                .body(is("Guten Tag Bill"));
    }
}
