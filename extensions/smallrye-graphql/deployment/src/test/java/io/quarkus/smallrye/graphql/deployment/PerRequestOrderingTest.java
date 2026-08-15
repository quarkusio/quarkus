package io.quarkus.smallrye.graphql.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.common.annotation.Blocking;

/**
 * Verifies that blocking GraphQL resolvers from different requests run concurrently,
 * and that parallel query fields within a single request are also allowed to run
 * concurrently (as the GraphQL spec permits).
 */
public class PerRequestOrderingTest extends AbstractGraphQLTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(ManagedLockResource.class, ThreadLocker.class)
                    // Pin to a single event loop
                    .addAsResource(new StringAsset(
                            "quarkus.http.io-threads=1\n"
                                    + "quarkus.vertx.event-loops-pool-size=1\n"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @BeforeEach
    public void resetState() {
        ManagedLockResource.locker = new ThreadLocker();
        ManagedLockResource.maxInFlight.set(0);
        ManagedLockResource.inFlight.set(0);
    }

    /**
     * Two concurrent HTTP requests, each invoking a single blocking resolver that parks until the
     * test releases it. With per-request ordering, both resolvers must park simultaneously on the
     * worker pool. With the previous per-event-loop ordering, the second request would queue
     * behind the first and only one would ever be parked at a time.
     */
    @Test
    public void concurrentRequestsAreNotSerialized() throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        CompletableFuture<Void> a = null;
        CompletableFuture<Void> b = null;
        try {
            a = CompletableFuture.runAsync(() -> postOk(getPayload("{ lock }")), pool);
            b = CompletableFuture.runAsync(() -> postOk(getPayload("{ lock }")), pool);

            assertTrue(ManagedLockResource.locker.awaitParked(2, 5, TimeUnit.SECONDS),
                    "Expected 2 blocking resolvers to be parked simultaneously, but they appear to be serialized");
        } finally {
            ManagedLockResource.locker.releaseAll();
            if (a != null && b != null) {
                CompletableFuture.allOf(a, b).get(10, TimeUnit.SECONDS);
            }
            pool.shutdown();
        }
    }

    /**
     * A single request that selects three parallel root fields. The GraphQL spec allows query
     * fields to resolve in parallel, and unordered {@code executeBlocking} lets them do so.
     * <p>
     * Asserts that at least 2 resolvers park simultaneously, confirming no within-request
     * serialization that would deadlock reactive clients (see issue #29141).
     */
    @Test
    public void parallelFieldsWithinOneRequestRunConcurrently() throws Exception {
        var pool = Executors.newSingleThreadExecutor();
        CompletableFuture<Void> f = null;
        try {
            f = CompletableFuture.runAsync(() -> postOk(getPayload("{ a: lock b: lock c: lock }")), pool);

            assertTrue(ManagedLockResource.locker.awaitParked(2, 5, TimeUnit.SECONDS),
                    "Expected at least 2 resolvers to run concurrently within one request");
        } finally {
            ManagedLockResource.locker.releaseAll();
            if (f != null) {
                f.get(10, TimeUnit.SECONDS);
            }
            pool.shutdown();
        }
        assertTrue(ManagedLockResource.maxInFlight.get() >= 2,
                "Expected concurrent execution within a request, but maxInFlight="
                        + ManagedLockResource.maxInFlight.get());
    }

    private static void postOk(String request) {
        RestAssured.given()
                .accept(MEDIATYPE_JSON)
                .contentType(MEDIATYPE_JSON)
                .body(request)
                .post("/graphql")
                .then()
                .statusCode(200);
    }

    /**
     * GraphQL resource whose single resolver parks on a {@link ThreadLocker} until the test
     * releases it. Used to deterministically observe how many resolvers run concurrently.
     */
    @GraphQLApi
    public static class ManagedLockResource {

        static volatile ThreadLocker locker = new ThreadLocker();
        static final AtomicInteger inFlight = new AtomicInteger();
        static final AtomicInteger maxInFlight = new AtomicInteger();

        @Query
        @Blocking
        public String lock() throws InterruptedException {
            int n = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(n, Math::max);
            try {
                locker.park();
            } finally {
                inFlight.decrementAndGet();
            }
            return "done";
        }
    }

    /**
     * Synchronization helper that parks calling threads and lets the test observe how many are
     * parked at the same time before releasing them. Deterministic alternative to {@code sleep}
     * for asserting concurrent vs. serialized execution.
     */
    public static class ThreadLocker {

        private final Semaphore parked = new Semaphore(0);
        private final CompletableFuture<Void> release = new CompletableFuture<>();

        public void park() throws InterruptedException {
            parked.release();
            try {
                release.get();
            } catch (ExecutionException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Returns {@code true} as soon as {@code n} threads are simultaneously parked; returns
         * {@code false} if the timeout elapses first.
         */
        public boolean awaitParked(int n, long timeout, TimeUnit unit) throws InterruptedException {
            return parked.tryAcquire(n, timeout, unit);
        }

        /** Release every parked thread, and let any future {@link #park()} call return immediately. */
        public void releaseAll() {
            release.complete(null);
        }
    }
}
