package io.quarkus.resteasy.mutiny.common.runtime;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.SyncInvoker;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.plugins.providers.sse.InboundSseEventImpl;
import org.jboss.resteasy.plugins.providers.sse.client.SseEventSourceImpl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.BackPressureStrategy;

public class MultiRxInvokerImpl implements MultiRxInvoker {

    private static Object monitor = new Object();
    private ClientInvocationBuilder syncInvoker;
    private ScheduledExecutorService executorService;
    private BackPressureStrategy backpressureStrategy = BackPressureStrategy.BUFFER;

    public MultiRxInvokerImpl(final SyncInvoker syncInvoker, final ExecutorService executorService) {
        if (!(syncInvoker instanceof ClientInvocationBuilder)) {
            throw new ProcessingException("Expected a ClientInvocationBuilder");
        }
        this.syncInvoker = (ClientInvocationBuilder) syncInvoker;
        if (executorService instanceof ScheduledExecutorService) {
            this.executorService = (ScheduledExecutorService) executorService;
        }
    }

    @Override
    public BackPressureStrategy getBackPressureStrategy() {
        return backpressureStrategy;
    }

    @Override
    public void setBackPressureStrategy(BackPressureStrategy strategy) {
        this.backpressureStrategy = strategy;
    }

    @Override
    public Multi<?> get() {
        return eventSourceToMulti(getEventSource(), String.class, "GET", null, getAccept());
    }

    @Override
    public <R> Multi<?> get(Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "GET", null, getAccept());
    }

    @Override
    public <R> Multi<?> get(GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "GET", null, getAccept());
    }

    @Override
    public Multi<?> put(Entity<?> entity) {
        return eventSourceToMulti(getEventSource(), String.class, "PUT", entity, getAccept());
    }

    @Override
    public <R> Multi<?> put(Entity<?> entity, Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "PUT", entity, getAccept());
    }

    @Override
    public <R> Multi<?> put(Entity<?> entity, GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "PUT", entity, getAccept());
    }

    @Override
    public Multi<?> post(Entity<?> entity) {
        return eventSourceToMulti(getEventSource(), String.class, "POST", entity, getAccept());
    }

    @Override
    public <R> Multi<?> post(Entity<?> entity, Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "POST", entity, getAccept());
    }

    @Override
    public <R> Multi<?> post(Entity<?> entity, GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "POST", entity, getAccept());
    }

    @Override
    public Multi<?> delete() {
        return eventSourceToMulti(getEventSource(), String.class, "DELETE", null, getAccept());
    }

    @Override
    public <R> Multi<?> delete(Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "DELETE", null, getAccept());
    }

    @Override
    public <R> Multi<?> delete(GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "DELETE", null, getAccept());
    }

    @Override
    public Multi<?> head() {
        return eventSourceToMulti(getEventSource(), String.class, "HEAD", null, getAccept());
    }

    @Override
    public Multi<?> options() {
        return eventSourceToMulti(getEventSource(), String.class, "OPTIONS", null, getAccept());
    }

    @Override
    public <R> Multi<?> options(Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "OPTIONS", null, getAccept());
    }

    @Override
    public <R> Multi<?> options(GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "OPTIONS", null, getAccept());
    }

    @Override
    public Multi<?> trace() {
        return eventSourceToMulti(getEventSource(), String.class, "TRACE", null, getAccept());
    }

    @Override
    public <R> Multi<?> trace(Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "TRACE", null, getAccept());
    }

    @Override
    public <R> Multi<?> trace(GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, "TRACE", null, getAccept());
    }

    @Override
    public Multi<?> method(String name) {
        return eventSourceToMulti(getEventSource(), String.class, name, null, getAccept());
    }

    @Override
    public <R> Multi<?> method(String name, Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, name, null, getAccept());
    }

    @Override
    public <R> Multi<?> method(String name, GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, name, null, getAccept());
    }

    @Override
    public Multi<?> method(String name, Entity<?> entity) {
        return eventSourceToMulti(getEventSource(), String.class, name, entity, getAccept());
    }

    @Override
    public <R> Multi<?> method(String name, Entity<?> entity, Class<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, name, entity, getAccept());
    }

    @Override
    public <R> Multi<?> method(String name, Entity<?> entity, GenericType<R> responseType) {
        return eventSourceToMulti(getEventSource(), responseType, name, entity, getAccept());
    }

    private <T> Multi<T> eventSourceToMulti(SseEventSourceImpl sseEventSource, Class<T> clazz, String verb,
            Entity<?> entity, MediaType[] mediaTypes) {
        return eventSourceToMulti(
                sseEventSource,
                (InboundSseEventImpl e) -> e.readData(clazz, e.getMediaType()),
                verb,
                entity,
                mediaTypes);
    }

    private <T> Multi<T> eventSourceToMulti(SseEventSourceImpl sseEventSource, GenericType<T> type, String verb,
            Entity<?> entity, MediaType[] mediaTypes) {
        return eventSourceToMulti(
                sseEventSource,
                (InboundSseEventImpl e) -> e.readData(type, e.getMediaType()),
                verb,
                entity,
                mediaTypes);
    }

    private <T> Multi<T> eventSourceToMulti(
            final SseEventSourceImpl sseEventSource,
            final Function<InboundSseEventImpl, T> tSupplier,
            final String verb,
            final Entity<?> entity,
            final MediaType[] mediaTypes) {
        final Multi<T> multi = Multi.createFrom().emitter(emitter -> {
            sseEventSource.register(
                    (InboundSseEvent e) -> emitter.emit(tSupplier.apply((InboundSseEventImpl) e)),
                    (Throwable t) -> emitter.fail(t),
                    () -> emitter.complete());
            synchronized (monitor) {
                if (!sseEventSource.isOpen()) {
                    sseEventSource.open(null, verb, entity, mediaTypes);
                }
            }
        },
                backpressureStrategy);
        return multi;
    }

    private SseEventSourceImpl getEventSource() {
        SseEventSourceImpl.SourceBuilder builder = (SseEventSourceImpl.SourceBuilder) SseEventSource
                .target(syncInvoker.getTarget());
        if (executorService != null) {
            builder.executor(executorService);
        }
        SseEventSourceImpl sseEventSource = (SseEventSourceImpl) builder.alwaysReconnect(false).build();
        return sseEventSource;
    }

    private MediaType[] getAccept() {
        if (syncInvoker != null) {
            ClientInvocationBuilder builder = syncInvoker;
            List<MediaType> accept = builder.getHeaders().getAcceptableMediaTypes();
            return accept.toArray(new MediaType[0]);
        } else {
            return null;
        }
    }
}
