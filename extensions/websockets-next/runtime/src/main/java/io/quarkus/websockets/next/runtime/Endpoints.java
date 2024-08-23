package io.quarkus.websockets.next.runtime;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.enterprise.context.SessionScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.UnhandledFailureStrategy;
import io.quarkus.websockets.next.WebSocketException;
import io.quarkus.websockets.next.runtime.WebSocketSessionContext.SessionContextState;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketBase;

class Endpoints {

    private static final Logger LOG = Logger.getLogger(Endpoints.class);

    static void initialize(Vertx vertx, ArcContainer container, Codecs codecs, WebSocketConnectionBase connection,
            WebSocketBase ws, String generatedEndpointClass, Optional<Duration> autoPingInterval,
            SecuritySupport securitySupport, UnhandledFailureStrategy unhandledFailureStrategy, TrafficLogger trafficLogger,
            Runnable onClose) {

        Context context = vertx.getOrCreateContext();

        // Initialize and capture the session context state that will be activated
        // during message processing
        WebSocketSessionContext sessionContext = sessionContext(container);
        SessionContextState sessionContextState = sessionContext.initializeContextState();
        ContextSupport contextSupport = new ContextSupport(connection, sessionContextState,
                sessionContext(container),
                container.requestContext());

        // Create an endpoint that delegates callbacks to the endpoint bean
        WebSocketEndpoint endpoint = createEndpoint(generatedEndpointClass, context, connection, codecs, contextSupport,
                securitySupport);

        // A broadcast processor is only needed if Multi is consumed by the callback
        BroadcastProcessor<Object> textBroadcastProcessor = endpoint.consumedTextMultiType() != null
                ? BroadcastProcessor.create()
                : null;
        BroadcastProcessor<Object> binaryBroadcastProcessor = endpoint.consumedBinaryMultiType() != null
                ? BroadcastProcessor.create()
                : null;

        // NOTE: We always invoke callbacks on a new duplicated context
        // and the endpoint is responsible to make the switch if blocking/virtualThread

        Context onOpenContext = ContextSupport.createNewDuplicatedContext(context, connection);
        onOpenContext.runOnContext(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                endpoint.onOpen().onComplete(r -> {
                    if (r.succeeded()) {
                        LOG.debugf("@OnOpen callback completed: %s", connection);
                        // If Multi is consumed we need to invoke the callback eagerly
                        // but after @OnOpen completes
                        if (textBroadcastProcessor != null) {
                            Multi<Object> multi = textBroadcastProcessor.onCancellation().call(connection::close);
                            onOpenContext.runOnContext(new Handler<Void>() {
                                @Override
                                public void handle(Void event) {
                                    endpoint.onTextMessage(multi).onComplete(r -> {
                                        if (r.succeeded()) {
                                            LOG.debugf("@OnTextMessage callback consuming Multi completed: %s",
                                                    connection);
                                        } else {
                                            handleFailure(unhandledFailureStrategy, r.cause(),
                                                    "Unable to complete @OnTextMessage callback consuming Multi",
                                                    connection);
                                        }
                                    });
                                }
                            });
                        }
                        if (binaryBroadcastProcessor != null) {
                            Multi<Object> multi = binaryBroadcastProcessor.onCancellation().call(connection::close);
                            onOpenContext.runOnContext(new Handler<Void>() {
                                @Override
                                public void handle(Void event) {
                                    endpoint.onBinaryMessage(multi).onComplete(r -> {
                                        if (r.succeeded()) {
                                            LOG.debugf("@OnBinaryMessage callback consuming Multi completed: %s",
                                                    connection);
                                        } else {
                                            handleFailure(unhandledFailureStrategy, r.cause(),
                                                    "Unable to complete @OnBinaryMessage callback consuming Multi",
                                                    connection);
                                        }
                                    });
                                }
                            });
                        }
                    } else {
                        handleFailure(unhandledFailureStrategy, r.cause(), "Unable to complete @OnOpen callback", connection);
                    }
                });
            }
        });

        if (textBroadcastProcessor == null) {
            // Multi not consumed - invoke @OnTextMessage callback for each message received
            textMessageHandler(connection, endpoint, ws, onOpenContext, m -> {
                if (trafficLogger != null) {
                    trafficLogger.textMessageReceived(connection, m);
                }
                endpoint.onTextMessage(m).onComplete(r -> {
                    if (r.succeeded()) {
                        LOG.debugf("@OnTextMessage callback consumed text message: %s", connection);
                    } else {
                        handleFailure(unhandledFailureStrategy, r.cause(),
                                "Unable to consume text message in @OnTextMessage callback",
                                connection);
                    }
                });
            }, true);
        } else {
            textMessageHandler(connection, endpoint, ws, onOpenContext, m -> {
                contextSupport.start();
                securitySupport.start();
                try {
                    if (trafficLogger != null) {
                        trafficLogger.textMessageReceived(connection, m);
                    }
                    textBroadcastProcessor.onNext(endpoint.decodeTextMultiItem(m));
                    LOG.debugf("Text message >> Multi: %s", connection);
                } catch (Throwable throwable) {
                    endpoint.doOnError(throwable).subscribe().with(
                            v -> LOG.debugf("Text message >> Multi: %s", connection),
                            t -> handleFailure(unhandledFailureStrategy, t, "Unable to send text message to Multi",
                                    connection));
                } finally {
                    contextSupport.end(false);
                }
            }, false);
        }

        if (binaryBroadcastProcessor == null) {
            // Multi not consumed - invoke @OnBinaryMessage callback for each message received
            binaryMessageHandler(connection, endpoint, ws, onOpenContext, m -> {
                if (trafficLogger != null) {
                    trafficLogger.binaryMessageReceived(connection, m);
                }
                endpoint.onBinaryMessage(m).onComplete(r -> {
                    if (r.succeeded()) {
                        LOG.debugf("@OnBinaryMessage callback consumed binary message: %s", connection);
                    } else {
                        handleFailure(unhandledFailureStrategy, r.cause(),
                                "Unable to consume binary message in @OnBinaryMessage callback",
                                connection);
                    }
                });
            }, true);
        } else {
            binaryMessageHandler(connection, endpoint, ws, onOpenContext, m -> {
                contextSupport.start();
                securitySupport.start();
                try {
                    if (trafficLogger != null) {
                        trafficLogger.binaryMessageReceived(connection, m);
                    }
                    binaryBroadcastProcessor.onNext(endpoint.decodeBinaryMultiItem(m));
                    LOG.debugf("Binary message >> Multi: %s", connection);
                } catch (Throwable throwable) {
                    endpoint.doOnError(throwable).subscribe().with(
                            v -> LOG.debugf("Binary message >> Multi: %s", connection),
                            t -> handleFailure(unhandledFailureStrategy, t, "Unable to send binary message to Multi",
                                    connection));
                } finally {
                    contextSupport.end(false);
                }
            }, false);
        }

        pongMessageHandler(connection, endpoint, ws, onOpenContext, m -> {
            endpoint.onPongMessage(m).onComplete(r -> {
                if (r.succeeded()) {
                    LOG.debugf("@OnPongMessage callback consumed text message: %s", connection);
                } else {
                    handleFailure(unhandledFailureStrategy, r.cause(),
                            "Unable to consume text message in @OnPongMessage callback", connection);
                }
            });
        });

        Long timerId;
        if (autoPingInterval.isPresent()) {
            timerId = vertx.setPeriodic(autoPingInterval.get().toMillis(), new Handler<Long>() {
                @Override
                public void handle(Long timerId) {
                    connection.sendAutoPing();
                }
            });
        } else {
            timerId = null;
        }

        ws.closeHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                if (trafficLogger != null) {
                    trafficLogger.connectionClosed(connection);
                }
                ContextSupport.createNewDuplicatedContext(context, connection).runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        endpoint.onClose().onComplete(r -> {
                            if (r.succeeded()) {
                                LOG.debugf("@OnClose callback completed: %s", connection);
                            } else {
                                handleFailure(unhandledFailureStrategy, r.cause(), "Unable to complete @OnClose callback",
                                        connection);
                            }
                            securitySupport.onClose();
                            onClose.run();
                            if (timerId != null) {
                                vertx.cancelTimer(timerId);
                            }
                        });
                    }
                });
            }
        });

        ws.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable t) {
                ContextSupport.createNewDuplicatedContext(context, connection).runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        endpoint.doOnError(t).subscribe().with(
                                v -> LOG.debugf("Error [%s] processed: %s", t.getClass(), connection),
                                t -> handleFailure(unhandledFailureStrategy, t, "Unhandled error occurred", connection));
                    }
                });
            }
        });
    }

    private static void handleFailure(UnhandledFailureStrategy strategy, Throwable cause, String message,
            WebSocketConnectionBase connection) {
        switch (strategy) {
            case CLOSE -> closeConnection(cause, connection);
            case LOG -> logFailure(cause, message, connection);
            case NOOP -> LOG.tracef("Unhandled failure ignored: %s", connection);
            default -> throw new IllegalArgumentException("Unexpected strategy: " + strategy);
        }
    }

    private static void closeConnection(Throwable cause, WebSocketConnectionBase connection) {
        if (connection.isClosed()) {
            return;
        }
        connection.close(CloseReason.INTERNAL_SERVER_ERROR).subscribe().with(
                v -> LOG.debugf("Connection closed due to unhandled failure %s: %s", cause, connection),
                t -> LOG.errorf("Unable to close connection [%s] due to unhandled failure [%s]: %s", connection.id(), cause,
                        t));
    }

    private static void logFailure(Throwable throwable, String message, WebSocketConnectionBase connection) {
        if (isWebSocketIsClosedFailure(throwable, connection)) {
            LOG.debugf(throwable,
                    message + ": %s",
                    connection);
        } else if (isSecurityFailure(throwable)) {
            // Avoid excessive logging for security failures
            LOG.errorf("Security failure: %s", throwable.toString());
        } else {
            LOG.errorf(throwable,
                    message + ": %s",
                    connection);
        }
    }

    private static boolean isSecurityFailure(Throwable throwable) {
        return throwable instanceof UnauthorizedException
                || throwable instanceof AuthenticationFailedException
                || throwable instanceof ForbiddenException;
    }

    static boolean isWebSocketIsClosedFailure(Throwable throwable, WebSocketConnectionBase connection) {
        if (!connection.isClosed()) {
            return false;
        }
        if (throwable == null) {
            return false;
        }
        String message = throwable.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("WebSocket is closed");
    }

    private static void textMessageHandler(WebSocketConnectionBase connection, WebSocketEndpoint endpoint, WebSocketBase ws,
            Context context, Consumer<String> textAction, boolean newDuplicatedContext) {
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

    private static void binaryMessageHandler(WebSocketConnectionBase connection, WebSocketEndpoint endpoint, WebSocketBase ws,
            Context context, Consumer<Buffer> binaryAction, boolean newDuplicatedContext) {
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
    }

    private static void pongMessageHandler(WebSocketConnectionBase connection, WebSocketEndpoint endpoint, WebSocketBase ws,
            Context context, Consumer<Buffer> pongAction) {
        ws.pongHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer message) {
                Context duplicatedContext = ContextSupport.createNewDuplicatedContext(context, connection);
                duplicatedContext.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        pongAction.accept(message);
                    }
                });
            }
        });
    }

    private static WebSocketEndpoint createEndpoint(String endpointClassName, Context context,
            WebSocketConnectionBase connection, Codecs codecs, ContextSupport contextSupport, SecuritySupport securitySupport) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = WebSocketServerRecorder.class.getClassLoader();
            }
            @SuppressWarnings("unchecked")
            Class<? extends WebSocketEndpoint> endpointClazz = (Class<? extends WebSocketEndpoint>) cl
                    .loadClass(endpointClassName);
            WebSocketEndpoint endpoint = (WebSocketEndpoint) endpointClazz
                    .getDeclaredConstructor(WebSocketConnectionBase.class, Codecs.class, ContextSupport.class,
                            SecuritySupport.class)
                    .newInstance(connection, codecs, contextSupport, securitySupport);
            return endpoint;
        } catch (Exception e) {
            throw new WebSocketException("Unable to create endpoint instance: " + endpointClassName, e);
        }
    }

    private static WebSocketSessionContext sessionContext(ArcContainer container) {
        for (InjectableContext injectableContext : container.getContexts(SessionScoped.class)) {
            if (WebSocketSessionContext.class.equals(injectableContext.getClass())) {
                return (WebSocketSessionContext) injectableContext;
            }
        }
        throw new WebSocketException("CDI session context not registered");
    }
}
