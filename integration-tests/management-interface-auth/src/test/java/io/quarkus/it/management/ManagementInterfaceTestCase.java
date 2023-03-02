package io.quarkus.it.management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class ManagementInterfaceTestCase {

    protected String getPrefix() {
        return "http://localhost:9001";
    }

    @Test
    void verifyThatHealthChecksAreExposedOnManagementInterface() {
        RestAssured.given().auth().preemptive().basic("john", "john").get(getPrefix() + "/q/health")
                .then().statusCode(401);

        RestAssured.given().auth().preemptive().basic("bob", "bob").get(getPrefix() + "/q/health")
                .then().statusCode(403);

        RestAssured.given().auth().basic("alice", "alice").get(getPrefix() + "/q/health")
                .then().statusCode(200)
                .body(Matchers.containsString("UP"));

        RestAssured.given().auth().basic("alice", "alice").get(getPrefix() + "/q/health/ready")
                .then().statusCode(200)
                .body(Matchers.containsString("UP"));

        RestAssured.get("/q/health")
                .then().statusCode(404);
    }

    @Test
    void verifyThatMetricsAreExposedOnManagementInterface() {
        RestAssured.given().auth().basic("alice", "alice").get(getPrefix() + "/q/metrics")
                .then().statusCode(200)
                .body(Matchers.containsString("http_server_bytes_read"));
        RestAssured.given().auth().basic("bob", "bob").get(getPrefix() + "/q/metrics")
                .then().statusCode(200)
                .body(Matchers.containsString("http_server_bytes_read"));
        RestAssured.given().auth().basic("john", "john").get(getPrefix() + "/q/metrics")
                .then().statusCode(401);

        RestAssured.get("/q/metrics")
                .then().statusCode(404);
    }

    @Test
    void verifyMainEndpoint() {
        RestAssured.get("/service/hello").then().statusCode(200)
                .body(Matchers.equalTo("hello"));

        RestAssured.given().auth().preemptive().basic("john", "john").get("/service/goodbye")
                .then().statusCode(401);

        RestAssured.given().auth().preemptive().basic("alice", "alice").get("/service/goodbye")
                .then().statusCode(403);

        RestAssured.given().auth().basic("bob", "bob").get("/service/goodbye")
                .then().statusCode(200)
                .body(Matchers.equalTo("goodbye"));
    }
}
