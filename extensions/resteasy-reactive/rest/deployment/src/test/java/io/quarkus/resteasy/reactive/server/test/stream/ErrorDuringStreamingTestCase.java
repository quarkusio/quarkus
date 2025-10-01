package io.quarkus.resteasy.reactive.server.test.stream;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.RestQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.impl.NoStackTraceException;

public class ErrorDuringStreamingTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class));

    @Inject
    Vertx vertx;

    @Test
    public void noFailure() throws ExecutionException, InterruptedException, TimeoutException {
        HttpClient client = null;
        try {
            AtomicLong count = new AtomicLong();
            CompletableFuture<Object> latch = new CompletableFuture<>();
            client = vertx.createHttpClient();
            sendRequest(client, "/test", latch, b -> count.getAndIncrement());
            latch.get(10, TimeUnit.SECONDS);
            Assertions.assertEquals(2 * 100, count.get());
        } finally {
            if (client != null) {
                client.close().toCompletionStage().toCompletableFuture().get();
            }
        }
    }

    @Test
    public void failure() throws InterruptedException, TimeoutException, ExecutionException {
        HttpClient client = null;
        AtomicLong count = new AtomicLong();
        try {
            CompletableFuture<Object> latch = new CompletableFuture<>();
            client = vertx.createHttpClient();
            sendRequest(client, "/test?fail=true", latch, b -> count.getAndIncrement());
            latch.get(10, TimeUnit.SECONDS);
            fail("The client should have failed as the server reset the connection");
        } catch (ExecutionException e) {
            Assertions.assertInstanceOf(HttpClosedException.class, e.getCause());
            Assertions.assertEquals(100, count.get());
        } finally {
            if (client != null) {
                client.close().toCompletionStage().toCompletableFuture().get();
            }
        }
    }

    private void sendRequest(HttpClient client, String requestURI, CompletableFuture<Object> latch,
            Consumer<Buffer> bodyConsumer) {
        Handler<Throwable> failure = latch::completeExceptionally;
        client.request(HttpMethod.GET, RestAssured.port, "localhost", requestURI)
                .onFailure(failure)
                .onSuccess(new Handler<>() {
                    @Override
                    public void handle(HttpClientRequest event) {
                        event.connect().onFailure(failure)
                                .onSuccess(response -> {
                                    response
                                            .handler(bodyConsumer::accept)
                                            .exceptionHandler(latch::completeExceptionally)
                                            .end(latch::complete);
                                });

                    }
                });
    }

    @Path("test")
    public static class Resource {

        @GET
        public Multi<String> stream(@RestQuery @DefaultValue("false") boolean fail) {
            return Multi.createFrom().emitter(emitter -> {
                emit(emitter);
                if (fail) {
                    throw new NoStackTraceException("dummy");
                } else {
                    emit(emitter);
                    emitter.complete();
                }
            });
        }

        private static void emit(MultiEmitter<? super String> emitter) {
            IntStream.range(0, 100).forEach(i -> {
                emitter.emit(String.valueOf(i));
            });
        }
    }
}
