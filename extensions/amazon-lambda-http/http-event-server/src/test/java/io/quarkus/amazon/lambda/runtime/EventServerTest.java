package io.quarkus.amazon.lambda.runtime;

import java.util.HashMap;
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

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class EventServerTest {

    static MockEventServer server;
    static ObjectMapper mapper;
    static ObjectReader eventReader;
    static ObjectWriter resWriter;

    @BeforeAll
    public static void start() {
        server = new MockHttpEventServer();
        server.start();
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        eventReader = mapper.readerFor(APIGatewayV2HTTPEvent.class);
        resWriter = mapper.writerFor(APIGatewayV2HTTPResponse.class);
    }

    @AfterAll
    public static void end() throws Exception {
        server.close();
    }

    @Test
    public void testServer() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget base = client.target("http://localhost:" + MockEventServer.DEFAULT_PORT);
        Future<Response> lambdaInvoke = base.request().async()
                .post(Entity.text("Hello World"));

        Response next = base.path(MockEventServer.NEXT_INVOCATION).request().get();
        Assertions.assertEquals(200, next.getStatus());
        String requestId = next.getHeaderString(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
        String traceId = next.getHeaderString(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY);
        Assertions.assertNotNull(requestId);
        Assertions.assertNotNull(traceId);
        String json = next.readEntity(String.class);
        APIGatewayV2HTTPEvent event = eventReader.readValue(json);
        Assertions.assertEquals("text/plain", event.getHeaders().get("Content-Type"));
        Assertions.assertEquals("Hello World", event.getBody());
        next.close();

        APIGatewayV2HTTPResponse res = new APIGatewayV2HTTPResponse();
        res.setStatusCode(201);
        res.setHeaders(new HashMap());
        res.getHeaders().put("Content-Type", "text/plain");
        res.setBody("Hi");
        Response sendResponse = base.path(MockEventServer.INVOCATION).path(requestId).path("response")
                .request().post(Entity.json(resWriter.writeValueAsString(res)));
        Assertions.assertEquals(204, sendResponse.getStatus());
        sendResponse.close();

        Response lambdaResponse = lambdaInvoke.get();
        Assertions.assertEquals(201, lambdaResponse.getStatus());
        Assertions.assertEquals("Hi", lambdaResponse.readEntity(String.class));
        lambdaResponse.close();
    }
}
