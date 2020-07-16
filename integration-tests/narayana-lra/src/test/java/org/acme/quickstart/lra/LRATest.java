package org.acme.quickstart.lra;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;

@QuarkusTest
class LRATest {
    @Test
    void testStartLRA() {
        // ask the coordinator to start an LRA
        String lra = startLRA();

        // verify that the coordinator knows about the LRA
        String lras = getLRAs();

        assertTrue(lras.contains(lra), "The Narayana LRA coordinator does not know about an active LRA");

        // end the new LRA
        endLRA(lra);

        // verify the LRA closed
        lras = getLRAs();

        assertFalse(lras.contains(lra), "The Narayana LRA coordinator still knows about a closed LRA");
    }

    @Test
    void testParticipant() {
        given()
                .when()
                .post("/txns/tx")
                .then()
                .statusCode(200);

        Response response = given()
                .when().get("/txns/completions");

        String completions = response.getBody().print();

        assertEquals("1", completions);
    }

    private String startLRA() {
        Response response = given()
                .when()
                .post("/lra-coordinator/start");
        ResponseBody responseBody = response.getBody();

        // the coordinator should have created a new REST resource so check for 201 status code
        response
                .then()
                .statusCode(201);

        String body = responseBody.print();

        assertTrue(body.contains("/"), "Response body for a new LRA is not a URL");

        String lra = body.substring(body.lastIndexOf('/') + 1);

        assertNotEquals(0, lra.length(), "new LRA is not a valid URL");

        // return the URL corresponding to the new LRA
        return lra;
    }

    private void endLRA(String lra) {
        given()
                .when()
                .put(String.format("/lra-coordinator/%s/close", lra))
                .then()
                .statusCode(200);
    }

    private String getLRAs() {
        Response response = given()
                .when().get("/lra-coordinator");
        ResponseBody responseBody = response.getBody();

        response
                .then()
                .statusCode(200);

        // return a JSON array containing active LRAs
        return responseBody.print(); // actually a JSON encoding of List<LRAData>
    }
}
