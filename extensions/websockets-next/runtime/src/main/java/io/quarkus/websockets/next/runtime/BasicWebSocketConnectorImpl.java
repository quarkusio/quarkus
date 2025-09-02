package io.quarkus.websockets.next.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Typed;

import org.jboss.logging.Logger;

import io.quarkus.tls.TlsConfigurationRegistry;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.runtime.config.WebSocketsClientRuntimeConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.http.WebSocketFrameType;
import io.vertx.core.impl.ContextImpl;
import io.vertx.core.impl.VertxImpl;

@Typed(BasicWebSocketConnector.class)
@Dependent
public class BasicWebSocketConnectorImpl extends WebSocketConnectorBase<BasicWebSocketConnectorImpl>
        implements BasicWebSocketConnector {

    private static final Logger LOG = Logger.getLogger(BasicWebSocketConnectorImpl.class);

    // mutable state

    private ExecutionModel executionModel = ExecutionModel.BLOCKING;

    private Consumer<WebSocketClientConnection> openHandler;

    private BiConsumer<WebSocketClientConnection, String> textMessageHandler;

    private BiConsumer<WebSocketClientConnection, Buffer> binaryMessageHandler;

    private BiConsumer<WebSocketClientConnection, Buffer> pingMessageHandler;

    private BiConsumer<WebSocketClientConnection, Buffer> pongMessageHandler;

    private BiConsumer<WebSocketClientConnection, CloseReason> closeHandler;

    private BiConsumer<WebSocketClientConnection, Throwable> errorHandler;

    BasicWebSocketConnectorImpl(Vertx vertx, Codecs codecs, ClientConnectionManager connectionManager,
            WebSocketsClientRuntimeConfig config, TlsConfigurationRegistry tlsConfigurationRegistry) {
        super(vertx, codecs, connectionManager, config, tlsConfigurationRegistry);
    }

    @Override
    public BasicWebSocketConnector executionModel(ExecutionModel model) {
        this.executionModel = Objects.requireNonNull(model);
        return self();
    }

    @Override
    public BasicWebSocketConnector path(String path) {
        setPath(Objects.requireNonNull(path));
        return self();
    }

    @Override
    public BasicWebSocketConnector onOpen(Consumer<WebSocketClientConnection> consumer) {
        this.openHandler = Objects.requireNonNull(consumer);
        return self();
    }

    @Override
    public BasicWebSocketConnector onTextMessage(BiConsumer<WebSocketClientConnection, String> consumer) {
        this.textMessageHandler = Objects.requireNonNull(consumer);
        return self();
    }

    @Override
    public BasicWebSocketConnector onBinaryMessage(BiConsumer<WebSocketClientConnection, Buffer> consumer) {
        this.binaryMessageHandler = Objects.requireNonNull(consumer);
        return self();
    }

    @Override
    public BasicWebSocketConnector onPing(BiConsumer<WebSocketClientConnection, Buffer> consumer) {
        this.pingMessageHandler = Objects.requireNonNull(consumer);
        return self();
    }

    @Override
    public BasicWebSocketConnector onPong(BiConsumer<WebSocketClientConnection, Buffer> consumer) {
        this.pongMessageHandler = Objects.requireNonNull(consumer);
        return self();
    }

    @Override
    public BasicWebSocketConnector onClose(BiConsumer<WebSocketClientConnection, CloseReason> consumer) {
        this.closeHandler = Objects.requireNonNull(consumer);
        return self();
    }

    @Override
    public BasicWebSocketConnector onError(BiConsumer<WebSocketClientConnection, Throwable> consumer) {
        this.errorHandler = Objects.requireNonNull(consumer);
        return self();
    }

    @Override
    public Uni<WebSocketClientConnection> connect() {
        if (baseUri == null) {
            throw new WebSocketClientException("Endpoint URI not set!");
        }

        // A new client is created for each connection
        // The client is created when the returned Uni is subscribed
        // The client is closed when the connection is closed
        AtomicReference<WebSocketClient> client = new AtomicReference<>();

        WebSocketConnectOptions connectOptions = newConnectOptions(baseUri);
        StringBuilder requestUri = new StringBuilder();
        String mergedPath = mergePath(baseUri.getPath(), replacePathParameters(path));
        requestUri.append(mergedPath);
        if (baseUri.getQuery() != null) {
            requestUri.append("?").append(baseUri.getQuery());
        }
        connectOptions.setURI(requestUri.toString());
        for (Entry<String, List<String>> e : headers.entrySet()) {
            for (String val : e.getValue()) {
                connectOptions.addHeader(e.getKey(), val);
            }
        }
        subprotocols.forEach(connectOptions::addSubProtocol);

        URI serverEndpointUri;
        try {
            serverEndpointUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(),
                    mergedPath,
                    baseUri.getQuery(), baseUri.getFragment());
        } catch (URISyntaxException e) {
            throw new WebSocketClientException(e);
        }

        Uni<WebSocketOpen> websocketOpen = Uni.createFrom().<WebSocketOpen> emitter(e -> {
            // Create a new event loop context for each client, otherwise the current context is used
            // We want to avoid a situation where if multiple clients/connections are created in a row,
            // the same event loop is used and so writing/receiving messages is de-facto serialized
            // Get rid of this workaround once https://github.com/eclipse-vertx/vert.x/issues/5366 is resolved
            ContextImpl context = ((VertxImpl) vertx).createEventLoopContext();
            context.dispatch(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    try {
                        WebSocketClient c = vertx.createWebSocketClient(populateClientOptions());
                        client.setPlain(c);
                        c.connect(connectOptions, new Handler<AsyncResult<WebSocket>>() {
                            @Override
                            public void handle(AsyncResult<WebSocket> r) {
                                if (r.succeeded()) {
                                    e.complete(new WebSocketOpen(newCleanupConsumer(c, context), r.result()));
                                } else {
                                    e.fail(r.cause());
                                }
                            }
                        });
                    } catch (RuntimeException re) {
                        e.fail(re);
                    }
                }
            });
        });
        return websocketOpen.map(wsOpen -> {
            WebSocket ws = wsOpen.websocket();
            String clientId = BasicWebSocketConnector.class.getName();
            TrafficLogger trafficLogger = TrafficLogger.forClient(config);
            WebSocketClientConnectionImpl connection = new WebSocketClientConnectionImpl(clientId,
                    ws,
                    codecs,
                    pathParams,
                    serverEndpointUri,
                    headers,
                    trafficLogger,
                    userData,
                    null,
                    wsOpen.cleanup());
            if (trafficLogger != null) {
                trafficLogger.connectionOpened(connection);
            }
            connectionManager.add(BasicWebSocketConnectorImpl.class.getName(), connection);

            if (openHandler != null) {
                doExecute(connection, null, (c, ignored) -> openHandler.accept(c));
            }

            if (textMessageHandler != null) {
                ws.textMessageHandler(new Handler<String>() {
                    @Override
                    public void handle(String message) {
                        if (trafficLogger != null) {
                            trafficLogger.textMessageReceived(connection, message);
                        }
                        doExecute(connection, message, textMessageHandler);
                    }
                });
            }

            if (binaryMessageHandler != null) {
                ws.binaryMessageHandler(new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer message) {
                        if (trafficLogger != null) {
                            trafficLogger.binaryMessageReceived(connection, message);
                        }
                        doExecute(connection, message, binaryMessageHandler);
                    }
                });
            }

            if (pingMessageHandler != null) {
                ws.frameHandler(new Handler<WebSocketFrame>() {

                    @Override
                    public void handle(WebSocketFrame frame) {
                        if (frame.type() == WebSocketFrameType.PING) {
                            doExecute(connection, frame.binaryData(), pingMessageHandler);
                        }
                    }
                });
            }

            if (pongMessageHandler != null) {
                ws.pongHandler(new Handler<Buffer>() {

                    @Override
                    public void handle(Buffer event) {
                        doExecute(connection, event, pongMessageHandler);
                    }
                });
            }

            if (errorHandler != null) {
                ws.exceptionHandler(new Handler<Throwable>() {

                    @Override
                    public void handle(Throwable event) {
                        doExecute(connection, event, errorHandler);
                    }
                });
            }

            ws.closeHandler(new Handler<Void>() {

                @Override
                public void handle(Void event) {
                    if (trafficLogger != null) {
                        trafficLogger.connectionClosed(connection);
                    }
                    if (closeHandler != null) {
                        CloseReason reason = CloseReason.INTERNAL_SERVER_ERROR;
                        if (ws.closeStatusCode() != null) {
                            reason = new CloseReason(ws.closeStatusCode(), ws.closeReason());
                        }
                        doExecute(connection, reason, closeHandler);
                    }
                    connectionManager.remove(BasicWebSocketConnectorImpl.class.getName(), connection);
                    client.get().close();
                }

            });

            return connection;
        });
    }

    private <MESSAGE> void doExecute(WebSocketClientConnectionImpl connection, MESSAGE message,
            BiConsumer<WebSocketClientConnection, MESSAGE> consumer) {
        // We always invoke callbacks on a new duplicated context and offload if blocking/virtualThread is needed
        Context context = vertx.getOrCreateContext();
        ContextSupport.createNewDuplicatedContext(context, connection).runOnContext(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                if (executionModel == ExecutionModel.VIRTUAL_THREAD) {
                    VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                        public void run() {
                            try {
                                consumer.accept(connection, message);
                            } catch (Exception e) {
                                LOG.errorf(e, "Unable to call handler: " + connection);
                            }
                        }
                    });
                } else if (executionModel == ExecutionModel.BLOCKING) {
                    vertx.executeBlocking(new Callable<Void>() {
                        @Override
                        public Void call() {
                            try {
                                consumer.accept(connection, message);
                            } catch (Exception e) {
                                LOG.errorf(e, "Unable to call handler: " + connection);
                            }
                            return null;
                        }
                    }, false);
                } else {
                    // Non-blocking -> event loop
                    try {
                        consumer.accept(connection, message);
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to call handler: " + connection);
                    }
                }
            }
        });
    }

    private String mergePath(String path1, String path2) {
        StringBuilder ret = new StringBuilder();
        if (path1 != null) {
            ret.append(path1);
        }
        if (path2 != null) {
            if (path1.endsWith("/")) {
                if (path2.startsWith("/")) {
                    ret.append(path2.substring(1));
                } else {
                    ret.append(path2);
                }
            } else {
                if (path2.startsWith("/")) {
                    ret.append(path2);
                } else {
                    ret.append("/").append(path2);
                }
            }
        }
        return ret.toString();
    }

}
