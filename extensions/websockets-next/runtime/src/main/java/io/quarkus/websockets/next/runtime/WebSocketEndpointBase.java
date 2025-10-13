package io.quarkus.websockets.next.runtime;

import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableBean;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.quarkus.websockets.next.InboundProcessingMode;
import io.quarkus.websockets.next.runtime.ConcurrencyLimiter.PromiseComplete;
import io.quarkus.websockets.next.runtime.telemetry.ErrorInterceptor;
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

    // Keep this field public - there's a problem with ConnectionArgumentProvider reading the protected field in the test mode
    public final WebSocketConnectionBase connection;

    protected final Codecs codecs;

    private final ErrorInterceptor errorInterceptor;

    private final ConcurrencyLimiter limiter;

    private final ArcContainer container;

    private final ContextSupport contextSupport;

    private final SecuritySupport securitySupport;

    private final InjectableBean<?> bean;

    private final Object beanInstance;

    public WebSocketEndpointBase(WebSocketConnectionBase connection, Codecs codecs, ContextSupport contextSupport,
            SecuritySupport securitySupport, ErrorInterceptor errorInterceptor) {
        this.connection = connection;
        this.codecs = codecs;
        this.limiter = inboundProcessingMode() == InboundProcessingMode.SERIAL ? new ConcurrencyLimiter(connection) : null;
        this.container = Arc.container();
        this.contextSupport = contextSupport;
        this.securitySupport = securitySupport;
        this.errorInterceptor = errorInterceptor;
        InjectableBean<?> bean = container.bean(beanIdentifier());
        if (bean.getScope().equals(ApplicationScoped.class)
                || bean.getScope().equals(Singleton.class)) {
            // For certain scopes, we can optimize and obtain the contextual reference immediately
            this.bean = null;
            this.beanInstance = container.instance(bean).get();
        } else {
            this.bean = bean;
            this.beanInstance = null;
        }
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
    public Future<Void> onPingMessage(Buffer message) {
        return execute(message, onPingMessageExecutionModel(), this::doOnPingMessage, false);
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
                    doExecute(context, message, executionModel, action, terminateSession, complete::complete,
                            complete::failure);
                }
            });
        } else {
            // No need to limit the concurrency
            doExecute(context, message, executionModel, action, terminateSession, promise::complete, promise::fail);
        }
        return promise.future();
    }

    private <M> void doExecute(Context context, M message, ExecutionModel executionModel,
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
        Promise<Void> promise = Promise.promise();
        // Always execute error handler on a new duplicated context
        ContextSupport.createNewDuplicatedContext(Vertx.currentContext(), connection).runOnContext(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                Handler<Void> contextSupportEnd = new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        contextSupport.end(false);
                    }
                };

                if (executionModel == ExecutionModel.VIRTUAL_THREAD) {
                    VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                        @Override
                        public void run() {
                            Context context = Vertx.currentContext();
                            contextSupport.start();
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
                            contextSupport.start();
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
                            contextSupport.start();
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
            }
        });
        return Uni.createFrom().completionStage(() -> promise.future().toCompletionStage());
    }

    public Object beanInstance() {
        return beanInstance != null ? beanInstance : container.instance(bean).get();
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

    protected Uni<Void> doOnPingMessage(Buffer message) {
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
        // This method is overridden if there is at least one error handler defined
        interceptError(t);
        return Uni.createFrom().failure(t);
    }

    // method is used in generated subclasses, if you change a name, change bytecode generation as well
    public void interceptError(Throwable t) {
        if (errorInterceptor != null) {
            errorInterceptor.intercept(t);
        }
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

    public Uni<Void> multiText(Multi<Object> multi, Function<? super Object, Uni<?>> action) {
        multi
                // Encode and send message
                .onItem().call(action)
                .onFailure().recoverWithMulti(t -> {
                    return doOnError(t).toMulti();
                })
                .subscribe().with(
                        m -> LOG.debugf("Multi >> text message: %s", connection),
                        t -> LOG.errorf(t, "Unable to send text message from Multi: %s ", connection));
        return Uni.createFrom().voidItem();
    }

    public Uni<Void> sendBinary(Buffer message, boolean broadcast) {
        return broadcast ? connection.broadcast().sendBinary(message) : connection.sendBinary(message);
    }

    public Uni<Void> multiBinary(Multi<Object> multi, Function<? super Object, Uni<?>> action) {
        multi
                // Encode and send message
                .onItem().call(action)
                .onFailure().recoverWithMulti(t -> doOnError(t).toMulti())
                .subscribe().with(
                        m -> LOG.debugf("Multi >> binary message: %s", connection),
                        t -> LOG.errorf(t, "Unable to send binary message from Multi: %s ", connection));
        return Uni.createFrom().voidItem();
    }

}
