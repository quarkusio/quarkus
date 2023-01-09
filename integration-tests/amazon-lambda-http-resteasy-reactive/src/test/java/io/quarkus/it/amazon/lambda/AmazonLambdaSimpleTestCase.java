package io.quarkus.it.amazon.lambda;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

import java.util.Arrays;
import java.util.HashMap;

import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testContext() throws Exception {
        given()
                .when()
                .get("/hello/context")
                .then()
                .statusCode(204);
        given()
                .when()
                .get("/hello/inject-event")
                .then()
                .statusCode(204);
    }

    @Test
    public void testGetText() throws Exception {
        testGetTextByEvent("/hello");
        testGetText("/hello");
    }

    private void testGetTextByEvent(String path) {
        APIGatewayV2HTTPEvent request = request(path);
        given()
                .contentType("application/json")
                .accept("application/json")
                .body(request)
                .when()
                .post(AmazonLambdaApi.API_BASE_PATH_TEST)
                .then()
                .statusCode(200)
                .body("body", equalTo("hello"))
                .body("headers.Content-Type", containsString("text/plain"));
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
        given()
                .when()
                .get("/nowhere")
                .then()
                .statusCode(404);
    }

    @Test
    public void testPostText() throws Exception {
        testPostTextByEvent("/hello");
        testPostText("/hello");
    }

    private void testPostTextByEvent(String path) {
        APIGatewayV2HTTPEvent request = request(path);
        request.getRequestContext().getHttp().setMethod("POST");
        request.setHeaders(new HashMap<>());
        request.getHeaders().put("Content-Type", "text/plain");
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
                .body("headers.Content-Type", containsString("text/plain"));
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

    @Test
    public void testProxyRequestContext() throws Exception {
        given()
                .when()
                .get("/hello/proxyRequestContext")
                .then()
                .statusCode(204);
    }
}
