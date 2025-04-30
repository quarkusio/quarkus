package io.quarkus.it.amazon.lambda.rest.servlet;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.http.model.ApiGatewayRequestIdentity;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyRequestContext;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaApi;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaV1SimpleTestCase {

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
    public void testGetText() throws Exception {
        testGetTextByEvent("/servlet/hello");
        testGetText("/servlet/hello");
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

    public void testPostText() throws Exception {
        testPostTextByEvent("/servlet/hello");
        testPostText("/servlet/hello");
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

}
