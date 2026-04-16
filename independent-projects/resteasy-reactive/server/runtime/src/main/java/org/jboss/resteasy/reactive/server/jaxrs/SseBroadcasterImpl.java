package org.jboss.resteasy.reactive.server.jaxrs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.logging.Logger;

public class SseBroadcasterImpl implements SseBroadcaster {

    private static final Logger log = Logger.getLogger(SseBroadcasterImpl.class);

    private final ConcurrentLinkedQueue<SseEventSink> outputQueue = new ConcurrentLinkedQueue<>();
    private final List<BiConsumer<SseEventSink, Throwable>> onErrorConsumers = new CopyOnWriteArrayList<>();
    private final List<Consumer<SseEventSink>> closeConsumers = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    // Used to perform a mutual exclusion between register and close operations
    // since every registered SseEventSink needs to be closed when
    // SseBroadcaster.close() is invoked to prevent leaks due to SseEventSink
    // never closed.
    // Actually most of the time when a SseEventSink is registered to a
    // SseBroadcaster, user is expected its termination to be handled by the
    // SseBroadcaster itself. So user will never call SseEventSink.close() on
    // each SseEventSink he registers but instead he will just call
    // SseBroadcaster.close().
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    @Override
    public synchronized void onError(BiConsumer<SseEventSink, Throwable> onError) {
        Objects.requireNonNull(onError);
        checkClosed();
        onErrorConsumers.add(onError);
    }

    @Override
    public synchronized void onClose(Consumer<SseEventSink> onClose) {
        Objects.requireNonNull(onClose);
        checkClosed();
        closeConsumers.add(onClose);
    }

    @Override
    public synchronized void register(SseEventSink sseEventSink) {
        Objects.requireNonNull(sseEventSink);
        checkClosed();
        readLock.lock();
        try {
            checkClosed();
            if (!(sseEventSink instanceof SseEventSinkImpl sinkImpl)) {
                throw new IllegalArgumentException("Can only work with Quarkus-REST instances: " + sseEventSink);
            }
            sinkImpl.register(this);
            outputQueue.add(sseEventSink);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public synchronized CompletionStage<?> broadcast(OutboundSseEvent event) {
        Objects.requireNonNull(event);
        checkClosed();

        List<CompletableFuture<?>> cfs = new ArrayList<>(outputQueue.size());
        for (SseEventSink eventSink : outputQueue) {
            CompletionStage<?> cs;
            try {
                CompletionStage<?> sendStage = eventSink.send(event);

                cs = sendStage.exceptionally((err) -> {
                    // do not propagate the exception to the returned CF
                    // apparently, the goal is to close this sink and not report the error
                    // of the broadcast operation
                    Throwable cause = err;
                    while (cause instanceof CompletionException && cause.getCause() != null) {
                        cause = cause.getCause();
                        if (cause instanceof IOException) {
                            try {
                                eventSink.close();
                            } catch (Exception ignore) {
                            }
                            break;
                        }
                    }

                    notifyOnErrorListeners(eventSink, err);
                    return null;
                });
            } catch (Exception e) {
                // do not propagate the exception to the returned CF
                // apparently, the goal is to close this sink and not report the error
                // of the broadcast operation
                notifyOnErrorListeners(eventSink, e);
                cs = CompletableFuture.completedFuture(null);
            }
            cfs.add(cs.toCompletableFuture());
        }
        return CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0]));
    }

    private void notifyOnErrorListeners(SseEventSink eventSink, Throwable throwable) {
        // We have to notify close listeners if the SSE event output has been
        // closed (either by client closing the connection (IOException) or by
        // calling SseEventSink.close() (IllegalStateException) on the server
        // side).
        if (throwable instanceof IOException || throwable instanceof IllegalStateException) {
            notifyOnCloseListeners(eventSink);
        }
        onErrorConsumers.forEach(consumer -> {
            consumer.accept(eventSink, throwable);
        });
    }

    private void notifyOnCloseListeners(SseEventSink eventSink) {
        // First remove the eventSink from the outputQueue to ensure that
        // concurrent calls to this method will notify listeners only once for a
        // given eventSink instance.
        if (outputQueue.remove(eventSink)) {
            closeConsumers.forEach(consumer -> {
                consumer.accept(eventSink);
            });
        }
    }

    private void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Broadcaster has been closed");
        }
    }

    @Override
    public synchronized void close() {
        close(true);
    }

    @Override
    public synchronized void close(final boolean cascading) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (cascading) {
            writeLock.lock();
            try {
                //Javadoc says close the broadcaster and all subscribed {@link SseEventSink} instances.
                //is it necessary to close the subscribed SseEventSink ?
                outputQueue.forEach(eventSink -> {
                    try {
                        eventSink.close();
                    } catch (RuntimeException e) {
                        log.debug(e.getLocalizedMessage());
                    } finally {
                        notifyOnCloseListeners(eventSink);
                    }
                });
            } finally {
                writeLock.unlock();
            }
        }
    }

    synchronized void fireClose(SseEventSinkImpl sseEventSink) {
        for (Consumer<SseEventSink> listener : closeConsumers) {
            listener.accept(sseEventSink);
        }
        if (!closed.get())
            outputQueue.remove(sseEventSink);
    }
}
