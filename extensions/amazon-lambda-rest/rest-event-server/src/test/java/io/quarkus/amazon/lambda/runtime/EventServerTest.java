package io.quarkus.amazon.lambda.runtime;

import java.util.HashMap;
import java.util.concurrent.Future;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;

public class EventServerTest {

    static MockEventServer server;
    static ObjectMapper mapper;
    static ObjectReader eventReader;
    static ObjectWriter resWriter;

    @BeforeAll
    public static void start() {
        server = new MockRestEventServer();
        server.start();
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        eventReader = mapper.readerFor(AwsProxyRequest.class);
        resWriter = mapper.writerFor(AwsProxyResponse.class);
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
        AwsProxyRequest event = eventReader.readValue(json);
        Assertions.assertEquals("text/plain", event.getMultiValueHeaders().getFirst("Content-Type"));
        Assertions.assertEquals("Hello World", event.getBody());
        next.close();

        AwsProxyResponse res = new AwsProxyResponse();
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

    @Test
    public void testDateHeaders() throws Exception {
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
        AwsProxyRequest event = eventReader.readValue(json);
        Assertions.assertEquals("text/plain", event.getMultiValueHeaders().getFirst("Content-Type"));
        Assertions.assertEquals("Hello World", event.getBody());
        next.close();

        AwsProxyResponse res = new AwsProxyResponse();
        res.setStatusCode(201);
        res.setMultiValueHeaders(new Headers());
        res.getMultiValueHeaders().add("Content-Type", "text/plain");
        res.getMultiValueHeaders().add("Last-Modified", "Tue, 26 Oct 2021 01:01:01 GMT");
        res.getMultiValueHeaders().add("Expires", "Tue, 26 Oct 2021 01:01:01 GMT");
        res.getMultiValueHeaders().add("Date", "Tue, 26 Oct 2021 01:01:01 GMT");
        res.setBody("Hi");
        Response sendResponse = base.path(MockEventServer.INVOCATION).path(requestId).path("response")
                .request().post(Entity.json(resWriter.writeValueAsString(res)));
        Assertions.assertEquals(204, sendResponse.getStatus());
        sendResponse.close();

        Response lambdaResponse = lambdaInvoke.get();
        Assertions.assertEquals(201, lambdaResponse.getStatus());
        Assertions.assertEquals("Hi", lambdaResponse.readEntity(String.class));
        Assertions.assertTrue(lambdaResponse.getStringHeaders().containsKey("Content-Type"));
        Assertions.assertEquals("Tue, 26 Oct 2021 01:01:01 GMT", lambdaResponse.getStringHeaders().getFirst("Last-Modified"));
        Assertions.assertEquals("Tue, 26 Oct 2021 01:01:01 GMT", lambdaResponse.getStringHeaders().getFirst("Expires"));
        Assertions.assertEquals("Tue, 26 Oct 2021 01:01:01 GMT", lambdaResponse.getStringHeaders().getFirst("Date"));
        lambdaResponse.close();
    }

    @Test
    public void testQueryParameters() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget base = client.target("http://localhost:" + MockEventServer.DEFAULT_PORT);
        Future<Response> lambdaInvoke = base.queryParam("foo", "bar")
                .queryParam("bar", "").request().async()
                .get();

        Response next = base.path(MockEventServer.NEXT_INVOCATION).request().get();
        Assertions.assertEquals(200, next.getStatus());
        String requestId = next.getHeaderString(AmazonLambdaApi.LAMBDA_RUNTIME_AWS_REQUEST_ID);
        String traceId = next.getHeaderString(AmazonLambdaApi.LAMBDA_TRACE_HEADER_KEY);
        Assertions.assertNotNull(requestId);
        Assertions.assertNotNull(traceId);
        String json = next.readEntity(String.class);
        AwsProxyRequest event = eventReader.readValue(json);
        Assertions.assertEquals("bar", event.getMultiValueQueryStringParameters().getFirst("foo"));
        Assertions.assertEquals("", event.getMultiValueQueryStringParameters().getFirst("bar"));
        next.close();

        AwsProxyResponse res = new AwsProxyResponse();
        res.setStatusCode(201);
        res.setMultiValueHeaders(new Headers());
        res.getMultiValueHeaders().add("Content-Type", "text/plain");
        res.getMultiValueHeaders().add("Last-Modified", "Tue, 26 Oct 2021 01:01:01 GMT");
        res.getMultiValueHeaders().add("Expires", "Tue, 26 Oct 2021 01:01:01 GMT");
        res.getMultiValueHeaders().add("Date", "Tue, 26 Oct 2021 01:01:01 GMT");
        res.setBody("Hi");
        Response sendResponse = base.path(MockEventServer.INVOCATION).path(requestId).path("response")
                .request().post(Entity.json(resWriter.writeValueAsString(res)));
        Assertions.assertEquals(204, sendResponse.getStatus());
        sendResponse.close();

        Response lambdaResponse = lambdaInvoke.get();
        Assertions.assertEquals(201, lambdaResponse.getStatus());
        Assertions.assertEquals("Hi", lambdaResponse.readEntity(String.class));
        Assertions.assertTrue(lambdaResponse.getStringHeaders().containsKey("Content-Type"));
        Assertions.assertEquals("Tue, 26 Oct 2021 01:01:01 GMT", lambdaResponse.getStringHeaders().getFirst("Last-Modified"));
        Assertions.assertEquals("Tue, 26 Oct 2021 01:01:01 GMT", lambdaResponse.getStringHeaders().getFirst("Expires"));
        Assertions.assertEquals("Tue, 26 Oct 2021 01:01:01 GMT", lambdaResponse.getStringHeaders().getFirst("Date"));
        lambdaResponse.close();
    }

}
