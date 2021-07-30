package io.quarkus.resteasy.reactive.server.test.stream;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.client.impl.MultiInvoker;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.Cancellable;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;

@DisabledOnOs(OS.WINDOWS)
public class StreamTestCase {

    @TestHTTPResource
    URI uri;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(StreamResource.class));

    @Test
    public void testStreamingDoesNotCloseConnection() throws Exception {
        Vertx v = Vertx.vertx();
        try {
            final CompletableFuture<Object> latch = new CompletableFuture<>();
            HttpClient client = v
                    .createHttpClient(
                            new HttpClientOptions().setKeepAlive(true).setIdleTimeout(10).setIdleTimeoutUnit(TimeUnit.SECONDS));
            sendRequest(latch, client, () -> sendRequest(latch, client, () -> latch.complete(null)));

            //should not have been closed
            latch.get();

        } finally {
            v.close().toCompletionStage().toCompletableFuture().get();
        }
    }

    private void sendRequest(CompletableFuture<Object> latch, HttpClient client, Runnable runnable) {
        Handler<Throwable> failure = latch::completeExceptionally;
        client.request(HttpMethod.GET, RestAssured.port, "localhost", "/stream/text/stream")
                .onFailure(failure)
                .onSuccess(new Handler<HttpClientRequest>() {
                    @Override
                    public void handle(HttpClientRequest event) {
                        event.end();
                        event.connect().onFailure(failure)
                                .onSuccess(response -> {
                                    response.request().connection().closeHandler(new Handler<Void>() {
                                        @Override
                                        public void handle(Void event) {
                                            latch.completeExceptionally(new Throwable("Connection was closed"));
                                        }
                                    });
                                    response.body().onFailure(failure)
                                            .onSuccess(buffer -> {
                                                try {
                                                    Assertions.assertEquals("foobar",
                                                            buffer.toString(StandardCharsets.US_ASCII));
                                                } catch (Throwable t) {
                                                    latch.completeExceptionally(t);
                                                }
                                                runnable.run();
                                            });
                                });

                    }
                });
    }

    @Test
    public void testStreaming() throws Exception {
        RestAssured.get("/stream/text/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/text/stream/publisher")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/text/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));

        RestAssured.get("/stream/byte-arrays/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/byte-arrays/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));

        RestAssured.get("/stream/char-arrays/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/char-arrays/stream/publisher")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/char-arrays/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));

        RestAssured.get("/stream/buffer/stream")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
        RestAssured.get("/stream/buffer/collect")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foobar"));
    }

    @Test
    public void testClientStreaming() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "stream/text/stream");
        Multi<String> multi = target.request().rx(MultiInvoker.class).get(String.class);
        List<String> list = multi.collect().asList().await().atMost(Duration.ofSeconds(5));
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("foo", list.get(0));
        Assertions.assertEquals("bar", list.get(1));
    }

    @Test
    public void testInfiniteStreamClosedByClientImmediately() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "stream/infinite/stream");
        Multi<String> multi = target.request().rx(MultiInvoker.class).get(String.class);
        Cancellable cancellable = multi.subscribe().with(item -> {
            System.err.println("Received " + item);
        });
        // immediately cancel
        cancellable.cancel();
        // give it some time and check
        Thread.sleep(2000);

        WebTarget checkTarget = client.target(uri.toString() + "stream/infinite/stream-was-cancelled");
        String check = checkTarget.request().get(String.class);
        Assertions.assertEquals("OK", check);
    }

    @Test
    public void testInfiniteStreamClosedByClientAfterRegistration() throws Exception {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "stream/infinite/stream");
        Multi<String> multi = target.request().rx(MultiInvoker.class).get(String.class);
        // cancel after two items
        CountDownLatch latch = new CountDownLatch(2);
        Cancellable cancellable = multi.subscribe().with(item -> {
            System.err.println("Received " + item);
            latch.countDown();
        });
        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
        // now cancel
        cancellable.cancel();
        // give it some time and check
        Thread.sleep(2000);

        WebTarget checkTarget = client.target(uri.toString() + "stream/infinite/stream-was-cancelled");
        String check = checkTarget.request().get(String.class);
        Assertions.assertEquals("OK", check);
    }

    @Test
    public void testSse() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "stream/sse");
        try (SseEventSource sse = SseEventSource.target(target).build()) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            List<String> results = new CopyOnWriteArrayList<>();
            sse.register(event -> {
                results.add(event.readData());
            }, error -> {
                errors.add(error);
            }, () -> {
                latch.countDown();
            });
            sse.open();
            Assertions.assertTrue(latch.await(20, TimeUnit.SECONDS));
            Assertions.assertEquals(Arrays.asList("a", "b", "c"), results);
            Assertions.assertEquals(0, errors.size());
        }
    }

    @Test
    public void testSseThrows() throws InterruptedException {
        Client client = ClientBuilder.newBuilder().build();
        WebTarget target = client.target(uri.toString() + "stream/sse/throws");
        try (SseEventSource sse = SseEventSource.target(target).build()) {
            CountDownLatch latch = new CountDownLatch(1);
            List<Throwable> errors = new CopyOnWriteArrayList<>();
            List<String> results = new CopyOnWriteArrayList<>();
            sse.register(event -> {
                results.add(event.readData());
            }, error -> {
                errors.add(error);
            }, () -> {
                latch.countDown();
            });
            sse.open();
            Assertions.assertTrue(latch.await(20, TimeUnit.SECONDS));
            Assertions.assertEquals(0, results.size());
            Assertions.assertEquals(1, errors.size());
        }
    }
}
