package io.quarkus.it.micrometer.prometheus;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

@QuarkusTest
public class ConnectionResetTagsTest {

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    void testConnectionResetIncludesAllContributorTags() throws Exception {
        WebClient client = WebClient.create(vertx);

        client.get(url.getPort(), url.getHost(), "/slow").send();

        // wait a bit for the request to reach the server
        Thread.sleep(500);

        // close the client to trigger a connection reset
        client.close();

        // wait a bit for Vert.x to process the reset
        Thread.sleep(500);

        String metricMatch = "http_server_requests_seconds_count{dummy=\"value\"," +
                "env=\"test\",env2=\"test\",foo=\"UNSET\",foo_response=\"UNSET\",method=\"GET\"," +
                "outcome=\"CLIENT_ERROR\",registry=\"prometheus\",status=\"RESET\"," +
                "uri=\"/slow\"}";

        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/q/metrics").then().extract().asString();
            assertTrue(body.contains(metricMatch),
                    "Expected metric with all contributor tags not found. Looking for:\n" + metricMatch);
        });
    }
}
