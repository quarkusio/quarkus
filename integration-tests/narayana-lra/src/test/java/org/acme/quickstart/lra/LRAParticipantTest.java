package org.acme.quickstart.lra;

import static io.restassured.RestAssured.given;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.acme.quickstart.lra.coordinator.TransactionalResource;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(LRAParticipantTestResourceLifecycle.class)
@TestHTTPEndpoint(TransactionalResource.class)
public class LRAParticipantTest {
    private String coordinatorEndpoint;

    @BeforeEach
    public void setUp() {
        // start an external LRA coordinator and save its address
        coordinatorEndpoint = LRAParticipantTestResourceLifecycle.getCoordinatorEndpoint();
    }

    // test that the JAX-RS ServerLRAFilter can start and end an LRA in a single business method
    @Test
    public void testLRAStartEnd() throws IOException {
        Response response = given().when().post("/lra");

        assertEquals(HttpURLConnection.HTTP_CREATED, response.getStatusCode());
        // the id of the new LRA is in the Location header:
        String lraId = response.getHeaders().get("Location").getValue();
        assertNotNull(lraId, "invocation did not return an LRA");

        // read the status of the LRA. Since the LRA should have ended the coordinator will no longer know about it
        String lraStatus = coordinatorGetRequest(String.format("%s/status", lraId),
                new int[] { HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_OK }, false);

        if (lraStatus != null) {
            assertEquals(LRAStatus.Closed.name(), lraStatus, "LRA should have closed");
        }
    }

    // test that JAX-RS ServerLRAFilter can start and end an LRA in separate business methods
    @Test
    public void testLRA() throws IOException {
        Response response = given().when().post("/start");

        assertEquals(HttpURLConnection.HTTP_CREATED, response.getStatusCode());
        // the id of the new LRA is in the Location header:
        String lraId = response.getHeaders().get("Location").getValue();
        assertNotNull(lraId, "invocation did not return an LRA");

        // verify that the coordinator knows about the new LRA (the last component is the transaction uid
        String[] segments = lraId.split("/");
        assertNotEquals(0, segments.length);
        String uid = segments[segments.length - 1];

        String allLRAs = coordinatorGetRequest(coordinatorEndpoint, new int[] { HttpURLConnection.HTTP_OK }, true);
        assertNotNull(allLRAs);
        assertTrue(allLRAs.contains(uid));

        // read the status of the LRA. Since the LRA hasn't ended yet the coordinator should still know about it
        String lraStatus = coordinatorGetRequest(String.format("%s/%s/status", coordinatorEndpoint, uid),
                new int[] { HttpURLConnection.HTTP_OK }, true);
        assertEquals(LRAStatus.Active.name(), lraStatus, "LRA should still be active");

        // now call a method that should end the LRA:
        response = given().header(LRA_HTTP_CONTEXT_HEADER, lraId).when().put("/end");
        assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
        assertEquals(lraId, response.getBody().print());

        // read the status of the LRA. Since the LRA should have ended the coordinator will no longer know about it
        lraStatus = coordinatorGetRequest(String.format("%s/status", lraId),
                new int[] { HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_OK }, false);

        if (lraStatus != null) {
            assertEquals(LRAStatus.Closed.name(), lraStatus, "LRA should have closed");
        }
    }

    // send an HTTP GET request to an endpoint
    private static String coordinatorGetRequest(String endpoint, int[] expectedResponseCodes, boolean readResponse)
            throws IOException {
        URL obj = new URL(endpoint);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        assertTrue(Arrays.stream(expectedResponseCodes).anyMatch(i -> i == responseCode),
                "unexpected response code: GET " + endpoint);

        if (!readResponse) {
            return null;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();
        String res;

        while ((res = in.readLine()) != null) {
            response.append(res);
        }

        in.close();

        return response.toString();
    }
}
