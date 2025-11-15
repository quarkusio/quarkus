package org.jboss.resteasy.reactive.client.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.client.SseEventFilter;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.reactive.common.util.RestMediaType;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.impl.ConnectionBase;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.core.parsetools.impl.RecordParserImpl;

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
     * after the subscription is cancelled, and we never get notified
     * See <a href="https://github.com/smallrye/smallrye-mutiny/issues/417">...</a>
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
                            registerForSse(
                                    multiRequest, responseType, vertxResponse,
                                    (String) restClientRequestContext.getProperties()
                                            .get(RestClientRequestContext.DEFAULT_CONTENT_TYPE_PROP),
                                    restClientRequestContext.getInvokedMethod());
                        } else if (response.getStatus() == 200
                                && isNewlineDelimited(response)) {
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <R> void registerForSse(MultiRequest<? super R> multiRequest,
            GenericType<R> responseType,
            HttpClientResponse vertxResponse, String defaultContentType,
            Method invokedMethod) {

        boolean returnSseEvent = SseEvent.class.equals(responseType.getRawType());
        GenericType responseTypeFirstParam = responseType.getType() instanceof ParameterizedType
                ? new GenericType(((ParameterizedType) responseType.getType()).getActualTypeArguments()[0])
                : null;

        Predicate<SseEvent<String>> eventPredicate = createEventPredicate(invokedMethod);

        // honestly, isn't reconnect contradictory with completion?
        // FIXME: Reconnect settings?
        // For now we don't want multi to reconnect
        SseEventSourceImpl sseSource = new SseEventSourceImpl(invocationBuilder.getTarget(),
                invocationBuilder, Integer.MAX_VALUE, TimeUnit.SECONDS, defaultContentType);

        multiRequest.onCancel(sseSource::close);
        sseSource.register(event -> {

            // TODO: we might want to cut down on the allocations here...

            if (eventPredicate != null) {
                boolean keep = eventPredicate.test(new SseEvent<>() {
                    @Override
                    public String id() {
                        return event.getId();
                    }

                    @Override
                    public String name() {
                        return event.getName();
                    }

                    @Override
                    public String comment() {
                        return event.getComment();
                    }

                    @Override
                    public String data() {
                        return event.readData();
                    }
                });
                if (!keep) {
                    return;
                }
            }

            // DO NOT pass the response mime type because it's SSE: let the event pick between the X-SSE-Content-Type header or
            // the content-type SSE field

            if (returnSseEvent) {
                multiRequest.emit((R) new SseEvent() {
                    @Override
                    public String id() {
                        return event.getId();
                    }

                    @Override
                    public String name() {
                        return event.getName();
                    }

                    @Override
                    public String comment() {
                        return event.getComment();
                    }

                    @Override
                    public Object data() {
                        if (responseTypeFirstParam != null) {
                            return event.readData(responseTypeFirstParam);
                        } else {
                            return event.readData(); // TODO: is this correct?
                        }
                    }
                });
            } else {
                R item = event.readData(responseType);
                if (item != null) { // we don't emit null because it breaks Multi (by design)
                    multiRequest.emit(item);
                }
            }

        }, multiRequest::fail, multiRequest::complete);
        // watch for user cancelling
        sseSource.registerAfterRequest(vertxResponse);
    }

    private Predicate<SseEvent<String>> createEventPredicate(Method invokedMethod) {
        if (invokedMethod == null) {
            return null; // should never happen
        }

        SseEventFilter filterAnnotation = invokedMethod.getAnnotation(SseEventFilter.class);
        if (filterAnnotation == null) {
            return null;
        }

        try {
            return filterAnnotation.value().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private <R> void registerForChunks(MultiRequest<? super R> multiRequest,
            RestClientRequestContext restClientRequestContext,
            GenericType<R> responseType,
            ResponseImpl response,
            HttpClientResponse vertxClientResponse) {
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
                    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                    R item = restClientRequestContext.readEntity(
                            in,
                            responseType,
                            mediaType,
                            restClientRequestContext.getMethodDeclaredAnnotationsSafe(),
                            response.getMetadata());
                    multiRequest.emitter.emit(item);

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
        Buffer delimiter = RecordParserImpl.latin1StringToBytes("\n");
        RecordParser parser = RecordParser.newDelimited(delimiter);
        AtomicReference<Promise> finalDelimiterHandled = new AtomicReference<>();
        parser.handler(new Handler<>() {
            @Override
            public void handle(Buffer chunk) {

                ByteArrayInputStream in = new ByteArrayInputStream(chunk.getBytes());
                try {
                    if (chunk.length() > 0) {
                        R item = restClientRequestContext.readEntity(in,
                                responseType,
                                response.getMediaType(),
                                restClientRequestContext.getMethodDeclaredAnnotationsSafe(),
                                response.getMetadata());
                        multiRequest.emit(item);
                    }
                } catch (IOException e) {
                    multiRequest.fail(e);
                } finally {
                    if (finalDelimiterHandled.get() != null) {
                        // in this case we know that we have handled the last event, so we need to
                        // signal completion so the Multi can be closed
                        finalDelimiterHandled.get().complete();
                    }
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
        vertxClientResponse.endHandler(new Handler<>() {
            @Override
            public void handle(Void c) {
                // Before closing the Multi, we need to make sure that the parser has emitted the last event.
                // Recall that the parser is delimited, which means that won't emit an event until the delimiter is reached
                // To force the parser to emit the last event we push a delimiter value and when we are sure that the Multi
                // has pushed it down the pipeline, only then do we close it
                Promise<Object> promise = Promise.promise();
                promise.future().onComplete(new Handler<>() {
                    @Override
                    public void handle(AsyncResult<Object> event) {
                        multiRequest.complete();
                    }
                });
                finalDelimiterHandled.set(promise);

                // this needs to happen after the promise has been set up, otherwise, the parser's handler could complete
                // before the finalDelimiterHandled has been populated
                parser.handle(delimiter);
            }
        });

        vertxClientResponse.handler(parser);

        // watch for user cancelling
        multiRequest.onCancel(() -> {
            vertxClientResponse.request().connection().close();
        });
    }

}
