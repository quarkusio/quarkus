package io.quarkus.it.micrometer.prometheus;

import static io.restassured.RestAssured.get;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClient;

@QuarkusTest
@TestProfile(AdditionalHttpMethodsProfile.class)
public class AdditionalHttpMethodsTest {

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    void testConfiguredAdditionalMethodIsPreserved() throws Exception {
        WebClient client = WebClient.create(vertx);

        CountDownLatch latch = new CountDownLatch(1);
        client.request(HttpMethod.valueOf("PROPFIND"), url.getPort(), url.getHost(), "/message")
                .send()
                .onComplete(ar -> latch.countDown());
        latch.await(5, SECONDS);
        client.close();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/q/metrics").then().extract().asString();
            assertTrue(body.contains("method=\"PROPFIND\""),
                    "Configured additional HTTP method PROPFIND should be preserved in metrics");
        });
    }

    @Test
    void testUnconfiguredMethodStillBecomesUnknown() throws Exception {
        WebClient client = WebClient.create(vertx);

        CountDownLatch latch = new CountDownLatch(1);
        client.request(HttpMethod.valueOf("FOOBAR"), url.getPort(), url.getHost(), "/message")
                .send()
                .onComplete(ar -> latch.countDown());
        latch.await(5, SECONDS);
        client.close();

        await().atMost(5, SECONDS).untilAsserted(() -> {
            String body = get("/q/metrics").then().extract().asString();
            assertFalse(body.contains("method=\"FOOBAR\""),
                    "Non-configured HTTP method FOOBAR should not appear in metrics");
        });
    }
}
