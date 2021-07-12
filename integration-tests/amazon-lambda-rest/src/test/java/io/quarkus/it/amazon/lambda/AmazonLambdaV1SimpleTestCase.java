package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import java.util.Arrays;

import javax.ws.rs.core.MediaType;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.amazon.lambda.http.model.ApiGatewayAuthorizerContext;
import io.quarkus.amazon.lambda.http.model.ApiGatewayRequestIdentity;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.CognitoAuthorizerClaims;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaV1SimpleTestCase {

    @Test
    public void testContext() throws Exception {
        given()
                .when()
                .get("/hello/context")
                .then()
                .statusCode(204);
    }

    @Test
    public void testJaxrsSecurityIAM() throws Exception {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath("/security/username");
        request.setRequestContext(new AwsProxyRequestContext());
        request.getRequestContext().setIdentity(new ApiGatewayRequestIdentity());
        request.getRequestContext().getIdentity().setUser("Bill");

        given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("body", equalTo("Bill"));
    }

    @Test
    public void testServletSecurityIAM() throws Exception {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath("/servlet/security");
        request.setRequestContext(new AwsProxyRequestContext());
        request.getRequestContext().setIdentity(new ApiGatewayRequestIdentity());
        request.getRequestContext().getIdentity().setUser("Bill");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("body", equalTo("Bill"));
    }

    @Test
    public void testJaxrsCognitoSecurityContext() throws Exception {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath("/security/username");
        request.setRequestContext(new AwsProxyRequestContext());
        request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
        request.getRequestContext().getAuthorizer().setClaims(new CognitoAuthorizerClaims());
        request.getRequestContext().getAuthorizer().getClaims().setUsername("Bill");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("body", equalTo("Bill"));
    }

    @Test
    public void testJaxrsCustomLambdaSecurityContext() throws Exception {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath("/security/username");
        request.setRequestContext(new AwsProxyRequestContext());
        request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
        request.getRequestContext().getAuthorizer().setPrincipalId("Bill");
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("body", equalTo("Bill"));
    }

    @Test
    public void testGetText() throws Exception {
        testGetTextByEvent("/vertx/hello");
        testGetTextByEvent("/servlet/hello");
        testGetTextByEvent("/hello");
        testGetText("/vertx/hello");
        testGetText("/servlet/hello");
        testGetText("/hello");
    }

    private void testGetTextByEvent(String path) {
        AwsProxyRequest request = request(path);
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("body", equalTo("hello"))
                .body("multiValueHeaders.Content-Type", hasItem(containsString("text/plain")));
    }

    private void testGetText(String path) {
        given()
                .when()
                .get(path)
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("text/plain"))
                .body(equalTo("hello"));
    }

    @Test
    public void testSwaggerUi() throws Exception {
        // this tests the FileRegion support in the handler
        given()
                .when()
                .get("/q/swagger-ui/")
                .then()
                .statusCode(200)
                .body(containsString("OpenAPI UI"));
    }

    private String body(AwsProxyResponse response) {
        if (!response.isBase64Encoded())
            return response.getBody();
        return new String(Base64.decodeBase64(response.getBody()));
    }

    private AwsProxyRequest request(String path) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath(path);
        return request;
    }

    @Test
    public void test404() throws Exception {
        given()
                .when()
                .get("/nowhere")
                .then()
                .statusCode(404);
    }

    @Test
    public void testPostText() throws Exception {
        testPostTextByEvent("/hello");
        testPostTextByEvent("/servlet/hello");
        testPostTextByEvent("/vertx/hello");
        testPostText("/hello");
        testPostText("/servlet/hello");
        testPostText("/vertx/hello");
    }

    private void testPostTextByEvent(String path) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("POST");
        request.setMultiValueHeaders(new Headers());
        request.getMultiValueHeaders().add("Content-Type", "text/plain");
        request.setPath(path);
        request.setBody("Bill");

        given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("body", equalTo("hello Bill"))
                .body("multiValueHeaders.Content-Type", hasItem(containsString("text/plain")));
    }

    private void testPostText(String path) {
        given()
                .contentType("text/plain")
                .body("Bill")
                .when()
                .post(path)
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("text/plain"))
                .body(equalTo("hello Bill"));
    }

    @Test
    public void testPostBinary() throws Exception {
        byte[] bytes = { 0, 1, 2, 3 };
        byte[] resBytes = { 4, 5, 6 };

        byte[] result = given()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes)
                .when()
                .post("hello")
                .then()
                .statusCode(200)
                .header("Content-Type", containsString(MediaType.APPLICATION_OCTET_STREAM))
                .extract().asByteArray();
        Assertions.assertTrue(Arrays.equals(resBytes, result));
    }

    @Test
    public void testPostEmpty() throws Exception {
        given()
                .when()
                .post("/hello/empty")
                .then()
                .statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/funqy", "/funqyAsync" })
    public void testFunqy(String path) {
        given()
                .contentType("application/json")
                .accept("application/json")
                .body("\"Bill\"")
                .when()
                .post(path)
                .then()
                .statusCode(200)
                .header("Content-Type", containsString("application/json"))
                .body(equalTo("\"Make it funqy Bill\""));
    }

    @Test
    public void testProxyRequestContext() throws Exception {
        given()
                .when()
                .get("/hello/proxyRequestContext")
                .then()
                .statusCode(204);
    }

}
