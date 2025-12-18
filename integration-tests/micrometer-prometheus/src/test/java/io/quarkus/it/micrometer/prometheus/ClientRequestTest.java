package io.quarkus.it.micrometer.prometheus;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        when().get("/client/status").then().statusCode(200)
                .body(containsString("ok400500timeout"));

        await().atMost(5, SECONDS).untilAsserted(() -> assertThat(
                get("/server-requests/count").body().as(Integer.class),
                greaterThan(7)));

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "SUCCESS")
                        .queryParam("status", "200")
                        .queryParam("uri", "/client/pong/{message}")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=200, uri=/client/pong/{message}");

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "SUCCESS")
                        .queryParam("status", "200")
                        .queryParam("uri", "/client/ping/{message}")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=200, uri=/client/ping/{message}");

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "SUCCESS")
                        .queryParam("status", "200")
                        .queryParam("uri", "/client/pong/{message}")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=200, uri=/client/pong/{message}");

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "SUCCESS")
                        .queryParam("status", "200")
                        .queryParam("uri", "/client/async-ping/{message}")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=200, uri=/client/async-ping/{message}");

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "SUCCESS")
                        .queryParam("status", "200")
                        .queryParam("uri", "/client/status")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=200, uri=/client/status");

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "SUCCESS")
                        .queryParam("status", "200")
                        .queryParam("uri", "/client/status/{statusCode}")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=200, uri=/client/status/{statusCode}");

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "CLIENT_ERROR")
                        .queryParam("status", "400")
                        .queryParam("uri", "/client/status/{statusCode}")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=400, uri=/client/status/{statusCode}");

        assertEquals(1,
                given()
                        .queryParam("method", "GET")
                        .queryParam("outcome", "SERVER_ERROR")
                        .queryParam("status", "500")
                        .queryParam("uri", "/client/status/{statusCode}")
                        .when().get("/server-requests/count")
                        .body().as(Integer.class),
                "Expected: http.server.requests, method=GET, status=500, uri=/client/status/{statusCode}");

        assertEquals(5,
                given()
                        .queryParam("clientName", "pingpong")
                        .when().get("/server-requests/client/count")
                        .body().as(Integer.class),
                "Expected: http.client.requests, clientName=pingpong");
    }
}
