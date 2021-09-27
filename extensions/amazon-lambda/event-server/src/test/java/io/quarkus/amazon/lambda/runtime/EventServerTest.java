package io.quarkus.amazon.lambda.runtime;

import java.util.concurrent.Future;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EventServerTest {

    static MockEventServer server;

    @BeforeAll
    public static void start() {
        server = new MockEventServer();
        server.start();
    }

    @AfterAll
    public static void end() throws Exception {
        server.close();
    }

    @Test
    public void testServer() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget base = client.target("http://localhost:" + MockEventServer.DEFAULT_PORT);
        WebTarget postEvent = base.path(MockEventServer.POST_EVENT);
        // test posting event works at '/_lambda'
        invokeTest(base, postEvent);
        // make sure posting event works on '/'
        invokeTest(base, base);
    }

    private void invokeTest(WebTarget base, WebTarget postEvent)
            throws InterruptedException, java.util.concurrent.ExecutionException {
        Future<Response> lambdaInvoke = postEvent.request().async().post(Entity.json("\"hello\""));

        Response next = base.path(MockEventServer.NEXT_INVOCATION).request().get();
        Assertions.assertEquals(200, next.getStatus());
        String requestId = next.getHeaderString(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
        String traceId = next.getHeaderString(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY);
        Assertions.assertNotNull(requestId);
        Assertions.assertNotNull(traceId);
        String json = next.readEntity(String.class);
        Assertions.assertEquals("\"hello\"", json);
        next.close();

        Response sendResponse = base.path(MockEventServer.INVOCATION).path(requestId).path("response")
                .request().post(Entity.json("\"good day\""));
        Assertions.assertEquals(204, sendResponse.getStatus());
        sendResponse.close();

        Response lambdaResponse = lambdaInvoke.get();
        Assertions.assertEquals(200, lambdaResponse.getStatus());
        Assertions.assertEquals("\"good day\"", lambdaResponse.readEntity(String.class));
        lambdaResponse.close();
    }
}
