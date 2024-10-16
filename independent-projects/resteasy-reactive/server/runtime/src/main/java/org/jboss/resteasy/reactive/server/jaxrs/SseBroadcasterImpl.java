package org.jboss.resteasy.reactive.server.jaxrs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

public class SseBroadcasterImpl implements SseBroadcaster {

    private final List<SseEventSink> sinks = new ArrayList<>();
    private final List<BiConsumer<SseEventSink, Throwable>> onErrorListeners = new ArrayList<>();
    private final List<Consumer<SseEventSink>> onCloseListeners = new ArrayList<>();
    private volatile boolean isClosed;

    @Override
    public synchronized void onError(BiConsumer<SseEventSink, Throwable> onError) {
        Objects.requireNonNull(onError);
        checkClosed();
        onErrorListeners.add(onError);
    }

    @Override
    public synchronized void onClose(Consumer<SseEventSink> onClose) {
        Objects.requireNonNull(onClose);
        checkClosed();
        onCloseListeners.add(onClose);
    }

    @Override
    public synchronized void register(SseEventSink sseEventSink) {
        Objects.requireNonNull(sseEventSink);
        checkClosed();
        if (sseEventSink instanceof SseEventSinkImpl == false) {
            throw new IllegalArgumentException("Can only work with Quarkus-REST instances: " + sseEventSink);
        }
        ((SseEventSinkImpl) sseEventSink).register(this);
        sinks.add(sseEventSink);
    }

    @Override
    public synchronized CompletionStage<?> broadcast(OutboundSseEvent event) {
        Objects.requireNonNull(event);
        checkClosed();
        CompletableFuture<?>[] cfs = new CompletableFuture[sinks.size()];
        for (int i = 0; i < sinks.size(); i++) {
            SseEventSink sseEventSink = sinks.get(i);
            CompletionStage<?> cs;
            try {
                cs = sseEventSink.send(event).exceptionally((t) -> {
                    // do not propagate the exception to the returned CF
                    // apparently, the goal is to close this sink and not report the error
                    // of the broadcast operation
                    notifyOnErrorListeners(sseEventSink, t);
                    return null;
                });
            } catch (Exception e) {
                // do not propagate the exception to the returned CF
                // apparently, the goal is to close this sink and not report the error
                // of the broadcast operation
                notifyOnErrorListeners(sseEventSink, e);
                cs = CompletableFuture.completedFuture(null);
            }
            cfs[i] = cs.toCompletableFuture();
        }
        return CompletableFuture.allOf(cfs);
    }

    private void notifyOnErrorListeners(SseEventSink eventSink, Throwable throwable) {
        // We have to notify close listeners if the SSE event output has been
        // closed (either by client closing the connection (IOException) or by
        // calling SseEventSink.close() (IllegalStateException) on the server
        // side).
        if (throwable instanceof IOException || throwable instanceof IllegalStateException) {
            notifyOnCloseListeners(eventSink);
        }
        onErrorListeners.forEach(consumer -> {
            consumer.accept(eventSink, throwable);
        });
    }

    private void notifyOnCloseListeners(SseEventSink eventSink) {
        // First remove the eventSink from the outputQueue to ensure that
        // concurrent calls to this method will notify listeners only once for a
        // given eventSink instance.
        if (sinks.remove(eventSink)) {
            onCloseListeners.forEach(consumer -> {
                consumer.accept(eventSink);
            });
        }
    }

    private void checkClosed() {
        if (isClosed) {
            throw new IllegalStateException("Broadcaster has been closed");
        }
    }

    @Override
    public synchronized void close() {
        close(true);
    }

    @Override
    public synchronized void close(boolean cascading) {
        if (isClosed) {
            return;
        }
        isClosed = true;
        if (cascading) {
            for (SseEventSink sink : sinks) {
                // this will in turn fire close events to our listeners
                sink.close();
            }
        }
    }

    synchronized void fireClose(SseEventSinkImpl sseEventSink) {
        for (Consumer<SseEventSink> listener : onCloseListeners) {
            listener.accept(sseEventSink);
        }
        if (!isClosed)
            sinks.remove(sseEventSink);
    }
}
