package io.quarkus.load.shedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.load.shedding.runtime.HttpLoadShedding;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Verifies that the health probes and the metrics endpoint survive an overload even
 * under pure load shedding, which rejects every other request, including requests
 * to the other non-application endpoints.
 * <p>
 * Priority load shedding exempts them through {@link RequestPriority#NEVER}, but
 * that mechanism is off when priority load shedding is disabled.
 */
public class ExemptPathsSurviveLoadSheddingTest {
    private static final int LIMIT = 5;

    private static final List<String> EXEMPT_PATHS = List.of(
            "/q/health/live", "/q/health/ready", "/q/health/started", "/q/metrics");

    // an application endpoint and a non-application endpoint that is not exempt
    private static final List<String> SHED_PATHS = List.of("/answer", "/q/answer");

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RouterInit.class))
            // the default minimum is above the limits this test uses
            .overrideConfigKey("quarkus.load-shedding.min-limit", "1")
            .overrideConfigKey("quarkus.load-shedding.initial-limit", "" + LIMIT)
            .overrideConfigKey("quarkus.load-shedding.max-limit", "" + LIMIT)
            .overrideConfigKey("quarkus.load-shedding.priority.enabled", "false")
            // a single event loop handles all incoming requests one after another, so that
            // the overload check and the following increment of the request counter cannot
            // interleave and permit more requests than the limit allows
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1");

    // the permitted requests, held open so that they stay in flight and the overload persists;
    // the test ends these requests at the end
    static final Queue<RoutingContext> held = new ConcurrentLinkedQueue<>();

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    public void test() throws InterruptedException {
        WebClient client = WebClient.create(vertx,
                new WebClientOptions().setPipelining(false),
                new PoolOptions().setHttp1MaxSize(LIMIT + EXEMPT_PATHS.size() + SHED_PATHS.size()));

        // exactly as many requests as the limit allows, so that all of them are permitted
        // and the next request finds the application overloaded
        CountDownLatch overloadBuildupRequestsFinished = new CountDownLatch(LIMIT);
        send(client, "/hold", LIMIT, status -> {
            overloadBuildupRequestsFinished.countDown();
        });

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (held.size() < LIMIT && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(held.size()).isEqualTo(LIMIT);

        Map<String, Integer> statuses = new ConcurrentHashMap<>();
        CountDownLatch done = new CountDownLatch(EXEMPT_PATHS.size() + SHED_PATHS.size());
        for (String path : EXEMPT_PATHS) {
            send(client, path, 1, status -> {
                statuses.put(path, status);
                done.countDown();
            });
        }
        for (String path : SHED_PATHS) {
            send(client, path, 1, status -> {
                statuses.put(path, status);
                done.countDown();
            });
        }
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

        for (String path : EXEMPT_PATHS) {
            assertThat(statuses).as(path).containsEntry(path, 200);
        }
        for (String path : SHED_PATHS) {
            assertThat(statuses).as(path).containsEntry(path, 503);
        }

        while (!held.isEmpty()) {
            RoutingContext ctx = held.poll();
            ctx.vertx().runOnContext(ignored -> ctx.response().end());
        }
        assertThat(overloadBuildupRequestsFinished.await(10, TimeUnit.SECONDS)).isTrue();
        client.close();
    }

    private void send(WebClient client, String path, int count, IntConsumer onResponse) {
        vertx.runOnContext(ignored -> {
            for (int i = 0; i < count; i++) {
                client.get(url.getPort(), url.getHost(), path)
                        .send()
                        .onSuccess(response -> onResponse.accept(response.statusCode()));
            }
        });
    }

    @Singleton
    public static class RouterInit {
        public void init(@Observes Router router) {
            router.route("/hold").order(HttpLoadShedding.PRIORITY + 1).handler(ctx -> {
                held.add(ctx);
            });

            router.route("/answer").order(HttpLoadShedding.PRIORITY + 1).handler(ctx -> {
                ctx.response().end();
            });

            router.route("/q/answer").order(HttpLoadShedding.PRIORITY + 1).handler(ctx -> {
                ctx.response().end();
            });
        }
    }
}
