package io.quarkus.it.amazon.lambda;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    // TODO: This is using the old deprecated LambdaClient test API.  I am keeping it here to test backward compatibility
    // these tests will need to be ported once LambdaClient is removed from Quarkus.

    @Test
    public void testCustomIDPSecurityContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/security/username");
        request.getRequestContext().setAuthorizer(new APIGatewayV2HTTPEvent.RequestContext.Authorizer());
        request.getRequestContext().getAuthorizer().setLambda(new HashMap<String, Object>());
        request.getRequestContext().getAuthorizer().getLambda().put("test", "test");
        request.getHeaders().put("x-user", "John");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "John");
    }

    @Test
    public void testContext() throws Exception {
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();
        request.setRawPath("/hello/context");
        request.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        request.getRequestContext().setHttp(new APIGatewayV2HTTPEvent.RequestContext.Http());
        request.getRequestContext().getHttp().setMethod("GET");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 204);
    }

    @Test
    public void testGetText() throws Exception {
        testGetText("/hello");
    }

    private String body(APIGatewayV2HTTPResponse response) {
        if (!response.getIsBase64Encoded())
            return response.getBody();
        return new String(Base64.decodeBase64(response.getBody()));
    }

    private void testGetText(String path) {
        APIGatewayV2HTTPEvent request = request(path);
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "hello");
        Assertions.assertTrue(out.getHeaders().get("Content-Type").startsWith("text/plain"));
    }

    private APIGatewayV2HTTPEvent request(String path) {
        APIGatewayV2HTTPEvent request = new APIGatewayV2HTTPEvent();
        request.setHeaders(new HashMap<>());
        request.setRawPath(path);
        request.setRequestContext(new APIGatewayV2HTTPEvent.RequestContext());
        request.getRequestContext().setHttp(new APIGatewayV2HTTPEvent.RequestContext.Http());
        request.getRequestContext().getHttp().setMethod("GET");
        return request;
    }

    @Test
    public void test404() throws Exception {
        APIGatewayV2HTTPEvent request = request("/nowhere");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 404);
    }

    @Test
    public void testPostText() throws Exception {
        testPostText("/hello");
    }

    private void testPostText(String path) {
        APIGatewayV2HTTPEvent request = request(path);
        request.getRequestContext().getHttp().setMethod("POST");
        request.setHeaders(new HashMap<>());
        request.getHeaders().put("Content-Type", "text/plain");
        request.setBody("Bill");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "hello Bill");
        Assertions.assertTrue(out.getHeaders().get("Content-Type").startsWith("text/plain"));
    }

    @Test
    public void testPostBinary() throws Exception {
        byte[] bytes = { 0, 1, 2, 3 };
        String body = Base64.encodeBase64String(bytes);
        APIGatewayV2HTTPEvent request = request("/hello");
        request.getRequestContext().getHttp().setMethod("POST");
        request.setHeaders(new HashMap<>());
        request.getHeaders().put("Content-Type", MediaType.APPLICATION_OCTET_STREAM);
        request.setBody(body);
        request.setIsBase64Encoded(true);
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(out.getHeaders().get("Content-Type"),
                MediaType.APPLICATION_OCTET_STREAM);
        Assertions.assertTrue(out.getIsBase64Encoded());
        byte[] rtn = Base64.decodeBase64(out.getBody());
        Assertions.assertEquals(rtn[0], 4);
        Assertions.assertEquals(rtn[1], 5);
        Assertions.assertEquals(rtn[2], 6);

    }

    @Test
    public void testPostEmpty() throws Exception {
        APIGatewayV2HTTPEvent request = request("/hello/empty");
        request.getRequestContext().getHttp().setMethod("POST");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 204);

    }

    @Test
    public void testProxyRequestContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/hello/proxyRequestContext");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 204);
    }

}
