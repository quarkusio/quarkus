package io.quarkus.resteasy.reactive.server.test.http2;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.quarkus.vertx.utils.VertxJavaIoContext;
import io.quarkus.vertx.utils.VertxOutputStream;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * Verifies that the exception and end handlers in {@link VertxOutputStream}
 * use {@code tryLock} so they never block when the writer holds the lock.
 * <p>
 * Same principle as {@code VertxBlockingOutputLockTest} in resteasy-classic:
 * a writer thread holds the lock inside {@code response.write()}, and a handler
 * fires on another thread. With {@code tryLock}, the handler returns immediately;
 * with {@code lock()}, the test times out.
 */
class VertxOutputStreamLockTest {

    @SuppressWarnings("unchecked")
    @Test
    void exceptionHandlerDoesNotBlockWhenWriterHoldsLock() {
        AtomicBoolean writerInWrite = new AtomicBoolean();
        AtomicBoolean releaseWriter = new AtomicBoolean();
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);

        VertxOutputStream output = createOutput(exceptionCaptor, null, writerInWrite, releaseWriter);
        Thread writer = startWriterThread(output);

        await().atMost(Duration.ofSeconds(5)).untilTrue(writerInWrite);

        AtomicBoolean handlerDone = new AtomicBoolean();
        new Thread(() -> {
            exceptionCaptor.getValue().handle(new RuntimeException("simulated connection error"));
            handlerDone.set(true);
        }).start();
        await().atMost(Duration.ofMillis(500)).untilTrue(handlerDone);

        releaseWriter.set(true);
        await().atMost(Duration.ofSeconds(5)).until(() -> !writer.isAlive());
    }

    @SuppressWarnings("unchecked")
    @Test
    void endHandlerDoesNotBlockWhenWriterHoldsLock() {
        AtomicBoolean writerInWrite = new AtomicBoolean();
        AtomicBoolean releaseWriter = new AtomicBoolean();
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
        ArgumentCaptor<Handler<AsyncResult<Void>>> endCaptor = ArgumentCaptor.forClass(Handler.class);

        VertxOutputStream output = createOutput(exceptionCaptor, endCaptor, writerInWrite, releaseWriter);
        Thread writer = startWriterThread(output);

        await().atMost(Duration.ofSeconds(5)).untilTrue(writerInWrite);

        AtomicBoolean handlerDone = new AtomicBoolean();
        new Thread(() -> {
            endCaptor.getValue().handle(null);
            handlerDone.set(true);
        }).start();
        await().atMost(Duration.ofMillis(500)).untilTrue(handlerDone);

        releaseWriter.set(true);
        await().atMost(Duration.ofSeconds(5)).until(() -> !writer.isAlive());
    }

    private VertxOutputStream createOutput(ArgumentCaptor<Handler<Throwable>> exceptionCaptor,
            ArgumentCaptor<Handler<AsyncResult<Void>>> endCaptor,
            AtomicBoolean writerInWrite, AtomicBoolean releaseWriter) {
        HttpServerRequest request = mock(HttpServerRequest.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        HttpConnection connection = mock(HttpConnection.class);
        RoutingContext routingContext = mock(RoutingContext.class);

        when(routingContext.request()).thenReturn(request);
        if (endCaptor != null) {
            when(routingContext.addEndHandler(endCaptor.capture())).thenReturn(0);
        } else {
            when(routingContext.addEndHandler(any(Handler.class))).thenReturn(0);
        }
        when(request.response()).thenReturn(response);
        when(request.connection()).thenReturn(connection);
        when(connection.close()).thenReturn(Future.succeededFuture());
        when(response.exceptionHandler(exceptionCaptor.capture())).thenReturn(response);
        when(response.drainHandler(any(Handler.class))).thenReturn(response);
        when(response.closeHandler(any(Handler.class))).thenReturn(response);
        when(response.setChunked(true)).thenReturn(response);
        when(response.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());

        when(response.write(any(Buffer.class))).thenAnswer(invocation -> {
            writerInWrite.set(true);
            await().atMost(Duration.ofSeconds(5)).untilTrue(releaseWriter);
            return Future.succeededFuture();
        });

        return new VertxOutputStream(new VertxJavaIoContext(routingContext, 128, 8192));
    }

    private Thread startWriterThread(VertxOutputStream output) {
        Thread writer = new Thread(() -> {
            try {
                // Must exceed outputBufferCapacity (8192) to trigger a flush to response.write()
                output.write(new byte[16384]);
            } catch (IOException ignored) {
            }
        });
        writer.start();
        return writer;
    }
}
