package io.quarkus.grpc.examples.stork;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

class GrpcStorkResponseTimeCollectionTestBase {

    @Test
    public void shouldCallConfigurableIfFaster() {
        given().body("0")
                .when().post("/test/delay")
                .then().statusCode(200);
        List<String> responses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Response response = get("/test/unary/1");
            response.then().statusCode(200);
            responses.add(response.asString());
        }

        assertThat(responses.stream().filter(r -> r.equals("moderately-slow")))
                .hasSizeLessThan(5);
        assertThat(responses.stream().filter(r -> r.equals("configurable")))
                .hasSizeGreaterThan(5);
    }

    @Test
    public void shouldCallModerateIfFaster() {
        given().body("1000")
                .when().post("/test/delay")
                .then().statusCode(200);
        List<String> responses = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Response response = get("/test/unary/2");
            response.then().statusCode(200);
            responses.add(response.asString());
        }

        assertThat(responses.stream().filter(r -> r.equals("moderately-slow")))
                .hasSizeGreaterThan(5);
        assertThat(responses.stream().filter(r -> r.equals("configurable")))
                .hasSizeLessThan(5);
    }
}
