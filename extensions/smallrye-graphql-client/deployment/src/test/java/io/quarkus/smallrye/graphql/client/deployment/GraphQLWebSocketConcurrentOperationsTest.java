package io.quarkus.smallrye.graphql.client.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClientBuilder;

/**
 * Verifies that concurrent blocking GraphQL operations sent over WebSocket
 * are not serialized across operations sharing an event loop.
 * Similar to
 * extensions/smallrye-graphql/deployment/src/test/java/io/quarkus/smallrye/graphql/deployment/PerRequestOrderingTest.java
 * but for WebSockets. Given that we use the client API, this lives in the client module even though
 * it's technically a test for the server side.
 */
public class GraphQLWebSocketConcurrentOperationsTest {

    static String url = "http://" + System.getProperty("quarkus.http.host", "localhost") + ":" +
            System.getProperty("quarkus.http.test-port", "8081") + "/graphql";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((JavaArchive jar) -> jar
                    .addClasses(BlockingApi.class, ThreadLocker.class)
                    .addAsResource(new StringAsset(
                            "quarkus.http.io-threads=1\n"
                                    + "quarkus.vertx.event-loops-pool-size=1\n"),
                            "application.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @BeforeEach
    public void resetState() {
        BlockingApi.locker = new ThreadLocker();
    }

    /**
     * Two concurrent operations sent over WebSocket, each invoking a blocking resolver
     * that parks until the test releases it. Both resolvers must park simultaneously on
     * the worker pool. If they were serialized (as the HTTP path was before the per-request
     * TaskQueue fix), only one would ever be parked at a time.
     */
    @Test
    public void concurrentOperationsOverWebSocketAreNotSerialized() throws Exception {
        DynamicGraphQLClientBuilder clientBuilder = DynamicGraphQLClientBuilder.newBuilder()
                .url(url)
                .executeSingleOperationsOverWebsocket(true);
        try (DynamicGraphQLClient client = clientBuilder.build()) {
            CompletableFuture<Response> a = client.executeAsync("{ lock }").subscribeAsCompletionStage();
            CompletableFuture<Response> b = client.executeAsync("{ lock }").subscribeAsCompletionStage();

            assertTrue(BlockingApi.locker.awaitParked(2, 10, TimeUnit.SECONDS),
                    "Expected 2 blocking resolvers to be parked simultaneously, but they appear to be serialized");

            BlockingApi.locker.releaseAll();

            Response ra = a.get(10, TimeUnit.SECONDS);
            Response rb = b.get(10, TimeUnit.SECONDS);

            assertTrue(ra.hasData(), "First operation should have data");
            assertTrue(rb.hasData(), "Second operation should have data");
        }
    }

    @GraphQLApi
    public static class BlockingApi {

        static volatile ThreadLocker locker = new ThreadLocker();

        @Query
        @Blocking
        public String lock() throws InterruptedException {
            locker.park();
            return "done";
        }
    }

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

        public boolean awaitParked(int n, long timeout, TimeUnit unit) throws InterruptedException {
            return parked.tryAcquire(n, timeout, unit);
        }

        public void releaseAll() {
            release.complete(null);
        }
    }
}
