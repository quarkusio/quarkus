package io.quarkus.it.amazon.lambda;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testGetText() {
        testGetText("/vertx/hello");
        testGetText("/servlet/hello");
        testGetText("/hello");
    }

    @Test
    public void testGetJson() {
        testGetJson("/vertx/json");
        testGetJson("/servlet/json");
        testGetJson("/hello/json");
    }

    @Test
    public void test404() {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath("/nowhere");
        AwsProxyResponse out = LambdaClient.invoke(AwsProxyResponse.class, request);
        Assertions.assertEquals(404, out.getStatusCode());
    }

    @Test
    public void testPostText() {
        testPostText("/hello");
        testPostText("/servlet/hello");
        testPostText("/vertx/hello");
    }

    @Test
    public void testPostBinary() {
        AwsProxyRequest request = new AwsProxyRequest();
        byte[] bytes = { 0, 1, 2, 3 };
        String body = Base64.encodeBase64String(bytes);
        request.setHttpMethod("POST");
        request.setMultiValueHeaders(new Headers());
        request.getMultiValueHeaders().add("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        request.setPath("/hello");
        request.setBody(body);
        request.setIsBase64Encoded(true);
        AwsProxyResponse out = LambdaClient.invoke(AwsProxyResponse.class, request);
        Assertions.assertEquals(200, out.getStatusCode());
        Assertions.assertEquals(MediaType.APPLICATION_OCTET_STREAM, out.getMultiValueHeaders().getFirst("Content-Type"));
        Assertions.assertTrue(out.isBase64Encoded());
        byte[] rtn = Base64.decodeBase64(out.getBody());
        Assertions.assertEquals(rtn[0], 4);
        Assertions.assertEquals(rtn[1], 5);
        Assertions.assertEquals(rtn[2], 6);

    }

    @Test
    public void testPostEmpty() {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("POST");
        request.setMultiValueHeaders(new Headers());
        request.setPath("/hello/empty");
        AwsProxyResponse out = LambdaClient.invoke(AwsProxyResponse.class, request);
        Assertions.assertEquals(204, out.getStatusCode());
    }

    private String body(AwsProxyResponse response) {
        return response.isBase64Encoded()
                ? new String(Base64.decodeBase64(response.getBody()))
                : response.getBody();
    }

    private void testGetText(String path) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath(path);
        AwsProxyResponse out = LambdaClient.invoke(AwsProxyResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "hello");
        Assertions.assertTrue(out.getMultiValueHeaders().getFirst("Content-Type").startsWith("text/plain"));
    }

    private void testGetJson(String path) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath(path);
        AwsProxyResponse out = LambdaClient.invoke(AwsProxyResponse.class, request);
        Assertions.assertEquals(200, out.getStatusCode());
        Assertions.assertEquals("{\"hello\":\"world\"}", body(out));
        Assertions.assertTrue(out.getMultiValueHeaders().getFirst("Content-Type").startsWith(APPLICATION_JSON));
    }

    private void testPostText(String path) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("POST");
        request.setMultiValueHeaders(new Headers());
        request.getMultiValueHeaders().add("Content-Type", "text/plain");
        request.setPath(path);
        request.setBody("Bill");
        AwsProxyResponse out = LambdaClient.invoke(AwsProxyResponse.class, request);
        Assertions.assertEquals(200, out.getStatusCode());
        Assertions.assertEquals("hello Bill", body(out));
        Assertions.assertTrue(out.getMultiValueHeaders().getFirst("Content-Type").startsWith("text/plain"));
    }
}
