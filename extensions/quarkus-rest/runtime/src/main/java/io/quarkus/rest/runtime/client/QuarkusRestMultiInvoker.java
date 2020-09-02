package io.quarkus.rest.runtime.client;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import io.smallrye.mutiny.Multi;

public class QuarkusRestMultiInvoker extends AbstractRxInvoker<Multi<?>> {

    private WebTarget target;

    public QuarkusRestMultiInvoker(WebTarget target) {
        this.target = target;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Multi<R> get(Class<R> responseType) {
        return (Multi<R>) super.get(responseType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Multi<R> get(GenericType<R> responseType) {
        return (Multi<R>) super.get(responseType);
    }

    @Override
    public <R> Multi<R> method(String name, Entity<?> entity, GenericType<R> responseType) {
        if (!name.equals("GET"))
            throw new IllegalStateException("Only GET will get you an SSE stream");
        if (entity != null)
            throw new IllegalStateException("Cannot send stuff over the SSE client");
        // FIXME: backpressure setting?
        return Multi.createFrom().emitter(emitter -> {
            // FIXME: Reconnect settings?
            // honestly, isn't reconnect contradictory with completion?
            QuarkusRestSseEventSource sseSource = new QuarkusRestSseEventSource(target, 0, null);
            sseSource.register(event -> {
                // FIXME: non-String
                emitter.emit((R) event.readData());
            }, error -> {
                emitter.fail(error);
            }, () -> {
                emitter.complete();
            });
            sseSource.open();
        });
    }

}
