package org.jboss.resteasy.reactive.client.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.core.parsetools.RecordParser;

public class MultiInvoker extends AbstractRxInvoker<Multi<?>> {

    private final InvocationBuilderImpl invocationBuilder;

    public MultiInvoker(InvocationBuilderImpl target) {
        this.invocationBuilder = target;
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

    /**
     * We need this class to work around a bug in Mutiny where we can register our cancel listener
     * after the subscription is cancelled and we never get notified
     * See https://github.com/smallrye/smallrye-mutiny/issues/417
     */
    static class MultiRequest<R> {

        private final AtomicReference<Runnable> onCancel = new AtomicReference<>();

        private final MultiEmitter<? super R> emitter;

        private static final Runnable CLEARED = () -> {
        };

        public MultiRequest(MultiEmitter<? super R> emitter) {
            this.emitter = emitter;
            emitter.onTermination(() -> {
                if (emitter.isCancelled()) {
                    this.cancel();
                }
            });
        }

        void emit(R item) {
            if (!isCancelled()) {
                emitter.emit(item);
            }
        }

        void fail(Throwable t) {
            if (!isCancelled()) {
                emitter.fail(t);
                cancel();
            }
        }

        void complete() {
            if (!isCancelled()) {
                emitter.complete();
                cancel();
            }
        }

        public boolean isCancelled() {
            return onCancel.get() == CLEARED;
        }

        private void cancel() {
            Runnable action = onCancel.getAndSet(CLEARED);
            if (action != null && action != CLEARED) {
                action.run();
            }
        }

        public void onCancel(Runnable onCancel) {
            if (this.onCancel.compareAndSet(null, onCancel)) {
                // this was a first set
            } else if (this.onCancel.get() == CLEARED) {
                // already cleared
                if (onCancel != null)
                    onCancel.run();
            } else {
                // it was already set
                throw new IllegalArgumentException("onCancel was already called");
            }
        }
    }

    @Override
    public <R> Multi<R> method(String name, Entity<?> entity, GenericType<R> responseType) {
        AsyncInvokerImpl invoker = (AsyncInvokerImpl) invocationBuilder.rx();
        // FIXME: backpressure setting?
        return Multi.createFrom().emitter(emitter -> {
            MultiRequest<R> multiRequest = new MultiRequest<>(emitter);
            RestClientRequestContext restClientRequestContext = invoker.performRequestInternal(name, entity, responseType,
                    false);
            restClientRequestContext.getResult().handle((response, connectionError) -> {
                if (connectionError != null) {
                    emitter.fail(connectionError);
                } else {
                    HttpClientResponse vertxResponse = restClientRequestContext.getVertxClientResponse();
                    if (!emitter.isCancelled()) {
                        if (response.getStatus() == 200
                                && MediaType.SERVER_SENT_EVENTS_TYPE.isCompatible(response.getMediaType())) {
                            registerForSse(multiRequest, responseType, response, vertxResponse);
                        } else if (response.getStatus() == 200
                                && RestMediaType.APPLICATION_STREAM_JSON_TYPE.isCompatible(response.getMediaType())) {
                            registerForJsonStream(multiRequest, restClientRequestContext, responseType, response,
                                    vertxResponse);
                        } else {
                            // read stuff in chunks
                            registerForChunks(multiRequest, restClientRequestContext, responseType, response, vertxResponse);
                        }
                        vertxResponse.resume();
                    } else {
                        vertxResponse.request().connection().close();
                    }
                }
                return null;
            });
        });
    }

    private boolean isNewlineDelimited(ResponseImpl response) {
        return RestMediaType.APPLICATION_STREAM_JSON_TYPE.isCompatible(response.getMediaType()) ||
                RestMediaType.APPLICATION_NDJSON_TYPE.isCompatible(response.getMediaType());
    }

    private <R> void registerForSse(MultiRequest<? super R> multiRequest,
            GenericType<R> responseType,
            Response response,
            HttpClientResponse vertxResponse) {
        // honestly, isn't reconnect contradictory with completion?
        // FIXME: Reconnect settings?
        // For now we don't want multi to reconnect
        SseEventSourceImpl sseSource = new SseEventSourceImpl(invocationBuilder.getTarget(),
                invocationBuilder, Integer.MAX_VALUE, TimeUnit.SECONDS);

        multiRequest.onCancel(sseSource::close);
        sseSource.register(event -> {
            // DO NOT pass the response mime type because it's SSE: let the event pick between the X-SSE-Content-Type header or
            // the content-type SSE field
            multiRequest.emit(event.readData(responseType));
        }, multiRequest::fail, multiRequest::complete);
        // watch for user cancelling
        sseSource.registerAfterRequest(vertxResponse);
    }

    private <R> void registerForChunks(MultiRequest<? super R> multiRequest,
            RestClientRequestContext restClientRequestContext,
            GenericType<R> responseType,
            ResponseImpl response,
            HttpClientResponse vertxClientResponse) {
        boolean isNewlineDelimited = isNewlineDelimited(response);
        // make sure we get exceptions on the response, like close events, otherwise they
        // will be logged as errors by vertx
        vertxClientResponse.exceptionHandler(t -> {
            if (t == ConnectionBase.CLOSED_EXCEPTION) {
                // we can ignore this one since we registered a closeHandler
            } else {
                multiRequest.emitter.fail(t);
            }
        });
        // we don't add a closeHandler handler on the connection as it can race with this handler
        // and close before the emitter emits anything
        // see: https://github.com/quarkusio/quarkus/pull/16438
        vertxClientResponse.handler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                try {
                    byte[] bytes = buffer.getBytes();
                    MediaType mediaType = response.getMediaType();

                    if (isNewlineDelimited) {
                        String charset = mediaType.getParameters().get(MediaType.CHARSET_PARAMETER);
                        charset = charset == null ? "UTF-8" : charset;
                        byte[] separator = "\n".getBytes(charset);
                        int start = 0;
                        while (start < bytes.length) {
                            int end = bytes.length;
                            for (int i = start; i < end; i++) {
                                if (bytes[i] == separator[0]) {
                                    int j;
                                    boolean matches = true;
                                    for (j = 1; j < separator.length; j++) {
                                        if (bytes[i + j] != separator[j]) {
                                            matches = false;
                                            break;
                                        }
                                    }
                                    if (matches) {
                                        end = i;
                                        break;
                                    }
                                }
                            }

                            if (start < end) {
                                ByteArrayInputStream in = new ByteArrayInputStream(bytes, start, end);
                                R item = restClientRequestContext.readEntity(in, responseType, mediaType,
                                        response.getMetadata());
                                multiRequest.emitter.emit(item);
                            }
                            start = end + separator.length;
                        }
                    } else {
                        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                        R item = restClientRequestContext.readEntity(in, responseType, mediaType,
                                response.getMetadata());
                        multiRequest.emitter.emit(item);
                    }
                } catch (Throwable t) {
                    // FIXME: probably close the client too? watch out that it doesn't call our close handler
                    // which calls emitter.complete()
                    multiRequest.emitter.fail(t);
                }
            }
        });

        // this captures the end of the response
        vertxClientResponse.endHandler(v -> {
            multiRequest.emitter.complete();
        });
        // watch for user cancelling
        multiRequest.onCancel(() -> {
            vertxClientResponse.request().connection().close();
        });
    }

    private <R> void registerForJsonStream(MultiRequest<? super R> multiRequest,
            RestClientRequestContext restClientRequestContext,
            GenericType<R> responseType,
            ResponseImpl response,
            HttpClientResponse vertxClientResponse) {
        RecordParser parser = RecordParser.newDelimited("\n");
        parser.handler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer chunk) {

                ByteArrayInputStream in = new ByteArrayInputStream(chunk.getBytes());
                try {
                    R item = restClientRequestContext.readEntity(in, responseType, response.getMediaType(),
                            response.getMetadata());
                    multiRequest.emit(item);
                } catch (IOException e) {
                    multiRequest.fail(e);
                }
            }
        });
        vertxClientResponse.exceptionHandler(t -> {
            if (t == ConnectionBase.CLOSED_EXCEPTION) {
                // we can ignore this one since we registered a closeHandler
            } else {
                multiRequest.fail(t);
            }
        });
        vertxClientResponse.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void c) {
                multiRequest.complete();
            }
        });

        vertxClientResponse.handler(parser);

        // watch for user cancelling
        multiRequest.onCancel(() -> {
            vertxClientResponse.request().connection().close();
        });
    }

}
