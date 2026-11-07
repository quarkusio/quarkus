package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Optional;

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

public class ResolverFailureTest {

    private static final String SKIP = "quarkus.http.rate-limit.lenient.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, FailingResolver.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(SKIP + "paths", "/lenient/*")
            .overrideConfigKey(SKIP + "phase", "pre-authentication")
            .overrideConfigKey(SKIP + "request-key", "custom:failing")
            .overrideRuntimeConfigKey(SKIP + "permits", "1")
            .overrideRuntimeConfigKey(SKIP + "window", "1m")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getMessage().contains("key resolution failed"))
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.getLevel()).isEqualTo(java.util.logging.Level.WARNING);
                        assertThat(String.format(r.getMessage(), r.getParameters()))
                                .isEqualTo("Rate limit rule 'lenient' key resolution failed; the request is keyed "
                                        + "by its client address");
                        assertThat(r.getThrown()).hasMessage("boom");
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

    /**
     * A resolver that fails, for example on input it cannot parse, must not switch its rule off: the request is keyed
     * by its client address, like a request without the header of a {@code header:} key.
     */
    @Test
    void failureFallsBackToTheClientAddressAndWarnsOnce() {
        assertThat(http.get("/lenient/x").statusCode()).isEqualTo(200);
        for (int i = 0; i < 3; i++) {
            assertThat(http.get("/lenient/x").statusCode()).isEqualTo(429);
        }
    }

    @Singleton
    @Identifier("failing")
    public static class FailingResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return Uni.createFrom().failure(new IllegalStateException("boom"));
        }
    }
}
