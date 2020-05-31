package ${package};

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.AwsProxyResponse;
import io.quarkus.amazon.lambda.http.model.Headers;
import io.quarkus.amazon.lambda.test.LambdaClient;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Assertions;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
public class GreetingTest {
    @Test
    public void testJaxrs() {
        RestAssured.when().get("/hello").then()
                .contentType("text/plain")
                .body(equalTo("hello jaxrs"));
    }

    @Test
    public void testServlet() {
        RestAssured.when().get("/servlet/hello").then()
                .contentType("text/plain")
                .body(equalTo("hello servlet"));
    }

    @Test
    public void testVertx() {
        RestAssured.when().get("/vertx/hello").then()
                .contentType("text/plain")
                .body(equalTo("hello vertx"));
    }

    @Test
    public void testFunqy() {
        RestAssured.when().get("/funqyHello").then()
                .contentType("application/json")
                .body(equalTo("\"hello funqy\""));
    }

    @Test
    public void testGetTextJaxrs() throws Exception {
        testGetText("/hello", "text/plain","hello jaxrs");
    }

    @Test
    public void testGetTextServelet() throws Exception {
        testGetText("/servlet/hello","text/plain", "hello servlet");
    }

    @Test
    public void testGetTextVertx() throws Exception {
        testGetText("/vertx/hello","text/plain", "hello vertx");
    }

    @Test
    public void testGetTextFunqy() throws Exception {
        testGetText("/funqyHello","application/json", "\"hello funqy\"");
    }

    private void testGetText(String path, String contentType, String bodyText) {
        AwsProxyRequest request = new AwsProxyRequest();
        request.setHttpMethod("GET");
        request.setPath(path);
        AwsProxyResponse out = LambdaClient.invoke(AwsProxyResponse.class, request);
        Assertions.assertEquals(out.getStatusCode(), 200);
        Assertions.assertEquals(body(out), bodyText);
        Assertions.assertTrue(out.getMultiValueHeaders().getFirst("Content-Type").startsWith(contentType));
    }

    private String body(AwsProxyResponse response) {
        if (!response.isBase64Encoded())
            return response.getBody();
        return new String(Base64.decodeBase64(response.getBody()));
    }

}
