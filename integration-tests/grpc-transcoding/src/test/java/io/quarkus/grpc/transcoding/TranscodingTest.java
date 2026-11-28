package io.quarkus.grpc.transcoding;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class TranscodingTest {

    @GrpcClient("transcoding-service")
    MutinyTranscodingServiceGrpc.MutinyTranscodingServiceStub client;

    @Test
    void testRegularGrpc() {
        EchoResponse response = client.getSimple(
                SinglePathRequest.newBuilder().setItemId("item-123").build())
                .await().indefinitely();
        assertThat(response.getItemId()).isEqualTo("item-123");
    }

    @Test
    void testSinglePathParam() {
        given()
                .contentType("application/json")
                .when().get("/v1/items/item-123")
                .then()
                .statusCode(200)
                .body("itemId", equalTo("item-123"));
    }

    @Test
    void testMultiplePathParams() {
        given()
                .contentType("application/json")
                .when().get("/v1/users/user-456/items/item-789")
                .then()
                .statusCode(200)
                .body("userId", equalTo("user-456"))
                .body("itemId", equalTo("item-789"));
    }

    @Test
    void testPathAndQueryParam() {
        given()
                .contentType("application/json")
                .queryParam("revision", 3)
                .when().get("/v1/items/item-123/revision")
                .then()
                .statusCode(200)
                .body("itemId", equalTo("item-123"))
                .body("revision", equalTo(3));
    }

    @Test
    void testMultiplePathAndQueryParams() {
        given()
                .contentType("application/json")
                .queryParam("color", "red")
                .queryParam("size", "L")
                .when().get("/v1/users/user-456/items/item-789/filter")
                .then()
                .statusCode(200)
                .body("userId", equalTo("user-456"))
                .body("itemId", equalTo("item-789"))
                .body("color", equalTo("red"))
                .body("size", equalTo("L"));
    }

    @Test
    void testPathAndBody() {
        given()
                .contentType("application/json")
                .body("{\"name\": \"Widget\", \"description\": \"A nice widget\"}")
                .when().post("/v1/items/item-123")
                .then()
                .statusCode(200)
                .body("itemId", equalTo("item-123"))
                .body("name", equalTo("Widget"))
                .body("description", equalTo("A nice widget"));
    }

    @Test
    void testPathQueryAndBody() {
        given()
                .contentType("application/json")
                .queryParam("filter", "new")
                .body("{\"name\": \"Widget\", \"description\": \"A nice widget\"}")
                .when().post("/v1/users/user-456/items/item-789")
                .then()
                .statusCode(200)
                .body("userId", equalTo("user-456"))
                .body("itemId", equalTo("item-789"))
                .body("filter", equalTo("new"))
                .body("name", equalTo("Widget"))
                .body("description", equalTo("A nice widget"));
    }

    @Test
    void testPathAndStarBody() {
        given()
                .contentType("application/json")
                .body("{\"name\": \"Widget\", \"description\": \"A nice widget\"}")
                .when().put("/v1/items/item-123/replace")
                .then()
                .statusCode(200)
                .body("itemId", equalTo("item-123"))
                .body("name", equalTo("Widget"))
                .body("description", equalTo("A nice widget"));
    }
}
