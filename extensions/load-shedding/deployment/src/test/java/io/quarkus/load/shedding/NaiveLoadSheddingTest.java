package io.quarkus.load.shedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.load.shedding.runtime.HttpLoadShedding;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class NaiveLoadSheddingTest {
    private static final int NUM_REQUESTS = 50;
    private static final int INITIAL_LIMIT = 5;

    // way less than the default, so that the test can also verify the configuration
    private static final int MAX_RETRY_AFTER_SECONDS = 7;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyResource.class, RequestCounter.class))
            .overrideConfigKey("quarkus.load-shedding.initial-limit", "" + INITIAL_LIMIT)
            // the default minimum is above the limits this test uses
            .overrideConfigKey("quarkus.load-shedding.min-limit", "1")
            .overrideConfigKey("quarkus.load-shedding.priority.enabled", "false")
            .overrideConfigKey("quarkus.load-shedding.max-retry-after", MAX_RETRY_AFTER_SECONDS + "s")
            // a single event loop handles all incoming requests one after another, so that
            // the overload check and the following increment of the request counter cannot
            // interleave and permit more requests than the limit allows
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1");

    // counted down when a request reaches the server, no matter if it is permitted or rejected
    static final CountDownLatch arrived = new CountDownLatch(NUM_REQUESTS);

    // opened by the test once all requests arrived
    static final CountDownLatch release = new CountDownLatch(1);

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    public void test() throws InterruptedException {
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger rejections = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(NUM_REQUESTS);

        WebClient client = WebClient.create(vertx,
                new WebClientOptions().setPipelining(false),
                new PoolOptions().setHttp1MaxSize(NUM_REQUESTS));
        try {
            // the requests must be sent from a Vert.x thread
            vertx.runOnContext(ignored -> {
                for (int i = 0; i < NUM_REQUESTS; i++) {
                    client.get(url.getPort(), url.getHost(), "/")
                            .send()
                            .onSuccess(response -> {
                                if (response.statusCode() == 200) {
                                    successes.incrementAndGet();
                                } else if (isRejectedCorrectly(response)) {
                                    rejections.incrementAndGet();
                                } else {
                                    errors.incrementAndGet();
                                }
                                done.countDown();
                            })
                            .onFailure(error -> {
                                errors.incrementAndGet();
                                done.countDown();
                            });
                }
            });

            // The TCP Vegas algorithm adapts the limit whenever a request ends, and an adaptation
            // in the middle of the run would make the result depend on timing. Permitted requests
            // therefore block until all requests arrived, which makes the outcome deterministic:
            // the first 5 requests are permitted and the remaining 45 are rejected.
            assertThat(arrived.await(10, TimeUnit.SECONDS)).isTrue();
            release.countDown();

            assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            client.close();
        }

        assertThat(successes).hasValue(INITIAL_LIMIT);
        assertThat(rejections).hasValue(NUM_REQUESTS - INITIAL_LIMIT);
        assertThat(errors).hasValue(0);
    }

    // A rejected request must not only fail, it must tell the client when to come back.
    // The overload here lasts well under a second, so the value is at the lower end of the
    // range. All we can verify is that it is a usable number within the configured bound.
    private static boolean isRejectedCorrectly(HttpResponse<Buffer> response) {
        if (response.statusCode() != 503) {
            return false;
        }

        String retryAfter = response.getHeader(HttpHeaders.RETRY_AFTER);
        if (retryAfter == null) {
            return false;
        }

        try {
            long seconds = Long.parseLong(retryAfter);
            return seconds >= 1 && seconds <= MAX_RETRY_AFTER_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
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
