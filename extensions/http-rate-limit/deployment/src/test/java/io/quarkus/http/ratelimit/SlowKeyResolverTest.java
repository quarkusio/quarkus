package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;

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

/**
 * A key resolver that never answers costs a request the fixed 250 ms bound on key resolution, and the request is then
 * keyed by its client address, with one WARN a minute.
 */
public class SlowKeyResolverTest {

    private static final String RULE = "quarkus.http.rate-limit.slow.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, NeverAnswers.class))
            .overrideConfigKey(RULE + "paths", "/slow/*")
            .overrideConfigKey(RULE + "phase", "pre-authentication")
            .overrideConfigKey(RULE + "request-key", "custom:never")
            .overrideRuntimeConfigKey(RULE + "permits", "1")
            .overrideRuntimeConfigKey(RULE + "window", "1m")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getMessage().contains("key resolution"))
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.getLevel()).isEqualTo(Level.WARNING);
                        assertThat(String.format(r.getMessage(), r.getParameters()))
                                .isEqualTo("Rate limit rule 'slow' key resolution took longer than 250ms; the"
                                        + " request is keyed by its client address");
                    }));

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
    void requestIsKeyedByItsAddressAfter250Ms() {
        for (int i = 0; i < 3; i++) {
            long start = System.nanoTime();
            // the limit of 1 applies to the client address
            assertThat(http.get("/slow/x").statusCode()).isEqualTo(i == 0 ? 200 : 429);
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isBetween(Duration.ofMillis(250),
                    Duration.ofMillis(750));
        }
    }

    @Singleton
    @Identifier("never")
    public static class NeverAnswers implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return Uni.createFrom().nothing();
        }
    }
}
