package io.quarkus.grpc.examples.hello;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import examples.HelloRequest;
import examples.UpdateRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class HelloWorldTranscodingTest {

    @Test
    public void testSimplePath() {
        given()
                .body(getJsonRequest("simple-test"))
                .contentType(ContentType.JSON)
                .when().post("/v1/simple")
                .then()
                .statusCode(200)
                .body("message", is("Hello from Simple Path, simple-test!"));
    }

    @Test
    public void testComplexPath() {
        given()
                .body(getJsonRequest("complex-test"))
                .contentType(ContentType.JSON)
                .when().post("/v1/complex/complex-test/path")
                .then()
                .statusCode(200)
                .body("message", is("Hello from Complex Path, complex-test!"));
    }

    @Test
    public void testResourceLookup() {
        given()
                .contentType(ContentType.JSON)
                .when().get("/v1/resources/resource-type-1/resource/1234")
                .then()
                .statusCode(200)
                .body("message", is("Resource details: type='resource-type-1', id='1234'"));
    }

    @Test
    public void testNestedResourceLookup() {
        given()
                .contentType(ContentType.JSON)
                .when().get("/v1/resources/update/1234")
                .then()
                .statusCode(200)
                .body("message", is("Greeting with id '1234' updated with nested resource details: name='update'"));
    }

    @Test
    public void testSearchGreetings() {
        given()
                .param("query", "test-query")
                .contentType(ContentType.JSON)
                .when().get("/v1/greetings")
                .then()
                .statusCode(200)
                .body("message", is("Matching greetings for your query: 'test-query'"));
    }

    @Test
    public void testUpdateGreeting() {
        given()
                .body(getUpdateRequest("5678"))
                .contentType(ContentType.JSON)
                .when().put("/v1/greetings/update")
                .then()
                .statusCode(200)
                .body("message", is("Greeting with id '5678' updated!"));
    }

    @Test
    public void testInvalidPath() {
        given()
                .contentType(ContentType.JSON)
                .when().get("/v1/invalid")
                .then()
                .statusCode(404);
    }

    @Test
    public void testInvalidMethod() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/v1/resources/resource-type-1/resource/1234")
                .then()
                .statusCode(404);
    }

    @Test
    public void testInvalidRequest() {
        given()
                .body("invalid")
                .contentType(ContentType.JSON)
                .when().post("/v1/simple")
                .then()
                .statusCode(422);
    }

    @Test
    public void testInvalidContentType() {
        given()
                .body(getJsonRequest("simple-test"))
                .contentType(ContentType.TEXT)
                .when().post("/v1/simple")
                .then()
                .statusCode(404);
    }

    private String getJsonRequest(String name) {
        try {
            return JsonFormat.printer().omittingInsignificantWhitespace()
                    .print(HelloRequest.newBuilder().setName(name).build());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUpdateRequest(String greetingId) {
        try {
            UpdateRequest updateRequest = UpdateRequest.newBuilder()
                    .setGreetingId(greetingId)
                    .setUpdatedContent(HelloRequest.newBuilder().setName("Updated Name").build())
                    .build();
            return JsonFormat.printer().omittingInsignificantWhitespace().print(updateRequest);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
