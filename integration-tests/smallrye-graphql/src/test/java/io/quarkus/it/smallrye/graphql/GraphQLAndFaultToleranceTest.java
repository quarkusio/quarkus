package io.quarkus.it.smallrye.graphql;

import static io.quarkus.it.smallrye.graphql.PayloadCreator.MEDIATYPE_JSON;
import static io.quarkus.it.smallrye.graphql.PayloadCreator.getPayload;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GraphQLAndFaultToleranceTest {

    @Test
    public void testGraphQLAndFaultToleranceTogether() {
        String helloRequest = getPayload("{\n" +
                "  faultTolerance\n" +
                "}");

        given()
                .when()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(helloRequest)
                .post("/graphql")
                .then()
                .statusCode(200)
                .body("errors[0].message", containsString("Timeout"));
    }
}
