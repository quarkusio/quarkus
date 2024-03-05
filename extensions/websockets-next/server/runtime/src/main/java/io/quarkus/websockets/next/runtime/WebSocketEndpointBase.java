package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.quarkus.websockets.next.WebSocket.ExecutionMode;
import io.quarkus.websockets.next.WebSocketServerConnection;
import io.quarkus.websockets.next.WebSocketsRuntimeConfig;
import io.quarkus.websockets.next.runtime.ConcurrencyLimiter.PromiseComplete;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

public abstract class WebSocketEndpointBase implements WebSocketEndpoint {

    private static final Logger LOG = Logger.getLogger(WebSocketEndpointBase.class);

    protected final WebSocketServerConnection connection;

    protected final Codecs codecs;

    private final ConcurrencyLimiter limiter;

    @SuppressWarnings("unused")
    private final WebSocketsRuntimeConfig config;

    private final ArcContainer container;

    private final ContextSupport contextSupport;

    public WebSocketEndpointBase(WebSocketServerConnection connection, Codecs codecs,
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
    public Future<Void> onMessage(Object message) {
        return execute(message, onMessageExecutionModel(), this::doOnMessage, false);
    }

    @Override
    public Future<Void> onClose() {
        return execute(null, onCloseExecutionModel(), this::doOnClose, true);
    }

    private Future<Void> execute(Object message, ExecutionModel executionModel,
            Function<Object, Uni<Void>> action, boolean terminateSession) {
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

    private Future<Void> doExecute(Context context, Promise<Void> promise, Object message, ExecutionModel executionModel,
            Function<Object, Uni<Void>> action, boolean terminateSession, Runnable onComplete,
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
        return null;
    }

    // TODO This implementation of timeout does not help a lot
    // Should we emit on the current context?
    // io.smallrye.mutiny.vertx.core.ContextAwareScheduler
    //    private Uni<Void> withTimeout(Uni<Void> action) {
    //        if (config.timeout().isEmpty()) {
    //            return action;
    //        }
    //        return action.ifNoItem().after(config.timeout().get()).fail();
    //    }

    protected Object beanInstance(String identifier) {
        return container.instance(container.bean(identifier)).get();
    }

    protected Uni<Void> doOnOpen(Object message) {
        return Uni.createFrom().voidItem();
    }

    protected Uni<Void> doOnMessage(Object message) {
        return Uni.createFrom().voidItem();
    }

    protected Uni<Void> doOnClose(Object message) {
        return Uni.createFrom().voidItem();
    }

    protected Object decodeText(Type type, String value, Class<?> codecBeanClass) {
        return codecs.textDecode(type, value, codecBeanClass);
    }

    protected String encodeText(Object value, Class<?> codecBeanClass) {
        if (value == null) {
            return null;
        }
        return codecs.textEncode(value, codecBeanClass);
    }

    protected Object decodeBinary(Type type, Buffer value, Class<?> codecBeanClass) {
        return codecs.binaryDecode(type, value, codecBeanClass);
    }

    protected Buffer encodeBinary(Object value, Class<?> codecBeanClass) {
        if (value == null) {
            return null;
        }
        return codecs.binaryEncode(value, codecBeanClass);
    }

    protected Uni<Void> sendText(String message, boolean broadcast) {
        return broadcast ? connection.broadcast().sendText(message) : connection.sendText(message);
    }

    protected Uni<Void> multiText(Multi<Object> multi, boolean broadcast, Function<Object, Uni<Void>> itemFun) {
        multi.onFailure().call(connection::close).subscribe().with(
                m -> {
                    itemFun.apply(m).subscribe().with(v -> LOG.debugf("Multi >> text message: %s", connection),
                            t -> LOG.errorf(t, "Unable to send text message from Multi: %s", connection));
                });
        return Uni.createFrom().voidItem();
    }

    protected Uni<Void> sendBinary(Buffer message, boolean broadcast) {
        return broadcast ? connection.broadcast().sendBinary(message) : connection.sendBinary(message);
    }

    protected Uni<Void> multiBinary(Multi<Object> multi, boolean broadcast, Function<Object, Uni<Void>> itemFun) {
        multi.onFailure().call(connection::close).subscribe().with(
                m -> {
                    itemFun.apply(m).subscribe().with(v -> LOG.debugf("Multi >> binary message: %s", connection),
                            t -> LOG.errorf(t, "Unable to send binary message from Multi: %s", connection));
                });
        return Uni.createFrom().voidItem();
    }
}
