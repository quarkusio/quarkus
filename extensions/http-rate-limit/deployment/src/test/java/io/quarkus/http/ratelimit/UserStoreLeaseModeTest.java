package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.runtime.internal.gcra.Gcra;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

/**
 * Lease mode over a user store that implements nothing but the documented {@link RateLimitStore} contract: the same
 * 8 admissions and 429 as {@link LeaseModeTest}, and breaker recovery through the zero-cost probe.
 */
public class UserStoreLeaseModeTest {

    private static final String STORE = "quarkus.http.rate-limit.store-";
    private static final String A = "quarkus.http.rate-limit.rules.a.";
    private static final String B = "quarkus.http.rate-limit.rules.b.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class, MinimalStore.class,
                            SwitchableStoreApp.class, SwitchableStoreApp.TunedBreaker.class))
            .overrideConfigKey(A + "paths", "/a/*")
            .overrideConfigKey(A + "phase", "pre-authentication")
            .overrideConfigKey(A + "request-key", "ip")
            .overrideRuntimeConfigKey(A + "permits", "8")
            .overrideRuntimeConfigKey(A + "window", "1m")
            .overrideConfigKey(A + "store", "mine")
            .overrideRuntimeConfigKey(A + "lease-size", "4")
            .overrideConfigKey(B + "paths", "/b/*")
            .overrideConfigKey(B + "phase", "pre-authentication")
            .overrideConfigKey(B + "request-key", "ip")
            .overrideRuntimeConfigKey(B + "permits", "100")
            .overrideRuntimeConfigKey(B + "window", "1m")
            .overrideConfigKey(B + "store", "mine")
            .overrideRuntimeConfigKey(B + "lease-size", "10")
            .overrideRuntimeConfigKey(STORE + "failure-mode", "open");

    /**
     * A map of theoretical arrival times and the GCRA arithmetic: the whole documented contract, including the
     * zero-cost probe (which {@link Gcra#tryConsume} already treats as non-mutating).
     */
    @Singleton
    @Identifier("mine")
    public static class MinimalStore implements RateLimitStore {

        public static final AtomicBoolean throwing = new AtomicBoolean();
        public static final AtomicInteger probes = new AtomicInteger();

        private final ConcurrentMap<String, Long> tats = new ConcurrentHashMap<>();

        @Override
        public Uni<RateLimitDecision> tryConsume(String key, long cost, RateLimitPolicy policy) {
            if (cost < 0 || cost > policy.burst()) {
                throw new IllegalArgumentException("cost " + cost);
            }
            if (cost == 0) {
                probes.incrementAndGet();
            }
            if (throwing.get()) {
                return Uni.createFrom().failure(new IllegalStateException("down"));
            }
            long now = System.nanoTime();
            long t = policy.emissionIntervalNanos();
            long tau = policy.burstToleranceNanos();
            Consume consume = new Consume(now, cost, t, tau);
            tats.compute(policy.id() + ":" + key, consume);
            Gcra.Outcome o = consume.outcome;
            return Uni.createFrom().item(o.allowed()
                    ? RateLimitDecision.allowed(o.remaining())
                    : RateLimitDecision.rejected(Duration.ofNanos(o.retryAfterNanos())));
        }

        /**
         * Decides one call atomically for its key, and keeps the outcome for the caller.
         */
        static final class Consume implements BiFunction<String, Long, Long> {

            final long now;
            final long cost;
            final long t;
            final long tau;
            Gcra.Outcome outcome;

            Consume(long now, long cost, long t, long tau) {
                this.now = now;
                this.cost = cost;
                this.t = t;
                this.tau = tau;
            }

            @Override
            public Long apply(String k, Long tat) {
                outcome = Gcra.tryConsume(tat == null ? Gcra.NO_STATE : tat, now, cost, t, tau);
                // a probe and a rejection write nothing
                return cost > 0 && outcome.allowed() ? outcome.newTat() : tat;
            }
        }
    }

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
    void theDocumentedContractIsEnough() {
        for (int i = 1; i <= 8; i++) {
            assertThat(http.get("/a/x").statusCode()).as("request %d", i)
                    .isEqualTo(200);
        }
        HttpResponse<Buffer> ninth = http.get("/a/x");
        assertThat(ninth.statusCode()).isEqualTo(429);
        assertThat(Integer.parseInt(ninth.getHeader("Retry-After"))).isBetween(1, 8);

        // the store goes down: claims fail open, the breaker opens, the probe closes it again
        MinimalStore.throwing.set(true);
        for (int i = 0; i < 6; i++) {
            assertThat(http.get("/b/x").statusCode()).isEqualTo(200);
        }
        int probesBefore = MinimalStore.probes.get();
        MinimalStore.throwing.set(false);
        await().atMost(Duration.ofSeconds(6)).untilAsserted(
                () -> assertThat(MinimalStore.probes.get()).isGreaterThan(probesBefore));
        // the probe reached the store, and requests are admitted
        await().atMost(Duration.ofSeconds(6)).untilAsserted(
                () -> assertThat(http.get("/b/x").statusCode()).isEqualTo(200));
    }
}
