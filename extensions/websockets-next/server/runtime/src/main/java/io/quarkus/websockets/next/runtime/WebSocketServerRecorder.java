package io.quarkus.websockets.next.runtime;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.SessionScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.WebSocketsRuntimeConfig;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.MessageType;
import io.quarkus.websockets.next.runtime.WebSocketSessionContext.SessionContextState;
import io.smallrye.common.vertx.VertxContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class WebSocketServerRecorder {

    private static final Logger LOG = Logger.getLogger(WebSocketServerRecorder.class);

    static final String WEB_SOCKET_CONN_KEY = WebSocketServerConnection.class.getName();

    private final WebSocketsRuntimeConfig config;

    public WebSocketServerRecorder(WebSocketsRuntimeConfig config) {
        this.config = config;
    }

    public Supplier<Object> connectionSupplier() {
        return new Supplier<Object>() {

            @Override
            public Object get() {
                Context context = Vertx.currentContext();
                if (context != null && VertxContext.isDuplicatedContext(context)) {
                    Object connection = context.getLocal(WEB_SOCKET_CONN_KEY);
                    if (connection != null) {
                        return connection;
                    }
                }
                throw new WebSocketServerException("Unable to obtain the connection from the Vert.x duplicated context");
            }
        };
    }

    public Handler<RoutingContext> createEndpointHandler(String endpointClass) {
        ArcContainer container = Arc.container();
        ConnectionManager connectionManager = container.instance(ConnectionManager.class).get();
        Codecs codecs = container.instance(Codecs.class).get();
        return new Handler<RoutingContext>() {

            @Override
            public void handle(RoutingContext ctx) {
                Future<ServerWebSocket> future = ctx.request().toWebSocket();
                future.onSuccess(ws -> {
                    Context context = VertxCoreRecorder.getVertx().get().getOrCreateContext();

                    WebSocketServerConnection connection = new WebSocketServerConnectionImpl(endpointClass, ws,
                            connectionManager, codecs, ctx);
                    connectionManager.add(endpointClass, connection);
                    LOG.debugf("Connnected: %s", connection);

                    // Initialize and capture the session context state that will be activated
                    // during message processing
                    WebSocketSessionContext sessionContext = sessionContext(container);
                    SessionContextState sessionContextState = sessionContext.initializeContextState();
                    ContextSupport contextSupport = new ContextSupport(connection, sessionContextState,
                            sessionContext(container),
                            container.requestContext());

                    // Create an endpoint that delegates callbacks to the @WebSocket bean
                    WebSocketEndpoint endpoint = createEndpoint(endpointClass, context, connection, codecs, config,
                            contextSupport);

                    // The processor is only needed if Multi is consumed by the @OnMessage callback
                    BroadcastProcessor<Object> broadcastProcessor = endpoint.consumedMultiType() != null
                            ? BroadcastProcessor.create()
                            : null;

                    // NOTE: we always invoke callbacks (onOpen, onMessage, onClose) on a new duplicated context
                    // and the endpoint is responsible to make the switch if blocking/virtualThread

                    Context onOpenContext = ContextSupport.createNewDuplicatedContext(context, connection);
                    onOpenContext.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            endpoint.onOpen().onComplete(r -> {
                                if (r.succeeded()) {
                                    LOG.debugf("@OnOpen callback completed: %s", connection);
                                    if (broadcastProcessor != null) {
                                        // If Multi is consumed we need to invoke @OnMessage callback eagerly
                                        // but after @OnOpen completes
                                        Multi<Object> multi = broadcastProcessor.onCancellation().call(connection::close);
                                        onOpenContext.runOnContext(new Handler<Void>() {
                                            @Override
                                            public void handle(Void event) {
                                                endpoint.onMessage(multi).onComplete(r -> {
                                                    if (r.succeeded()) {
                                                        LOG.debugf("@OnMessage callback consuming Multi completed: %s",
                                                                connection);
                                                    } else {
                                                        LOG.errorf(r.cause(),
                                                                "Unable to complete @OnMessage callback consuming Multi: %s",
                                                                connection);
                                                    }
                                                });
                                            }
                                        });
                                    }
                                } else {
                                    LOG.errorf(r.cause(), "Unable to complete @OnOpen callback: %s", connection);
                                }
                            });
                        }
                    });

                    if (broadcastProcessor == null) {
                        // Multi not consumed - invoke @OnMessage callback for each message received
                        messageHandlers(connection, endpoint, ws, context, m -> {
                            endpoint.onMessage(m).onComplete(r -> {
                                if (r.succeeded()) {
                                    LOG.debugf("@OnMessage callback consumed binary message: %s", connection);
                                } else {
                                    LOG.errorf(r.cause(), "Unable to consume binary message in @OnMessage callback: %s",
                                            connection);
                                }
                            });
                        }, m -> {
                            endpoint.onMessage(m).onComplete(r -> {
                                if (r.succeeded()) {
                                    LOG.debugf("@OnMessage callback consumed text message: %s", connection);
                                } else {
                                    LOG.errorf(r.cause(), "Unable to consume text message in @OnMessage callback: %s",
                                            connection);
                                }
                            });
                        }, true);
                    } else {
                        // Multi consumed - forward message to subcribers
                        messageHandlers(connection, endpoint, ws, onOpenContext, m -> {
                            contextSupport.start();
                            broadcastProcessor.onNext(endpoint.decodeMultiItem(m));
                            LOG.debugf("Binary message >> Multi: %s", connection);
                            contextSupport.end(false);
                        }, m -> {
                            contextSupport.start();
                            broadcastProcessor.onNext(endpoint.decodeMultiItem(m));
                            LOG.debugf("Text message >> Multi: %s", connection);
                            contextSupport.end(false);
                        }, false);
                    }

                    ws.closeHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            ContextSupport.createNewDuplicatedContext(context, connection).runOnContext(new Handler<Void>() {
                                @Override
                                public void handle(Void event) {
                                    endpoint.onClose().onComplete(r -> {
                                        if (r.succeeded()) {
                                            LOG.debugf("@OnClose callback completed: %s", connection);
                                        } else {
                                            LOG.errorf(r.cause(), "Unable to complete @OnClose callback: %s", connection);
                                        }
                                        connectionManager.remove(endpointClass, connection);
                                    });
                                }
                            });
                        }
                    });
                });
            }
        };
    }

    private void messageHandlers(WebSocketServerConnection connection, WebSocketEndpoint endpoint, ServerWebSocket ws,
            Context context, Consumer<Buffer> binaryAction, Consumer<String> textAction, boolean newDuplicatedContext) {
        if (endpoint.consumedMessageType() == MessageType.BINARY) {
            ws.binaryMessageHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer message) {
                    Context duplicatedContext = newDuplicatedContext
                            ? ContextSupport.createNewDuplicatedContext(context, connection)
                            : context;
                    duplicatedContext.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            binaryAction.accept(message);
                        }
                    });
                }
            });
        } else if (endpoint.consumedMessageType() == MessageType.TEXT) {
            ws.textMessageHandler(new Handler<String>() {
                @Override
                public void handle(String message) {
                    Context duplicatedContext = newDuplicatedContext
                            ? ContextSupport.createNewDuplicatedContext(context, connection)
                            : context;
                    duplicatedContext.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            textAction.accept(message);
                        }
                    });
                }
            });
        }
    }

    private WebSocketEndpoint createEndpoint(String endpointClassName, Context context, WebSocketServerConnection connection,
            Codecs codecs, WebSocketsRuntimeConfig config, ContextSupport contextSupport) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = WebSocketServerRecorder.class.getClassLoader();
            }
            @SuppressWarnings("unchecked")
            Class<? extends WebSocketEndpoint> endpointClazz = (Class<? extends WebSocketEndpoint>) cl
                    .loadClass(endpointClassName);
            WebSocketEndpoint endpoint = (WebSocketEndpoint) endpointClazz
                    .getDeclaredConstructor(WebSocketServerConnection.class, Codecs.class,
                            WebSocketsRuntimeConfig.class, ContextSupport.class)
                    .newInstance(connection, codecs, config, contextSupport);
            return endpoint;
        } catch (Exception e) {
            throw new WebSocketServerException("Unable to create endpoint instance: " + endpointClassName, e);
        }
    }

    private static WebSocketSessionContext sessionContext(ArcContainer container) {
        for (InjectableContext injectableContext : container.getContexts(SessionScoped.class)) {
            if (WebSocketSessionContext.class.equals(injectableContext.getClass())) {
                return (WebSocketSessionContext) injectableContext;
            }
        }
        throw new WebSocketServerException("CDI session context not registered");
    }

}
