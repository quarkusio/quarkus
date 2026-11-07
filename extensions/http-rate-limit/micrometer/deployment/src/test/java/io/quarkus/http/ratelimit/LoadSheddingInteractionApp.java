package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import io.quarkus.builder.Version;
import io.quarkus.http.ratelimit.runtime.internal.RateLimitFilter;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * The application of the load shedding interaction tests, configured like {@code ExemptPathsSurviveLoadSheddingTest}
 * in the load shedding extension: a fixed concurrency limit of {@link #LIMIT}, pure load shedding (no priorities) and a
 * single event loop.
 * <p>
 * Routes: {@code /hold} keeps its requests open until {@link #releaseHolds()}, {@code /answer} answers at once, and
 * {@code /slow} and {@code /auth/*} answer after {@link #SLOW_MILLIS}; all of them run after both rate limit hooks.
 * A request carrying {@code X-Probe} has the value of {@link RateLimitFilter#SKIP_SAMPLE_KEY}, as load shedding reads it
 * when the response ends, recorded in {@link #skipSample}.
 */
final class LoadSheddingInteractionApp {

    static final int LIMIT = 5;

    static final List<Dependency> DEPENDENCIES = List.of(
            Dependency.of("io.quarkus", "quarkus-load-shedding-deployment", Version.getVersion()),
            Dependency.of("io.quarkus", "quarkus-smallrye-health-deployment", Version.getVersion()));

    // copies of the constants of HttpLoadShedding and LoadSheddingMetrics, the meter names as the Prometheus scrape
    // shows them: load shedding is only forced into the application, it is not on the test compile classpath
    static final int LOAD_SHEDDING_PRIORITY = -1_000_000_000;
    static final String SHED_REQUESTS = "load_shedding_shed_requests_total";
    static final String CURRENT_REQUESTS = "load_shedding_current_requests";
    static final String CURRENT_LIMIT = "load_shedding_current_limit_requests";
    static final String OVERLOAD_STATUS = "load_shedding_overload_status";

    static final long SLOW_MILLIS = 100;

    // the permitted /hold requests, held open so that they stay in flight and the overload persists
    static final Queue<RoutingContext> held = new ConcurrentLinkedQueue<>();

    // X-Probe value -> the skip-sample marking when the response ended; empty when the key was absent
    static final Map<String, Optional<Object>> skipSample = new ConcurrentHashMap<>();

    private LoadSheddingInteractionApp() {
    }

    static QuarkusExtensionTest create(Class<?>... classes) {
        return new QuarkusExtensionTest()
                .withApplicationRoot(jar -> jar.addClasses(Routes.class, LoadSheddingInteractionApp.class, TestHttp.class,
                        TestRateLimitClock.class, TestClient.class, MetricsScrape.class).addClasses(classes))
                .setForcedDependencies(DEPENDENCIES)
                // the default minimum is above the limits this test uses
                .overrideConfigKey("quarkus.load-shedding.min-limit", "1")
                .overrideConfigKey("quarkus.load-shedding.initial-limit", "" + LIMIT)
                .overrideConfigKey("quarkus.load-shedding.max-limit", "" + LIMIT)
                .overrideConfigKey("quarkus.load-shedding.priority.enabled", "false")
                // a single event loop handles all incoming requests one after another, so that
                // the overload check and the following increment of the request counter cannot
                // interleave and permit more requests than the limit allows
                .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1");
    }

    /**
     * Lets the overload detector adapt the limit at all. The limit starts at its maximum, so it can only decrease, and
     * it decreases when the estimated queue exceeds {@code beta-factor * (1 + log10(limit))}; with the default factor
     * of 6 that is more than the limit of 5 itself, which no queue estimate ever reaches. With a factor of 3, the limit
     * decreases once a request takes at least 2.5 times the lowest latency seen: a 429 of some microseconds taken as the
     * baseline against a {@code /slow} request does that by a factor of a thousand, while the jitter of {@code /slow}
     * requests among themselves never comes close.
     * <p>
     * A probe resets the lowest latency to that of the request it happens on, every {@code 30 * limit} requests or so by
     * default, at a jittered count. One that fell on the normal traffic would erase a wrong baseline before it is
     * compared against, and let a broken marking pass now and then, so probes are pushed out of reach of these tests.
     */
    static QuarkusExtensionTest adaptive(QuarkusExtensionTest test) {
        return test.overrideConfigKey("quarkus.load-shedding.alpha-factor", "1")
                .overrideConfigKey("quarkus.load-shedding.beta-factor", "3")
                .overrideConfigKey("quarkus.load-shedding.probe-factor", "1000");
    }

    static WebClient client(Vertx vertx) {
        return WebClient.create(vertx, new WebClientOptions().setPipelining(false),
                new PoolOptions().setHttp1MaxSize(4 * LIMIT));
    }

    static CompletableFuture<HttpResponse<Buffer>> sendAsync(Vertx vertx, WebClient client, URL url, String path,
            Map<String, String> headers) {
        CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();
        vertx.runOnContext(ignored -> {
            HttpRequest<Buffer> request = client.get(url.getPort(), url.getHost(), path);
            headers.forEach(request::putHeader);
            request.send().onSuccess(result::complete).onFailure(result::completeExceptionally);
        });
        return result;
    }

    static HttpResponse<Buffer> get(Vertx vertx, WebClient client, URL url, String path, Map<String, String> headers) {
        return TestHttp.get(vertx, client, url, path, headers);
    }

    static int status(Vertx vertx, WebClient client, URL url, String path, String clientKey) {
        return get(vertx, client, url, path, Map.of("X-Client", clientKey)).statusCode();
    }

    /**
     * Sends {@link #LIMIT} requests to {@code /hold} and waits until all of them are held, which leaves the application
     * overloaded until {@link #releaseHolds()}.
     *
     * @return the responses to the held requests
     */
    static List<CompletableFuture<HttpResponse<Buffer>>> overload(Vertx vertx, WebClient client, URL url,
            String clientKey) throws InterruptedException {
        List<CompletableFuture<HttpResponse<Buffer>>> holds = new ArrayList<>();
        for (int i = 0; i < LIMIT; i++) {
            holds.add(sendAsync(vertx, client, url, "/hold", Map.of("X-Client", clientKey)));
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (held.size() < LIMIT && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(held).hasSize(LIMIT);
        return holds;
    }

    static void releaseHolds() {
        RoutingContext ctx;
        while ((ctx = held.poll()) != null) {
            RoutingContext request = ctx;
            request.vertx().runOnContext(ignored -> request.response().end());
        }
    }

    /**
     * Sends 10 requests to {@code /slow}, at most 4 at a time. The detector only adapts the limit on requests that end
     * while at least half of the limit is in flight; with 4 in flight against a limit of 5, the first two requests of
     * each round of 4 to end do, so the limit could change here at all.
     */
    static void normalTraffic(Vertx vertx, WebClient client, URL url) throws Exception {
        int sent = 0;
        while (sent < 10) {
            List<CompletableFuture<HttpResponse<Buffer>>> round = new ArrayList<>();
            for (int i = 0; i < 4 && sent < 10; i++, sent++) {
                round.add(sendAsync(vertx, client, url, "/slow", Map.of("X-Client", "normal")));
            }
            for (CompletableFuture<HttpResponse<Buffer>> response : round) {
                HttpResponse<Buffer> served = response.get(10, TimeUnit.SECONDS);
                assertThat(served.statusCode()).isEqualTo(200);
                assertThat(served.bodyAsString()).isEqualTo("slow");
            }
        }
    }

    /**
     * Asserts that the detector neither signals an overload nor has adapted its limit, once every request has ended.
     */
    static void assertDetectorUndisturbed(Vertx vertx, URL url) {
        TestClient http = new TestClient(vertx, url);
        try {
            // the end handlers run once the response is written, possibly after the client has seen it; the scrape
            // is itself a request in flight, so 1 means that every other request has ended
            await().atMost(5, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> assertScrape(http).hasMetricWithLabelsAndValue(CURRENT_REQUESTS, 1));
            assertScrape(http)
                    .hasMetricWithLabelsAndValue(OVERLOAD_STATUS, 0)
                    .hasMetricWithLabelsAndValue(CURRENT_LIMIT, LIMIT);
        } finally {
            http.close();
        }
    }

    static Map<String, String> probe(String id, Map<String, String> headers) {
        Map<String, String> result = new HashMap<>(headers);
        result.put("X-Probe", id);
        return result;
    }

    // the end handler runs once the response is written, possibly after the client has seen it
    static Optional<Object> recorded(String id) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!skipSample.containsKey(id) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(skipSample).containsKey(id);
        return skipSample.get(id);
    }

    @Singleton
    public static class Routes {

        void init(@Observes Router router) {
            // right after load shedding, so that the end handler sees what load shedding sees
            router.route().order(LOAD_SHEDDING_PRIORITY + 1).handler(ctx -> {
                String probe = ctx.request().getHeader("X-Probe");
                if (probe != null) {
                    ctx.addEndHandler(
                            ignored -> skipSample.put(probe, Optional.ofNullable(ctx.get(RateLimitFilter.SKIP_SAMPLE_KEY))));
                }
                ctx.next();
            });
            router.get("/hold").handler(held::add);
            router.get("/answer").handler(ctx -> ctx.end("answer"));
            router.get("/slow").handler(LoadSheddingInteractionApp.Routes::slow);
            router.get("/auth/*").handler(LoadSheddingInteractionApp.Routes::slow);
        }

        private static void slow(RoutingContext ctx) {
            ctx.vertx().setTimer(SLOW_MILLIS, ignored -> ctx.end("slow"));
        }
    }
}
