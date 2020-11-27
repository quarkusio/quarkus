package org.jboss.resteasy.reactive.client.impl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.net.impl.ConnectionBase;
import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class MultiInvoker extends AbstractRxInvoker<Multi<?>> {

    private WebTargetImpl target;

    public MultiInvoker(WebTargetImpl target) {
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
        AsyncInvokerImpl invoker = (AsyncInvokerImpl) target.request().rx();
        // FIXME: backpressure setting?
        return Multi.createFrom().emitter(emitter -> {
            RestClientRequestContext restClientRequestContext = invoker.performRequestInternal(name, entity, responseType,
                    false);
            restClientRequestContext.getResult().handle((response, connectionError) -> {
                if (connectionError != null) {
                    emitter.fail(connectionError);
                } else {
                    HttpClientResponse vertxResponse = restClientRequestContext.getVertxClientResponse();
                    // FIXME: this is probably not good enough
                    if (response.getStatus() == 200
                            && MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(response.getMediaType())) {
                        registerForSse(emitter, responseType, response, vertxResponse);
                    } else {
                        // read stuff in chunks
                        registerForChunks(emitter, restClientRequestContext, responseType, response, vertxResponse);
                    }
                    vertxResponse.resume();
                }
                return null;
            });
        });
    }

    private <R> void registerForSse(MultiEmitter<? super R> emitter,
            GenericType<R> responseType,
            Response response,
            HttpClientResponse vertxResponse) {
        // honestly, isn't reconnect contradictory with completion?
        // FIXME: Reconnect settings?
        // For now we don't want multi to reconnect
        SseEventSourceImpl sseSource = new SseEventSourceImpl(target, Integer.MAX_VALUE, TimeUnit.SECONDS);
        // FIXME: deal with cancellation
        sseSource.register(event -> {
            // DO NOT pass the response mime type because it's SSE: let the event pick between the X-SSE-Content-Type header or
            // the content-type SSE field
            emitter.emit((R) event.readData(responseType));
        }, error -> {
            emitter.fail(error);
        }, () -> {
            emitter.complete();
        });
        sseSource.registerAfterRequest(vertxResponse);
    }

    private <R> void registerForChunks(MultiEmitter<? super R> emitter,
            RestClientRequestContext restClientRequestContext,
            GenericType<R> responseType,
            Response response,
            HttpClientResponse vertxClientResponse) {
        // make sure we get exceptions on the response, like close events, otherwise they
        // will be logged as errors by vertx
        vertxClientResponse.exceptionHandler(t -> {
            if (t == ConnectionBase.CLOSED_EXCEPTION) {
                // we can ignore this one since we registered a closeHandler
            } else {
                emitter.fail(t);
            }
        });
        HttpConnection connection = vertxClientResponse.request().connection();
        // this captures the server closing
        connection.closeHandler(v -> {
            emitter.complete();
        });
        vertxClientResponse.handler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    ByteArrayInputStream in = new ByteArrayInputStream(buffer.getBytes());
                    R item = restClientRequestContext.readEntity(in, responseType, response.getMediaType(),
                            response.getMetadata());
                    emitter.emit(item);
                } catch (Throwable t) {
                    // FIXME: probably close the client too? watch out that it doesn't call our close handler
                    // which calls emitter.complete()
                    emitter.fail(t);
                }
            }
        });
        // this captures the end of the response
        // FIXME: won't this call complete twice()?
        vertxClientResponse.endHandler(v -> {
            emitter.complete();
        });
    }

}
