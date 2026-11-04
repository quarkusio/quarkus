package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import jdk.jfr.Recording;
import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

public class JfrEventsTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";
    private static final String REJECTED = "quarkus.RateLimitRejected";
    private static final String EVALUATION = "quarkus.RateLimitEvaluation";
    private static final Duration DELAY = Duration.ofMillis(50);

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, SlowStore.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PREFIX + "api.paths", "/limited/*")
            .overrideConfigKey(PREFIX + "api.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "api.request-key", "path")
            .overrideRuntimeConfigKey(PREFIX + "api.permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "api.window", "1m")
            .overrideConfigKey(PREFIX + "slow.paths", "/slow/*")
            .overrideConfigKey(PREFIX + "slow.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "slow.store", "slow")
            .overrideConfigKey(PREFIX + "slow.request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "slow.permits", "100")
            .overrideRuntimeConfigKey(PREFIX + "slow.window", "1m")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.store-timeout", "5s");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void rejectionRecordsOneEventWithoutKeyOrAddress() throws IOException {
        AtomicReference<String> retryAfter = new AtomicReference<>();
        List<RecordedEvent> events = record(Duration.ZERO, () -> {
            assertThat(get("/limited/a").statusCode()).isEqualTo(200);
            // a matrix parameter does not change the path the rule matches, nor the one the event records
            HttpResponse<Buffer> rejected = get("/limited/a;x=1");
            assertThat(rejected.statusCode()).isEqualTo(429);
            retryAfter.set(rejected.getHeader("Retry-After"));
        });

        List<RecordedEvent> rejections = named(events, REJECTED);
        assertThat(rejections).hasSize(1);
        RecordedEvent rejection = rejections.getFirst();
        assertThat(rejection.getString("rule")).isEqualTo("api");
        assertThat(rejection.getString("phase")).isEqualTo("pre-authentication");
        assertThat(rejection.getString("httpMethod")).isEqualTo("GET");
        assertThat(rejection.getString("path")).isEqualTo("/limited/a");
        assertThat(rejection.getDuration("retryAfter").toSeconds()).isEqualTo(Long.parseLong(retryAfter.get()));
        // keys can be addresses, API keys or token hashes, and recordings are shared
        assertThat(rejection.getFields()).extracting(ValueDescriptor::getName)
                .doesNotContain("key", "clientAddress", "address", "client", "principal");

        // both events of the rejected request carry the same request ID
        assertThat(rejection.getString("traceId")).isNotNull();
        List<RecordedEvent> evaluations = named(events, EVALUATION);
        assertThat(evaluations).hasSize(2);
        assertThat(evaluations).extracting(e -> e.getString("outcome")).containsExactly("ALLOWED", "REJECTED");
        RecordedEvent rejectedEvaluation = evaluations.get(1);
        assertThat(rejectedEvaluation.getString("traceId")).isEqualTo(rejection.getString("traceId"));
        assertThat(rejectedEvaluation.getString("spanId")).isEqualTo(rejection.getString("spanId"));
        assertThat(evaluations.getFirst().getString("traceId")).isNotNull()
                .isNotEqualTo(rejection.getString("traceId"));
        assertThat(rejectedEvaluation.getFields()).extracting(ValueDescriptor::getName)
                .doesNotContain("key", "clientAddress", "address", "client", "principal");
    }

    @Test
    void slowEvaluationIsRecordedWithTheDefaultThreshold() throws IOException {
        // the first local decisions of a cold JVM can exceed the threshold
        for (int i = 0; i < 20; i++) {
            assertThat(get("/limited/warm-up-" + i).statusCode()).isEqualTo(200);
        }
        List<RecordedEvent> events = record(null, () -> {
            assertThat(get("/slow/a").statusCode()).isEqualTo(200);
            // a local decision is far below the default threshold of 10 ms
            assertThat(get("/limited/b").statusCode()).isEqualTo(200);
        });

        List<RecordedEvent> evaluations = named(events, EVALUATION);
        assertThat(evaluations).hasSize(1);
        RecordedEvent evaluation = evaluations.getFirst();
        assertThat(evaluation.getString("rule")).isEqualTo("slow");
        assertThat(evaluation.getString("phase")).isEqualTo("pre-authentication");
        assertThat(evaluation.getString("store")).isEqualTo("slow");
        assertThat(evaluation.getString("outcome")).isEqualTo("ALLOWED");
        assertThat(evaluation.getDuration()).isGreaterThanOrEqualTo(DELAY);
        assertThat(evaluation.getString("traceId")).isNotNull();
    }

    @Test
    void fastEvaluationIsRecordedWithAZeroThreshold() throws IOException {
        List<RecordedEvent> events = record(Duration.ZERO,
                () -> assertThat(get("/limited/c").statusCode()).isEqualTo(200));

        List<RecordedEvent> evaluations = named(events, EVALUATION);
        assertThat(evaluations).hasSize(1);
        assertThat(evaluations.getFirst().getString("rule")).isEqualTo("api");
        assertThat(evaluations.getFirst().getString("store")).isEqualTo("local");
        assertThat(evaluations.getFirst().getString("outcome")).isEqualTo("ALLOWED");
        assertThat(named(events, REJECTED)).isEmpty();
    }

    /**
     * @param threshold the evaluation threshold, {@code null} for the event's default
     */
    private static List<RecordedEvent> record(Duration threshold, Runnable action)
            throws IOException {
        Path dump = Files.createTempFile("rate-limit", ".jfr");
        try {
            try (Recording recording = new Recording()) {
                recording.enable(REJECTED);
                if (threshold == null) {
                    recording.enable(EVALUATION);
                } else {
                    recording.enable(EVALUATION).withThreshold(threshold);
                }
                recording.start();
                action.run();
                recording.stop();
                recording.dump(dump);
            }
            return RecordingFile.readAllEvents(dump).stream()
                    .sorted(Comparator.comparing(RecordedEvent::getStartTime))
                    .toList();
        } finally {
            Files.deleteIfExists(dump);
        }
    }

    private static List<RecordedEvent> named(List<RecordedEvent> events, String name) {
        return events.stream().filter(e -> e.getEventType().getName().equals(name)).toList();
    }

    private HttpResponse<Buffer> get(String path) {
        return http.get(path);
    }

    /**
     * Admits every request after {@link #DELAY}.
     */
    @Singleton
    @Identifier("slow")
    public static class SlowStore implements RateLimitStore {

        @Override
        public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
            return Uni.createFrom().<RateLimitDecision> item(RateLimitDecision.allowed(policy.burst() - cost))
                    .onItem().delayIt().by(DELAY);
        }
    }
}
