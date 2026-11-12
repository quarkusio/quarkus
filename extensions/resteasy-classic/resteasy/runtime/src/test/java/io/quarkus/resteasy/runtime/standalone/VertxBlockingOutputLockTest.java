package io.quarkus.resteasy.runtime.standalone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.netty.buffer.Unpooled;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * Verifies that the exception and end handlers in {@link VertxBlockingOutput}
 * use {@code tryLock} so they never block when the writer holds the lock.
 * <p>
 * The real deadlock (AB-BA between the {@code ReentrantLock} and the Vert.x
 * HTTP/2 connection monitor) requires precise timing with real HTTP/2 frames
 * and cannot be reproduced from a high-level client. This test simulates the
 * lock contention directly: a writer thread holds the lock inside
 * {@code response.write()}, and a handler fires on another thread.
 * With {@code tryLock}, the handler returns immediately; with {@code lock()},
 * the test times out.
 */
class VertxBlockingOutputLockTest {

    @SuppressWarnings("unchecked")
    @Test
    void exceptionHandlerDoesNotBlockWhenWriterHoldsLock() {
        AtomicBoolean writerInWrite = new AtomicBoolean();
        AtomicBoolean releaseWriter = new AtomicBoolean();
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
        ArgumentCaptor<Handler<Void>> endCaptor = ArgumentCaptor.forClass(Handler.class);

        VertxBlockingOutput output = createOutput(exceptionCaptor, endCaptor, writerInWrite, releaseWriter);
        Thread writer = startWriterThread(output);

        await().atMost(Duration.ofSeconds(5)).untilTrue(writerInWrite);

        AtomicBoolean handlerDone = new AtomicBoolean();
        new Thread(() -> {
            exceptionCaptor.getValue().handle(new RuntimeException("simulated connection error"));
            handlerDone.set(true);
        }).start();
        await().atMost(Duration.ofMillis(500)).untilTrue(handlerDone);

        assertThat(output.throwable)
                .as("throwable should be set regardless of lock outcome")
                .isNotNull();

        releaseWriter.set(true);
        await().atMost(Duration.ofSeconds(5)).until(() -> !writer.isAlive());
    }

    @SuppressWarnings("unchecked")
    @Test
    void endHandlerDoesNotBlockWhenWriterHoldsLock() {
        AtomicBoolean writerInWrite = new AtomicBoolean();
        AtomicBoolean releaseWriter = new AtomicBoolean();
        ArgumentCaptor<Handler<Throwable>> exceptionCaptor = ArgumentCaptor.forClass(Handler.class);
        ArgumentCaptor<Handler<Void>> endCaptor = ArgumentCaptor.forClass(Handler.class);

        VertxBlockingOutput output = createOutput(exceptionCaptor, endCaptor, writerInWrite, releaseWriter);
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

    private VertxBlockingOutput createOutput(ArgumentCaptor<Handler<Throwable>> exceptionCaptor,
            ArgumentCaptor<Handler<Void>> endCaptor,
            AtomicBoolean writerInWrite, AtomicBoolean releaseWriter) {
        HttpServerRequest request = mock(HttpServerRequest.class);
        HttpServerResponse response = mock(HttpServerResponse.class);
        HttpConnection connection = mock(HttpConnection.class);

        when(request.response()).thenReturn(response);
        when(request.connection()).thenReturn(connection);
        when(connection.close()).thenReturn(Future.succeededFuture());

        when(response.exceptionHandler(exceptionCaptor.capture())).thenReturn(response);
        when(response.endHandler(endCaptor.capture())).thenReturn(response);

        when(response.write(any(Buffer.class))).thenAnswer(invocation -> {
            writerInWrite.set(true);
            await().atMost(Duration.ofSeconds(5)).untilTrue(releaseWriter);
            return Future.succeededFuture();
        });

        return new VertxBlockingOutput(request);
    }

    private Thread startWriterThread(VertxBlockingOutput output) {
        Thread writer = new Thread(() -> {
            try {
                output.write(Unpooled.wrappedBuffer(new byte[10]), false);
            } catch (IOException ignored) {
            }
        });
        writer.start();
        return writer;
    }
}
