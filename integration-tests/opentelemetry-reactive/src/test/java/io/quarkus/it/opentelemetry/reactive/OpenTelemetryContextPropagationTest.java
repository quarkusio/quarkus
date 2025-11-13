package io.quarkus.it.opentelemetry.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

@TestProfile(OpenTelemetryContextPropagationTest.OtelContextPropagationTestProfile.class)
@QuarkusTest
public class OpenTelemetryContextPropagationTest {

    /**
     * This message is logged by
     * {@link io.quarkus.opentelemetry.runtime.QuarkusContextStorage#attach(Context, io.opentelemetry.context.Context)}
     * when the context to attach is not the expected OpenTelemetry context.
     */
    private static final String UNEXPECTED_OTEL_CONTEXT = "Context in storage not the expected context";

    @TestHTTPResource("baggage")
    URI baggageUri;

    /**
     * This method tests and verifies <a href="https://github.com/quarkusio/quarkus/issues/49468">issue #49468</a>.
     * If the issue wasn't fixed, presence of the Vert.x HTTP authentication handler will cause
     * the {@link io.quarkus.opentelemetry.runtime.propagation.OpenTelemetryMpContextPropagationProvider}
     * to set the incorrect OpenTelemetry context while Quarkus REST resource method is executed.
     */
    @Test
    public void testContextPropagationInConcurrentRequests() throws InterruptedException {
        // if this test turns out too heavy for our CI resources, we can lower the number of requests;
        // whether the original issue was reproduced highly depends on number of requests and the executor machine
        // for example on my laptop, the missing baggage value could only be always reproduced with the 100_000 requests
        // however the 'Context in storage not the expected context' is much easier to reproduce and 10_000 requests
        // is enough on my laptop most of the time; if this test becomes flaky, consider re-test it with increased
        // number of requests it should make failures more frequent; nevertheless, if this test is re-run many times,
        // it reproduces the "missing baggage value" issue as well, just less often
        int numOfRequests = 10_000;
        AtomicReference<Throwable> throwableReference = new AtomicReference<>();
        CountDownLatch messageLatch = new CountDownLatch(numOfRequests);

        Vertx vertx = Vertx.vertx();
        HttpClient httpClient = vertx.createHttpClient();
        RequestOptions options = new RequestOptions()
                .setHost(baggageUri.getHost())
                .setSsl(false)
                .setMethod(HttpMethod.GET)
                .setURI("/baggage/build")
                .setPort(baggageUri.getPort());
        try {
            for (int i = 0; i < numOfRequests; i++) {
                httpClient
                        .request(options)
                        .map(HttpClientRequest::send)
                        .onComplete(ar -> {
                            if (ar.succeeded()) {
                                ar.result().onComplete(ar1 -> {
                                    if (ar1.succeeded()) {
                                        if (ar1.result().statusCode() == 200) {
                                            messageLatch.countDown();
                                        } else {
                                            throwableReference.set(new RuntimeException(
                                                    "Unexpected response status code " + ar1.result().statusCode()
                                                            + " from server: " + ar1.result().statusMessage()));
                                        }
                                    } else {
                                        throwableReference.set(ar1.cause());
                                    }
                                });
                            } else {
                                throwableReference.set(ar.cause());
                            }
                        });
            }

            assertTrue(messageLatch.await(30, TimeUnit.SECONDS),
                    () -> "Latch timed out, encountered failure was: " + throwableReference.get());
            assertThat(throwableReference.get()).isNull();
            verifyLogs();
        } finally {
            Future.join(httpClient.close(), vertx.close());
        }
    }

    private static void verifyLogs() {
        Path quarkusLogFile = Paths.get(".", "target").resolve("quarkus.log");
        assertTrue(quarkusLogFile.toFile().exists(), "Log file " + quarkusLogFile + " does not exist");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(quarkusLogFile)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(UNEXPECTED_OTEL_CONTEXT)) {
                    Assertions.fail("Log should not contain message: " + line);
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class OtelContextPropagationTestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.log.file.enabled", "true");
        }
    }
}
