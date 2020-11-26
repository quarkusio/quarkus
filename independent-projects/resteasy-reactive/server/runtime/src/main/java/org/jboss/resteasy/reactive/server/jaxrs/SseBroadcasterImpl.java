package org.jboss.resteasy.reactive.server.jaxrs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseBroadcaster;
import javax.ws.rs.sse.SseEventSink;

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
        if (sseEventSink instanceof QuarkusRestSseEventSink == false)
            throw new IllegalArgumentException("Can only work with Quarkus-REST instances: " + sseEventSink);
        ((QuarkusRestSseEventSink) sseEventSink).register(this);
        sinks.add(sseEventSink);
    }

    @Override
    public synchronized CompletionStage<?> broadcast(OutboundSseEvent event) {
        Objects.requireNonNull(event);
        checkClosed();
        CompletableFuture<?>[] cfs = new CompletableFuture[sinks.size()];
        for (int i = 0; i < sinks.size(); i++) {
            SseEventSink sseEventSink = sinks.get(i);
            cfs[i] = sseEventSink.send(event).toCompletableFuture();
        }
        return CompletableFuture.allOf(cfs);
    }

    private void checkClosed() {
        if (isClosed)
            throw new IllegalStateException("Broadcaster has been closed");
    }

    @Override
    public synchronized void close() {
        if (isClosed)
            return;
        isClosed = true;
        for (SseEventSink sink : sinks) {
            // this will in turn fire close events to our listeners
            sink.close();
        }
    }

    synchronized void fireClose(QuarkusRestSseEventSink sseEventSink) {
        for (Consumer<SseEventSink> listener : onCloseListeners) {
            listener.accept(sseEventSink);
        }
    }

    synchronized void fireException(QuarkusRestSseEventSink sseEventSink, Throwable t) {
        for (BiConsumer<SseEventSink, Throwable> listener : onErrorListeners) {
            listener.accept(sseEventSink, t);
        }
    }

}
