package io.quarkus.load.shedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
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
 * Verifies the guarantee that priority load shedding exists to provide: requests
 * to the non-application endpoints survive an overload deep enough that ordinary
 * traffic is rejected.
 * <p>
 * The built-in prioritizer assigns {@link RequestPriority#NEVER} to those endpoints.
 * An orchestrator probing them would conclude the application is dead if they start
 * failing, which is a worse outcome than an overload itself.
 */
public class NeverRequestsSurviveLoadSheddingTest {
    private static final Logger log = Logger.getLogger(NeverRequestsSurviveLoadSheddingTest.class);

    private static final int LIMIT = 5;

    // 5 requests to get to the limit
    // 5 more requests to reach overload depth of 1.0 (2 should be enough, but let's play safe)
    private static final int NUM_OVERLOAD_BUILDUP_REQUESTS = 2 * LIMIT;

    // value is arbitrary
    private static final int NUM_NEVER_REQUESTS = LIMIT;

    // value is arbitrary
    private static final int NUM_NORMAL_REQUESTS = LIMIT;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(RouterInit.class))
            // the default minimum is above the limits this test uses
            .overrideConfigKey("quarkus.load-shedding.min-limit", "1")
            // in this test, we always stay in the 2nd phase of overload
            .overrideConfigKey("quarkus.load-shedding.initial-limit", "" + LIMIT)
            .overrideConfigKey("quarkus.load-shedding.max-limit", "" + LIMIT)
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
                new PoolOptions().setHttp1MaxSize(NUM_OVERLOAD_BUILDUP_REQUESTS
                        + NUM_NORMAL_REQUESTS + NUM_NEVER_REQUESTS));

        CountDownLatch overloadBuildupRequestsFinished = new CountDownLatch(NUM_OVERLOAD_BUILDUP_REQUESTS);
        send(client, "/q/hold", NUM_OVERLOAD_BUILDUP_REQUESTS, status -> {
            overloadBuildupRequestsFinished.countDown();
        });

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (held.size() < NUM_OVERLOAD_BUILDUP_REQUESTS && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(held.size()).isEqualTo(NUM_OVERLOAD_BUILDUP_REQUESTS);

        AtomicInteger normalSuccesses = new AtomicInteger();
        AtomicInteger normalRejections = new AtomicInteger();
        CountDownLatch normalDone = new CountDownLatch(NUM_NORMAL_REQUESTS);

        send(client, "/answer", NUM_NORMAL_REQUESTS, status -> {
            if (status == 503) {
                normalRejections.incrementAndGet();
            } else if (status == 200) {
                normalSuccesses.incrementAndGet();
            }
            normalDone.countDown();
        });

        AtomicInteger neverSuccesses = new AtomicInteger();
        AtomicInteger neverRejections = new AtomicInteger();
        CountDownLatch neverDone = new CountDownLatch(NUM_NEVER_REQUESTS);

        send(client, "/q/answer", NUM_NEVER_REQUESTS, status -> {
            if (status == 503) {
                neverRejections.incrementAndGet();
            } else if (status == 200) {
                neverSuccesses.incrementAndGet();
            }
            neverDone.countDown();
        });

        assertThat(normalDone.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(normalSuccesses).hasValue(0);
        assertThat(normalRejections).hasValue(NUM_NORMAL_REQUESTS);

        assertThat(neverDone.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(neverSuccesses).hasValue(NUM_NEVER_REQUESTS);
        assertThat(neverRejections).hasValue(0);

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
            router.route("/q/hold").order(HttpLoadShedding.PRIORITY + 1).handler(ctx -> {
                held.add(ctx);
            });

            router.route("/q/answer").order(HttpLoadShedding.PRIORITY + 1).handler(ctx -> {
                ctx.response().end();
            });

            router.route("/answer").order(HttpLoadShedding.PRIORITY + 1).handler(ctx -> {
                ctx.response().end();
            });
        }
    }
}
