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
        RestAssured.get(getPrefix() + "/q/health")
                .then().statusCode(200)
                .body(Matchers.containsString("UP"));

        RestAssured.get("/q/health")
                .then().statusCode(404);
    }

    @Test
    void verifyThatMetricsAreExposedOnManagementInterface() {
        RestAssured.get(getPrefix() + "/q/metrics")
                .then().statusCode(200)
                .body(Matchers.containsString("http_server_bytes_read"));

        RestAssured.get("/q/metrics")
                .then().statusCode(404);
    }

}