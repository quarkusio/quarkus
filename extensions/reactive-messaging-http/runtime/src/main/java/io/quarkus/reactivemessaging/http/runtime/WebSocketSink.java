package io.quarkus.reactivemessaging.http.runtime;

import static java.util.Arrays.asList;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.logging.Logger;

import io.quarkus.reactivemessaging.http.runtime.serializers.Serializer;
import io.quarkus.reactivemessaging.http.runtime.serializers.SerializerFactoryBase;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniRetry;
import io.smallrye.mutiny.vertx.AsyncResultUni;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;

class WebSocketSink {

    private static final Logger log = Logger.getLogger(WebSocketSink.class);

    private static final String WSS = "wss";
    private static final List<String> supportedSchemes = asList("ws", WSS);

    private final URI uri;
    private final HttpClient httpClient;
    private final SubscriberBuilder<Message<?>, Void> subscriber;
    private final boolean ssl;
    private final String serializer;
    private final SerializerFactoryBase serializerFactory;

    WebSocketSink(Vertx vertx, URI uri, String serializer, SerializerFactoryBase serializerFactory,
            int maxRetries, Optional<Duration> delay, double jitter) {
        this.uri = uri;
        this.serializerFactory = serializerFactory;
        this.serializer = serializer;

        String scheme = uri.getScheme().toLowerCase(Locale.getDefault());
        if (!supportedSchemes.contains(scheme)) {
            throw new IllegalArgumentException("Invalid scheme '" + scheme + "' for the websocket sink URL: " + uri);
        }
        ssl = WSS.equals(scheme);

        httpClient = vertx.createHttpClient();
        subscriber = ReactiveStreams.<Message<?>> builder()
                .flatMapCompletionStage(m -> {
                    Uni<Void> send = send(m);

                    log.debugf("maxRetries: %d", maxRetries);
                    if (maxRetries > 0) {
                        UniRetry<Void> retry = send.onFailure().retry();
                        if (delay.isPresent()) {
                            retry = retry.withBackOff(delay.get()).withJitter(jitter);
                        }
                        send = retry.atMost(maxRetries);
                    }

                    return send
                            .onItemOrFailure().transformToUni(
                                    (result, error) -> {
                                        if (error != null) {
                                            return Uni.createFrom().completionStage(m.nack(error).thenApply(x -> {
                                                log.debug("error responding", error);
                                                return m;
                                            }));
                                        }
                                        return Uni.createFrom().completionStage(m.ack().thenApply(x -> {
                                            log.debug("responded with success", error);
                                            return m;
                                        }));
                                    })
                            .subscribeAsCompletionStage();
                })
                .ignore();
    }

    private final AtomicReference<WebSocket> websocket = new AtomicReference<>();

    private void connect(WebSocketConnectOptions options, Handler<AsyncResult<WebSocket>> handler) {
        log.debug("using a new web socket connection");
        httpClient.webSocket(options, connectResult -> {
            if (connectResult.succeeded()) {
                WebSocket result = connectResult.result();
                WebSocket oldWs = websocket.getAndSet(result);
                if (oldWs != null) { // someone might have initialized it in parallel
                    log.debug("Closing previous web socket connection");
                    oldWs.close();
                }

                result.closeHandler(ignored -> {
                    log.debug("WebSocket disconnected");
                    websocket.compareAndSet(result, null);
                });
                handler.handle(connectResult);
            } else {
                handler.handle(Future.failedFuture(connectResult.cause()));
            }
        });
    }

    private Uni<Void> send(Message<?> message) {
        WebSocketConnectOptions options = options();
        Serializer<Object> serializer = serializerFactory.getSerializer(this.serializer, message.getPayload());
        Buffer serialized = serializer.serialize(message.getPayload());

        return AsyncResultUni.toUni(
                // all happening in "one step" so that the retry mechanism is applied to the connection too
                handler -> {
                    WebSocket ws = websocket.get();
                    if (ws != null && !ws.isClosed()) {
                        log.debug("reusing a previous web socket connection");
                        _send(ws, serialized, handler);
                    } else {
                        connect(options, result -> {
                            if (result.succeeded()) {
                                _send(result.result(), serialized, handler);
                            } else {
                                handler.handle(Future.failedFuture(result.cause()));
                            }
                        });
                    }
                });
    }

    private WebSocketConnectOptions options() {
        return new WebSocketConnectOptions()
                .setSsl(ssl)
                .setHost(uri.getHost())
                .setPort(uri.getPort())
                .setURI(uri.getPath());
    }

    private void _send(WebSocket webSocket, Buffer serialized, Handler<AsyncResult<Void>> handler) {
        log.debug("sending out the message");
        webSocket.write(serialized, writeResult -> {
            if (writeResult.succeeded()) {
                log.debug("success");
            } else {
                Throwable cause = writeResult.cause();
                log.debug("failure", cause);
            }
            handler.handle(writeResult);
        });
    }

    SubscriberBuilder<Message<?>, Void> sink() {
        return subscriber;
    }
}
