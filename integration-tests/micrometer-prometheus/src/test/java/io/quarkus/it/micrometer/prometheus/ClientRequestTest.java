package io.quarkus.it.micrometer.prometheus;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ClientRequestTest {
    @Test
    void testClientRequests() {
        when().get("/client/ping/one").then().statusCode(200)
                .body(containsString("one"));
        when().get("/client/ping/two").then().statusCode(200)
                .body(containsString("two"));
        when().get("/client/async-ping/one").then().statusCode(200)
                .body(containsString("one"));
        when().get("/client/async-ping/two").then().statusCode(200)
                .body(containsString("two"));

        when().get("/q/metrics").then().statusCode(200)
                .body(containsString(
                        "http_client_requests_seconds_count{clientName=\"localhost\",env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/pong/{message}\""))
                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/ping/{message}\""))
                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/pong/{message}\""))
                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/async-ping/{message}\""));
    }
}
