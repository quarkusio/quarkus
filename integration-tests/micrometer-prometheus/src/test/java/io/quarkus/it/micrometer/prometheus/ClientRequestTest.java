package io.quarkus.it.micrometer.prometheus;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Disabled("flaky")
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
        when().get("/client/status").then().statusCode(200)
                .body(containsString("ok400500timeout"));

        when().get("/q/metrics").then()
                .statusCode(200)
                .body(containsString(
                        "http_client_requests_seconds_count{clientName=\"localhost\",env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/pong/{message}\""))
                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/ping/{message}\""))
                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/pong/{message}\""))
                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/async-ping/{message}\""))
                // local client/server request for status code 200
                .body(containsString(
                        "http_server_requests_seconds_count{env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/status\""))
                .body(containsString(
                        "http_client_requests_seconds_count{clientName=\"localhost\",env=\"test\",method=\"GET\",outcome=\"SUCCESS\",registry=\"prometheus\",status=\"200\",uri=\"/client/status/{statusCode}\""))
                // local client/server request for status code 400
                .body(containsString(
                        "http_server_requests_seconds_max{env=\"test\",method=\"GET\",outcome=\"CLIENT_ERROR\",registry=\"prometheus\",status=\"400\",uri=\"/client/status/{statusCode}\""))
                .body(containsString(
                        "http_client_requests_seconds_count{clientName=\"localhost\",env=\"test\",method=\"GET\",outcome=\"CLIENT_ERROR\",registry=\"prometheus\",status=\"400\",uri=\"/client/status/{statusCode}\""))
                // local client/server request for status code 500
                .body(containsString(
                        "http_server_requests_seconds_max{env=\"test\",method=\"GET\",outcome=\"SERVER_ERROR\",registry=\"prometheus\",status=\"500\",uri=\"/client/status/{statusCode}\""))
                .body(containsString(
                        "http_client_requests_seconds_count{clientName=\"localhost\",env=\"test\",method=\"GET\",outcome=\"SERVER_ERROR\",registry=\"prometheus\",status=\"500\",uri=\"/client/status/{statusCode}\""));
    }
}
