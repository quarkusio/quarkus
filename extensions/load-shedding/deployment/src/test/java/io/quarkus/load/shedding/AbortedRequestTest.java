package io.quarkus.load.shedding;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.load.shedding.runtime.OverloadDetector;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

/**
 * Everything the overload detector reports rests on one invariant: every request that begins
 * also ends. The count of requests in flight is decremented from an end handler, and if that
 * handler can fail to run, the count can increase without decreasing, which would end up
 * with the application declaring itself permanently overloaded.
 * <p>
 * A client that disappears mid-request is the ordinary way for a response never to reach
 * anyone, and it is also exactly what happens during overload, when clients time out and
 * give up. Here the endpoint finishes into a connection that is already gone, so the response
 * is never delivered. The test asserts that the request is still accounted for.
 */
public class AbortedRequestTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(MyResource.class));

    static final CountDownLatch arrived = new CountDownLatch(1);

    static final CountDownLatch release = new CountDownLatch(1);

    @Inject
    OverloadDetector detector;

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    public void test() throws Exception {
        assertThat(detector.currentRequests()).isZero();

        NetClient client = vertx.createNetClient();
        NetSocket socket = client.connect(url.getPort(), url.getHost())
                .toCompletionStage()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
        socket.write("GET /slow HTTP/1.1\r\nHost: " + url.getHost() + ":" + url.getPort() + "\r\n\r\n");

        assertThat(arrived.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(detector.currentRequests()).isEqualTo(1);

        // the client is gone before the endpoint produced anything
        socket.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
        client.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);

        // only now, so that the endpoint writes the response into a connection that is already gone
        release.countDown();

        // the load shedding end handler runs on the event loop some time after the connection is gone,
        // so this cannot be asserted synchronously
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline && detector.currentRequests() > 0) {
            Thread.sleep(10);
        }

        assertThat(detector.currentRequests()).isZero();
    }

    @Path("/slow")
    public static class MyResource {
        // this method must not run on the event loop, as it would block the connection close
        // and the end handler would never run
        @GET
        public String hello() throws InterruptedException {
            arrived.countDown();
            release.await(10, TimeUnit.SECONDS);
            return "Hello, world!";
        }
    }
}
