package io.quarkus.vertx.http.runtime.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.dev.spi.HotReplacementContext;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;

class RemoteSyncHandlerHttpTest {

    @Test
    void trueChunkedRequestCrossingLimitReturnsPayloadTooLarge() throws Exception {
        Vertx vertx = Vertx.vertx();
        var context = org.mockito.Mockito.mock(HotReplacementContext.class);
        var handler = new RemoteSyncHandler("secret", request -> request.response().setStatusCode(404).end(),
                context, "/root", new RemoteDevBodyLimits(5, 10, 4)) {
            @Override
            <T> Future<T> executeBlocking(Callable<T> action) {
                return vertx.executeBlocking(action, false);
            }
        };
        var server = await(vertx.createHttpServer().requestHandler(handler).listen(0));
        var client = vertx.createHttpClient();
        try {
            var request = await(client.request(HttpMethod.PUT, server.actualPort(), "localhost",
                    "/root/app/classes/com/acme/Foo.class"));
            request.setChunked(true)
                    .putHeader("Content-Type", RemoteSyncHandler.APPLICATION_QUARKUS)
                    .putHeader(RemoteSyncHandler.QUARKUS_SESSION, "session")
                    .putHeader(RemoteSyncHandler.QUARKUS_SESSION_COUNT, "1")
                    .putHeader(RemoteSyncHandler.QUARKUS_PASSWORD, "irrelevant");

            var response = await(request.send(Buffer.buffer("123456")));
            await(response.body());

            assertThat(response.statusCode()).isEqualTo(413);
            assertThat(response.getHeader(RemoteSyncHandler.QUARKUS_ERROR))
                    .contains("quarkus.http.limits.max-body-size");
            org.mockito.Mockito.verify(context, org.mockito.Mockito.never()).updateFile(
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
            handler.close();
            awaitSpoolCleanup(handler);
        } finally {
            handler.close();
            await(client.close());
            await(server.close());
            await(vertx.close());
        }
    }

    private static <T> T await(Future<T> future) throws Exception {
        return future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }

    private static void awaitSpoolCleanup(RemoteSyncHandler handler) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (handler.bodySpoolDirectory() != null && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(handler.bodySpoolDirectory()).isNull();
    }
}
