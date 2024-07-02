package io.quarkus.grpc.examples.hello;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;

import examples.HelloRequest;
import io.restassured.http.ContentType;

class HelloWorldTranscodingEndpointTestBase {

    @Test
    public void testSimplePath() {
        given()
                .body(getJsonRequest("simple-test"))
                .contentType(ContentType.JSON)
                .when().post("/v2/simple")
                .then()
                .statusCode(200)
                .body("message", is("Hello from Simple Path, simple-test!"));
    }

    @Test
    public void testComplexPath() {
        given()
                .contentType(ContentType.JSON)
                .when().post("/v2/complex/complex-test/path")
                .then()
                .statusCode(200)
                .body("message", is("Hello from Complex Path, complex-test!"));
    }

    private String getJsonRequest(String name) {
        try {
            return JsonFormat.printer().omittingInsignificantWhitespace()
                    .print(HelloRequest.newBuilder().setName(name).build());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
