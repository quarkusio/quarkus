package io.quarkus.websockets.next.runtime;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Typed;

import org.jboss.logging.Logger;

import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.WebSocketsClientRuntimeConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.http.WebSocketConnectOptions;

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

    private BiConsumer<WebSocketClientConnection, Buffer> pongMessageHandler;

    private BiConsumer<WebSocketClientConnection, CloseReason> closeHandler;

    private BiConsumer<WebSocketClientConnection, Throwable> errorHandler;

    BasicWebSocketConnectorImpl(Vertx vertx, Codecs codecs, ClientConnectionManager connectionManager,
            WebSocketsClientRuntimeConfig config) {
        super(vertx, codecs, connectionManager, config);
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

        // Currently we create a new client for each connection
        // The client is closed when the connection is closed
        // TODO would it make sense to share clients?
        WebSocketClientOptions clientOptions = new WebSocketClientOptions();
        if (config.offerPerMessageCompression()) {
            clientOptions.setTryUsePerMessageCompression(true);
            if (config.compressionLevel().isPresent()) {
                clientOptions.setCompressionLevel(config.compressionLevel().getAsInt());
            }
        }
        if (config.maxMessageSize().isPresent()) {
            clientOptions.setMaxMessageSize(config.maxMessageSize().getAsInt());
        }

        WebSocketClient client = vertx.createWebSocketClient();

        WebSocketConnectOptions connectOptions = new WebSocketConnectOptions()
                .setSsl(baseUri.getScheme().equals("https"))
                .setHost(baseUri.getHost())
                .setPort(baseUri.getPort());
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

        return UniHelper.toUni(client.connect(connectOptions))
                .map(ws -> {
                    String clientId = BasicWebSocketConnector.class.getName();
                    WebSocketClientConnectionImpl connection = new WebSocketClientConnectionImpl(clientId, ws,
                            codecs,
                            pathParams,
                            serverEndpointUri,
                            headers);
                    LOG.debugf("Client connection created: %s", connection);
                    connectionManager.add(BasicWebSocketConnectorImpl.class.getName(), connection);

                    if (openHandler != null) {
                        doExecute(connection, null, (c, ignored) -> openHandler.accept(c));
                    }

                    if (textMessageHandler != null) {
                        ws.textMessageHandler(new Handler<String>() {
                            @Override
                            public void handle(String event) {
                                doExecute(connection, event, textMessageHandler);
                            }
                        });
                    }

                    if (binaryMessageHandler != null) {
                        ws.binaryMessageHandler(new Handler<Buffer>() {

                            @Override
                            public void handle(Buffer event) {
                                doExecute(connection, event, binaryMessageHandler);
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
                            if (closeHandler != null) {
                                doExecute(connection, new CloseReason(ws.closeStatusCode(), ws.closeReason()), closeHandler);
                            }
                            connectionManager.remove(BasicWebSocketConnectorImpl.class.getName(), connection);
                            client.close();
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
        StringBuilder path = new StringBuilder();
        if (path1 != null) {
            path.append(path1);
        }
        if (path2 != null) {
            if (path1.endsWith("/")) {
                if (path2.startsWith("/")) {
                    path.append(path2.substring(1));
                } else {
                    path.append(path2);
                }
            } else {
                if (path2.startsWith("/")) {
                    path.append(path2);
                } else {
                    path.append(path2.substring(1));
                }
            }
        }
        return path.toString();
    }

}
