package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.quarkus.websockets.next.WebSocket.ExecutionMode;
import io.quarkus.websockets.next.WebSocketConnection;
import io.quarkus.websockets.next.WebSocketsRuntimeConfig;
import io.quarkus.websockets.next.runtime.ConcurrencyLimiter.PromiseComplete;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.UniHelper;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public abstract class WebSocketEndpointBase implements WebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(WebSocketEndpointBase.class);

    // Keep this field public - there's a problem with ConnectionArgumentProvider reading the protected field in the test mode
    public final WebSocketConnection connection;

    protected final Codecs codecs;

    private final ConcurrencyLimiter limiter;

    @SuppressWarnings("unused")
    private final WebSocketsRuntimeConfig config;

    private final ArcContainer container;

    private final ContextSupport contextSupport;

    public WebSocketEndpointBase(WebSocketConnection connection, Codecs codecs,
            WebSocketsRuntimeConfig config, ContextSupport contextSupport) {
        this.connection = connection;
        this.codecs = codecs;
        this.limiter = executionMode() == ExecutionMode.SERIAL ? new ConcurrencyLimiter(connection) : null;
        this.config = config;
        this.container = Arc.container();
        this.contextSupport = contextSupport;
    }

    @Override
    public Future<Void> onOpen() {
        return execute(null, onOpenExecutionModel(), this::doOnOpen, false);
    }

    @Override
    public Future<Void> onTextMessage(Object message) {
        return execute(message, onTextMessageExecutionModel(), this::doOnTextMessage, false);
    }

    @Override
    public Future<Void> onBinaryMessage(Object message) {
        return execute(message, onBinaryMessageExecutionModel(), this::doOnBinaryMessage, false);
    }

    @Override
    public Future<Void> onPongMessage(Buffer message) {
        return execute(message, onPongMessageExecutionModel(), this::doOnPongMessage, false);
    }

    @Override
    public Future<Void> onClose() {
        return execute(null, onCloseExecutionModel(), this::doOnClose, true);
    }

    private <M> Future<Void> execute(M message, ExecutionModel executionModel,
            Function<M, Uni<Void>> action, boolean terminateSession) {
        if (executionModel == ExecutionModel.NONE) {
            if (terminateSession) {
                // Just start and terminate the session context
                contextSupport.startSession();
                contextSupport.endSession();
            }
            return Future.succeededFuture();
        }
        Promise<Void> promise = Promise.promise();
        Context context = Vertx.currentContext();
        if (limiter != null) {
            PromiseComplete complete = limiter.newComplete(promise);
            limiter.run(context, new Runnable() {
                @Override
                public void run() {
                    doExecute(context, promise, message, executionModel, action, terminateSession, complete::complete,
                            complete::failure);
                }
            });
        } else {
            // No need to limit the concurrency
            doExecute(context, promise, message, executionModel, action, terminateSession, promise::complete, promise::fail);
        }
        return promise.future();
    }

    private <M> void doExecute(Context context, Promise<Void> promise, M message, ExecutionModel executionModel,
            Function<M, Uni<Void>> action, boolean terminateSession, Runnable onComplete,
            Consumer<? super Throwable> onFailure) {
        Handler<Void> contextSupportEnd = executionModel.isBlocking() ? new Handler<Void>() {

            @Override
            public void handle(Void event) {
                contextSupport.end(terminateSession);
            }
        } : null;

        if (executionModel == ExecutionModel.VIRTUAL_THREAD) {
            VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                @Override
                public void run() {
                    Context context = Vertx.currentContext();
                    contextSupport.start();
                    action.apply(message).subscribe().with(
                            v -> {
                                context.runOnContext(contextSupportEnd);
                                onComplete.run();
                            },
                            t -> {
                                context.runOnContext(contextSupportEnd);
                                onFailure.accept(t);
                            });
                }
            });
        } else if (executionModel == ExecutionModel.WORKER_THREAD) {
            context.executeBlocking(new Callable<Void>() {
                @Override
                public Void call() {
                    Context context = Vertx.currentContext();
                    contextSupport.start();
                    action.apply(message).subscribe().with(
                            v -> {
                                context.runOnContext(contextSupportEnd);
                                onComplete.run();
                            },
                            t -> {
                                context.runOnContext(contextSupportEnd);
                                onFailure.accept(t);
                            });
                    return null;
                }
            }, false);
        } else {
            // Event loop
            contextSupport.start();
            action.apply(message).subscribe().with(
                    v -> {
                        contextSupport.end(terminateSession);
                        onComplete.run();
                    },
                    t -> {
                        contextSupport.end(terminateSession);
                        onFailure.accept(t);
                    });
        }
    }

    public Uni<Void> doErrorExecute(Throwable throwable, ExecutionModel executionModel,
            Function<Throwable, Uni<Void>> action) {
        // We need to capture the current request context state so that it can be activated
        // when the error callback is executed
        ContextState requestContextState = contextSupport.currentRequestContextState();
        Handler<Void> contextSupportEnd = new Handler<Void>() {

            @Override
            public void handle(Void event) {
                contextSupport.end(false, false);
            }
        };
        contextSupportEnd.handle(null);

        Promise<Void> promise = Promise.promise();
        if (executionModel == ExecutionModel.VIRTUAL_THREAD) {
            VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                @Override
                public void run() {
                    Context context = Vertx.currentContext();
                    contextSupport.start(requestContextState);
                    action.apply(throwable).subscribe().with(
                            v -> {
                                context.runOnContext(contextSupportEnd);
                                promise.complete();
                            },
                            t -> {
                                context.runOnContext(contextSupportEnd);
                                promise.fail(t);
                            });
                }
            });
        } else if (executionModel == ExecutionModel.WORKER_THREAD) {
            Vertx.currentContext().executeBlocking(new Callable<Void>() {
                @Override
                public Void call() {
                    Context context = Vertx.currentContext();
                    contextSupport.start(requestContextState);
                    action.apply(throwable).subscribe().with(
                            v -> {
                                context.runOnContext(contextSupportEnd);
                                promise.complete();
                            },
                            t -> {
                                context.runOnContext(contextSupportEnd);
                                promise.fail(t);
                            });
                    return null;
                }
            }, false);
        } else {
            Vertx.currentContext().runOnContext(new Handler<Void>() {
                @Override
                public void handle(Void event) {
                    Context context = Vertx.currentContext();
                    contextSupport.start(requestContextState);
                    action.apply(throwable).subscribe().with(
                            v -> {
                                context.runOnContext(contextSupportEnd);
                                promise.complete();
                            },
                            t -> {
                                context.runOnContext(contextSupportEnd);
                                promise.fail(t);
                            });
                }
            });
        }
        return UniHelper.toUni(promise.future());
    }

    public Object beanInstance(String identifier) {
        return container.instance(container.bean(identifier)).get();
    }

    protected Uni<Void> doOnOpen(Object message) {
        return Uni.createFrom().voidItem();
    }

    protected Uni<Void> doOnTextMessage(Object message) {
        return Uni.createFrom().voidItem();
    }

    protected Uni<Void> doOnBinaryMessage(Object message) {
        return Uni.createFrom().voidItem();
    }

    protected Uni<Void> doOnPongMessage(Buffer message) {
        return Uni.createFrom().voidItem();
    }

    protected Uni<Void> doOnClose(Object message) {
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> doOnError(Throwable t) {
        // This method is overriden if there is at least one error handler defined
        return Uni.createFrom().failure(t);
    }

    public Object decodeText(Type type, String value, Class<?> codecBeanClass) {
        return codecs.textDecode(type, value, codecBeanClass);
    }

    public String encodeText(Object value, Class<?> codecBeanClass) {
        if (value == null) {
            return null;
        }
        return codecs.textEncode(value, codecBeanClass);
    }

    public Object decodeBinary(Type type, Buffer value, Class<?> codecBeanClass) {
        return codecs.binaryDecode(type, value, codecBeanClass);
    }

    public Buffer encodeBinary(Object value, Class<?> codecBeanClass) {
        if (value == null) {
            return null;
        }
        return codecs.binaryEncode(value, codecBeanClass);
    }

    public Uni<Void> sendText(String message, boolean broadcast) {
        return broadcast ? connection.broadcast().sendText(message) : connection.sendText(message);
    }

    public Uni<Void> multiText(Multi<Object> multi, Function<Object, Uni<Void>> action) {
        multi.onFailure().recoverWithMulti(t -> doOnError(t).toMulti())
                .subscribe().with(
                        m -> {
                            // Encode and send message
                            action.apply(m)
                                    .onFailure().recoverWithUni(this::doOnError)
                                    .subscribe()
                                    .with(v -> LOG.debugf("Multi >> text message: %s", connection),
                                            t -> LOG.errorf(t, "Unable to send text message from Multi: %s", connection));
                        },
                        t -> {
                            LOG.errorf(t, "Unable to send text message from Multi: %s ", connection);
                        });
        return Uni.createFrom().voidItem();
    }

    public Uni<Void> sendBinary(Buffer message, boolean broadcast) {
        return broadcast ? connection.broadcast().sendBinary(message) : connection.sendBinary(message);
    }

    public Uni<Void> multiBinary(Multi<Object> multi, Function<Object, Uni<Void>> action) {
        multi.onFailure().recoverWithMulti(t -> doOnError(t).toMulti())
                .subscribe().with(
                        m -> {
                            // Encode and send message
                            action.apply(m)
                                    .onFailure().recoverWithUni(this::doOnError)
                                    .subscribe()
                                    .with(v -> LOG.debugf("Multi >> binary message: %s", connection),
                                            t -> LOG.errorf(t, "Unable to send binary message from Multi: %s", connection));
                        },
                        t -> {
                            LOG.errorf(t, "Unable to send text message from Multi: %s ", connection);
                        });
        return Uni.createFrom().voidItem();
    }
}
