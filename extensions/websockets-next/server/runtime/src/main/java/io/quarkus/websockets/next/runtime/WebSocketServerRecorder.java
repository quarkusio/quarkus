package io.quarkus.websockets.next.runtime;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.context.SessionScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.quarkus.websockets.next.WebSocketRuntimeConfig;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.quarkus.websockets.next.WebSocketServerException;
import io.quarkus.websockets.next.runtime.WebSocketEndpoint.MessageType;
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

    private static final String WEB_SOCKET_CONN_KEY = WebSocketServerConnection.class.getName();

    private final WebSocketRuntimeConfig config;

    public WebSocketServerRecorder(WebSocketRuntimeConfig config) {
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
                throw new WebSocketServerException("Unable to obtain the connection from the Vertx duplicated context");
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
                    // Create a new duplicated context
                    Context context = VertxContext
                            .createNewDuplicatedContext(VertxCoreRecorder.getVertx().get().getOrCreateContext());
                    VertxContextSafetyToggle.setContextSafe(context, true);

                    WebSocketServerConnection connection = new WebSocketServerConnectionImpl(endpointClass, ws,
                            connectionManager,
                            Map.copyOf(ctx.pathParams()), codecs);
                    LOG.debugf("WebSocket connnected: %s", connection);

                    // This is a bit weird but we need to store the connection to initialize the bean later on
                    context.putLocal(WEB_SOCKET_CONN_KEY, connection);
                    connectionManager.add(endpointClass, connection);

                    // Create an endpoint that delegates callbacks to the @WebSocket bean
                    WebSocketEndpoint endpoint = createEndpoint(endpointClass, context, connection, codecs, config);
                    WebSocketSessionContext sessionContext = sessionContext(container);

                    // The processor is only needed if Multi is consumed by the @OnMessage callback
                    BroadcastProcessor<Object> broadcastProcessor = endpoint.consumedMultiType() != null
                            ? BroadcastProcessor.create()
                            : null;

                    // Invoke @OnOpen callback
                    // Note we always run on the duplicated context and the endpoint is responsible to make the switch if blocking/virtualThread
                    context.runOnContext(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            sessionContext.activate();
                            endpoint.onOpen(context).onComplete(r -> {
                                if (r.succeeded()) {
                                    LOG.debugf("@OnOpen callback completed: %s", connection);
                                    if (broadcastProcessor != null) {
                                        // If Multi is consumed we need to invoke @OnMessage callback eagerly
                                        // but after @OnOpen completes
                                        Multi<Object> multi = broadcastProcessor.onCancellation().call(connection::close);
                                        context.runOnContext(new Handler<Void>() {
                                            @Override
                                            public void handle(Void event) {
                                                endpoint.onMessage(context, multi).onComplete(r -> {
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
                        messageHandlers(endpoint, ws, context, m -> {
                            endpoint.onMessage(context, m).onComplete(r -> {
                                if (r.succeeded()) {
                                    LOG.debugf("@OnMessage callback consumed binary message: %s", connection);
                                } else {
                                    LOG.errorf(r.cause(), "Unable to consume binary message in @OnMessage callback: %s",
                                            connection);
                                }
                            });
                        }, m -> {
                            endpoint.onMessage(context, m).onComplete(r -> {
                                if (r.succeeded()) {
                                    LOG.debugf("@OnMessage callback consumed text message: %s", connection);
                                } else {
                                    LOG.errorf(r.cause(), "Unable to consume text message in @OnMessage callback: %s",
                                            connection);
                                }
                            });
                        });
                    } else {
                        // Multi consumed - forward message to subcribers
                        messageHandlers(endpoint, ws, context, m -> {
                            broadcastProcessor.onNext(endpoint.decodeMultiItem(m));
                            LOG.debugf("Binary message >> Multi: %s", connection);
                        }, m -> {
                            broadcastProcessor.onNext(endpoint.decodeMultiItem(m));
                            LOG.debugf("Text message >> Multi: %s", connection);
                        });
                    }

                    ws.closeHandler(new Handler<Void>() {
                        @Override
                        public void handle(Void event) {
                            context.runOnContext(new Handler<Void>() {
                                @Override
                                public void handle(Void event) {
                                    endpoint.onClose(context).onComplete(r -> {
                                        if (r.succeeded()) {
                                            LOG.debugf("@OnClose callback completed: %s", connection);
                                        } else {
                                            LOG.errorf(r.cause(), "Unable to complete @OnClose callback: %s", connection);
                                        }
                                        connectionManager.remove(endpointClass, connection);
                                        sessionContext.terminate();
                                    });
                                }
                            });
                        }
                    });
                });
            }
        };
    }

    private void messageHandlers(WebSocketEndpoint endpoint, ServerWebSocket ws, Context context, Consumer<Buffer> binaryAction,
            Consumer<String> textAction) {
        if (endpoint.consumedMessageType() == MessageType.BINARY) {
            ws.binaryMessageHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer message) {
                    context.runOnContext(new Handler<Void>() {
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
                    context.runOnContext(new Handler<Void>() {
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
            Codecs codecs, WebSocketRuntimeConfig config) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = WebSocketServerRecorder.class.getClassLoader();
            }
            @SuppressWarnings("unchecked")
            Class<? extends WebSocketEndpoint> endpointClazz = (Class<? extends WebSocketEndpoint>) cl
                    .loadClass(endpointClassName);
            WebSocketEndpoint endpoint = (WebSocketEndpoint) endpointClazz
                    .getDeclaredConstructor(Context.class, WebSocketServerConnection.class, Codecs.class,
                            WebSocketRuntimeConfig.class)
                    .newInstance(context, connection, codecs, config);
            return endpoint;
        } catch (Exception e) {
            throw new WebSocketServerException("Unable to create endpoint instance: " + endpointClassName, e);
        }
    }

    private WebSocketSessionContext sessionContext(ArcContainer container) {
        for (InjectableContext injectableContext : container.getContexts(SessionScoped.class)) {
            if (WebSocketSessionContext.class.equals(injectableContext.getClass())) {
                return (WebSocketSessionContext) injectableContext;
            }
        }
        throw new WebSocketServerException("CDI session context not registered");
    }

}
