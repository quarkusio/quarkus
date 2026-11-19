package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;
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
import io.vertx.core.Context;
import io.vertx.core.Vertx;

/**
 * Key resolvers, limit resolvers and stores run on the event loop. Either resolver may still complete on a thread of
 * its own, for example a client library's: the next stage runs on the request's context all the same.
 */
public class ForeignThreadCompletionTest {

    private static final String RULE = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    ForeignResolver.class, ForeignLimitResolver.class, RecordingStore.class))
            .overrideConfigKey(RULE + "paths", "/api/*")
            .overrideConfigKey(RULE + "phase", "pre-authentication")
            .overrideConfigKey(RULE + "request-key", "custom:foreign")
            .overrideConfigKey(RULE + "limit-resolver", "foreign")
            .overrideConfigKey(RULE + "store", "recording");

    static final AtomicReference<String> overrideRanOn = new AtomicReference<>();
    static final AtomicReference<String> storeRanOn = new AtomicReference<>();

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
    void theOverrideAndTheStoreRunOnTheRequestContext() {
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        assertThat(overrideRanOn.get()).isEqualTo("event loop");
        assertThat(storeRanOn.get()).isEqualTo("event loop");
    }

    /**
     * @return where the caller runs: on an event loop with a Vert.x context, or on which other thread
     */
    static String where() {
        return Vertx.currentContext() != null && Context.isOnEventLoopThread() ? "event loop"
                : Thread.currentThread().getName();
    }

    /**
     * @return an item emitted from a thread of its own, as a client library may complete
     */
    static <T> Uni<T> fromForeignThread(String name, T item) {
        return Uni.createFrom().emitter(em -> new Thread(() -> em.complete(item), name).start());
    }

    @Singleton
    @Identifier("foreign")
    public static class ForeignResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return fromForeignThread("foreign-resolver", Optional.of("k"));
        }
    }

    @Singleton
    @Identifier("foreign")
    public static class ForeignLimitResolver implements RateLimitResolver {

        @Override
        public Uni<RateLimit> resolve(RateLimitContext context) {
            overrideRanOn.set(where());
            return fromForeignThread("foreign-limit", RateLimit.of(10, Duration.ofMinutes(1)));
        }
    }

    @Singleton
    @Identifier("recording")
    public static class RecordingStore implements RateLimitStore {

        @Override
        public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
            if (cost > 0) {
                storeRanOn.set(where());
            }
            return Uni.createFrom().item(RateLimitDecision.allowed(policy.burst() - cost));
        }
    }
}
