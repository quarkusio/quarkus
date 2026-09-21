package io.quarkus.load.shedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.quarkus.builder.Version;
import io.quarkus.load.shedding.runtime.HttpLoadShedding;
import io.quarkus.load.shedding.runtime.LoadSheddingMetrics;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class MetricsLoadSheddingTest {
    private static final int NUM_REQUESTS = 50;
    private static final int INITIAL_LIMIT = 5;
    private static final int MAX_LIMIT = 2 * INITIAL_LIMIT;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyResource.class, RequestCounter.class))
            .overrideConfigKey("quarkus.load-shedding.initial-limit", "" + INITIAL_LIMIT)
            .overrideConfigKey("quarkus.load-shedding.max-limit", "" + MAX_LIMIT)
            .overrideConfigKey("quarkus.load-shedding.priority.enabled", "false")
            // a single event loop handles all incoming requests one after another, so that
            // the overload check and the following increment of the request counter cannot
            // interleave and permit more requests than the limit allows
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-micrometer-deployment", Version.getVersion())));

    // counted down when a request reaches the server, no matter if it is permitted or rejected
    static final CountDownLatch arrived = new CountDownLatch(NUM_REQUESTS);

    // opened by the test once all requests arrived
    static final CountDownLatch release = new CountDownLatch(1);

    @Inject
    Vertx vertx;

    @Inject
    MeterRegistry registry;

    @TestHTTPResource
    URL url;

    // the composite registry the application publishes into has no backend of its own, so
    // there would be nothing to read the meters from
    @BeforeAll
    static void addSimpleRegistry() {
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    public void test() throws InterruptedException {
        assertThat(shedRequests()).isZero();
        assertThat(overloaded()).isZero();
        assertThat(currentRequests()).isZero();
        assertThat(currentLimit()).isEqualTo(INITIAL_LIMIT);

        CountDownLatch finished = new CountDownLatch(NUM_REQUESTS);

        WebClient client = WebClient.create(vertx,
                new WebClientOptions().setPipelining(false),
                new PoolOptions().setHttp1MaxSize(NUM_REQUESTS));
        try {
            // the requests must be sent from a Vert.x thread
            vertx.runOnContext(ignored -> {
                for (int i = 0; i < NUM_REQUESTS; i++) {
                    client.get(url.getPort(), url.getHost(), "/")
                            .send()
                            .onComplete(ignored2 -> finished.countDown());
                }
            });

            assertThat(arrived.await(10, TimeUnit.SECONDS)).isTrue();

            assertThat(currentRequests()).isEqualTo(INITIAL_LIMIT);
            assertThat(overloaded()).isEqualTo(1.0);
            // priority load shedding is disabled, so excess requests are always rejected
            // and the overload depth doesn't increase
            assertThat(overloadDepth()).isZero();
            assertThat(overloadDurationSeconds()).isPositive();

            release.countDown();

            assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            client.close();
        }

        // a rejected request is counted before its response is written, so the count is
        // complete as soon as the client saw all the responses
        assertThat(shedRequests()).isEqualTo(NUM_REQUESTS - INITIAL_LIMIT);

        // a permitted request, on the other hand, is only uncounted by an end handler that
        // may still run after the client saw the response, so we have to wait here
        awaitDrained();
        assertThat(overloaded()).isZero();
        assertThat(overloadDurationSeconds()).isZero();
    }

    private void awaitDrained() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (currentRequests() > 0 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(currentRequests()).isZero();
    }

    private double shedRequests() {
        return registry.get(LoadSheddingMetrics.SHED_REQUESTS).functionCounter().count();
    }

    private double currentRequests() {
        return registry.get(LoadSheddingMetrics.CURRENT_REQUESTS).gauge().value();
    }

    private double currentLimit() {
        return registry.get(LoadSheddingMetrics.CURRENT_LIMIT).gauge().value();
    }

    private double overloaded() {
        return registry.get(LoadSheddingMetrics.OVERLOAD_STATUS).gauge().value();
    }

    private double overloadDepth() {
        return registry.get(LoadSheddingMetrics.OVERLOAD_DEPTH).gauge().value();
    }

    private double overloadDurationSeconds() {
        return registry.get(LoadSheddingMetrics.OVERLOAD_DURATION).timeGauge().value(TimeUnit.SECONDS);
    }

    @Path("/")
    public static class MyResource {
        // this method must not run on the event loop, as it would block the remaining requests
        // and the latch would never be counted down
        @GET
        public String hello() throws InterruptedException {
            release.await(10, TimeUnit.SECONDS);
            return "Hello, world!";
        }
    }

    @Singleton
    public static class RequestCounter {
        // ordered before the load shedding route, so that requests are counted
        // before they can be rejected
        void init(@Observes Router router) {
            router.route().order(HttpLoadShedding.PRIORITY - 1).handler(ctx -> {
                arrived.countDown();
                ctx.next();
            });
        }
    }
}
