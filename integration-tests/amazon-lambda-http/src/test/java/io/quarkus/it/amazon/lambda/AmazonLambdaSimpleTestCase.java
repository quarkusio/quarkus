package io.quarkus.it.amazon.lambda;

import java.util.HashMap;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

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
    public void testJaxrsCognitoJWTSecurityContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/security/username");
        request.getRequestContext().setAuthorizer(new APIGatewayV2HTTPEvent.RequestContext.Authorizer());
        request.getRequestContext().getAuthorizer().setJwt(new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT());
        request.getRequestContext().getAuthorizer().getJwt().setClaims(new HashMap<>());
        request.getRequestContext().getAuthorizer().getJwt().getClaims().put("cognito:username", "Bill");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "Bill");
    }

    @Test
    public void testJaxrsIAMSecurityContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/security/username");
        request.getRequestContext().setAuthorizer(new APIGatewayV2HTTPEvent.RequestContext.Authorizer());
        request.getRequestContext().getAuthorizer().setIam(new APIGatewayV2HTTPEvent.RequestContext.IAM());
        request.getRequestContext().getAuthorizer().getIam().setUserId("Bill");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "Bill");
    }

    @Test
    public void testJaxrsCustomLambdaSecurityContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/security/username");
        request.getRequestContext().setAuthorizer(new APIGatewayV2HTTPEvent.RequestContext.Authorizer());
        request.getRequestContext().getAuthorizer().setLambda(new HashMap<>());
        request.getRequestContext().getAuthorizer().getLambda().put("principalId", "Bill");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "Bill");
    }

    @Test
    public void testServletCognitoJWTSecurityContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/servlet/security");
        request.getRequestContext().setAuthorizer(new APIGatewayV2HTTPEvent.RequestContext.Authorizer());
        request.getRequestContext().getAuthorizer().setJwt(new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT());
        request.getRequestContext().getAuthorizer().getJwt().setClaims(new HashMap<>());
        request.getRequestContext().getAuthorizer().getJwt().getClaims().put("cognito:username", "Bill");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "Bill");
    }

    @Test
    public void testVertxCognitoJWTSecurityContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/vertx/security");
        request.getRequestContext().setAuthorizer(new APIGatewayV2HTTPEvent.RequestContext.Authorizer());
        request.getRequestContext().getAuthorizer().setJwt(new APIGatewayV2HTTPEvent.RequestContext.Authorizer.JWT());
        request.getRequestContext().getAuthorizer().getJwt().setClaims(new HashMap<>());
        request.getRequestContext().getAuthorizer().getJwt().getClaims().put("cognito:username", "Bill");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "Bill");
    }

    @Test
    public void testGetText() throws Exception {
        testGetText("/vertx/hello");
        testGetText("/servlet/hello");
        testGetText("/hello");
    }

    @Test
    public void testSwaggerUi() throws Exception {
        // this tests the FileRegion support in the handler
        APIGatewayV2HTTPEvent request = request("/q/swagger-ui/");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertTrue(body(out).contains("OpenAPI UI"));

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
        testPostText("/servlet/hello");
        testPostText("/vertx/hello");
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

    @ParameterizedTest
    @ValueSource(strings = { "/funqy", "/funqyAsync" })
    public void testFunqy(String path) {
        APIGatewayV2HTTPEvent request = request(path);
        request.getRequestContext().getHttp().setMethod("POST");
        request.setHeaders(new HashMap<>());
        request.getHeaders().put("Content-Type", MediaType.APPLICATION_JSON);
        request.setBody("\"Bill\"");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), "\"Make it funqy Bill\"");
        Assertions.assertTrue(out.getHeaders().get("Content-Type").startsWith("application/json"));
    }

    @Test
    public void testProxyRequestContext() throws Exception {
        APIGatewayV2HTTPEvent request = request("/hello/proxyRequestContext");
        APIGatewayV2HTTPResponse out = LambdaClient.invoke(APIGatewayV2HTTPResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 204);
    }

}
