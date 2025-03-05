package io.quarkus.vertx.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setCurrentContextSafe;
import static io.smallrye.common.expression.Expression.Flag.LENIENT_SYNTAX;
import static io.smallrye.common.expression.Expression.Flag.NO_TRIM;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.vertx.LocalEventBusCodec;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.expression.Expression;
import io.smallrye.common.expression.ResolveContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.impl.ContextInternal;
import io.vertx.core.impl.VertxInternal;

@Recorder
public class VertxEventBusConsumerRecorder {

    private static final Logger LOGGER = Logger.getLogger(VertxEventBusConsumerRecorder.class.getName());

    static volatile Vertx vertx;
    static volatile List<MessageConsumer<?>> messageConsumers;

    public void configureVertx(Supplier<Vertx> vertx,
            List<EventConsumerInfo> messageConsumerConfigurations,
            LaunchMode launchMode, ShutdownContext shutdown, Map<Class<?>, Class<?>> codecByClass,
            List<Class<?>> selectorTypes) {
        VertxEventBusConsumerRecorder.vertx = vertx.get();
        VertxEventBusConsumerRecorder.messageConsumers = new CopyOnWriteArrayList<>();

        registerMessageConsumers(messageConsumerConfigurations);
        registerCodecs(codecByClass, selectorTypes);

        if (launchMode == LaunchMode.DEVELOPMENT) {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    unregisterMessageConsumers();
                }
            });
        } else {
            shutdown.addShutdownTask(new Runnable() {
                @Override
                public void run() {
                    destroy();
                }
            });
        }
    }

    public RuntimeValue<CurrentContextFactory> currentContextFactory() {
        return new RuntimeValue<>(new VertxCurrentContextFactory());
    }

    public static Vertx getVertx() {
        return vertx;
    }

    void destroy() {
        messageConsumers = null;
        vertx = null;
    }

    void registerMessageConsumers(List<EventConsumerInfo> messageConsumerConfigurations) {
        if (!messageConsumerConfigurations.isEmpty()) {
            EventBus eventBus = vertx.eventBus();
            VertxInternal vi = (VertxInternal) VertxEventBusConsumerRecorder.vertx;
            CountDownLatch latch = new CountDownLatch(messageConsumerConfigurations.size());
            final List<Throwable> registrationFailures = new ArrayList<>();
            for (EventConsumerInfo info : messageConsumerConfigurations) {
                EventConsumerInvoker invoker = new EventConsumerInvoker(info.invoker.getValue(), info.splitHeadersBodyParams);
                String address = lookUpPropertyValue(info.annotation.value());
                boolean local = info.annotation.local();
                boolean blocking = info.annotation.blocking() || info.blockingAnnotation || info.runOnVirtualThreadAnnotation;
                boolean runOnVirtualThread = info.runOnVirtualThreadAnnotation;
                boolean ordered = info.annotation.ordered();
                // Create a context attached to each consumer
                // If we don't all consumers will use the same event loop and so published messages (dispatched to all
                // consumers) delivery is serialized.
                ContextInternal context = vi.createEventLoopContext();
                context.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void x) {
                        MessageConsumer<Object> consumer;
                        if (local) {
                            consumer = eventBus.localConsumer(address);
                        } else {
                            consumer = eventBus.consumer(address);
                        }

                        consumer.handler(new Handler<Message<Object>>() {
                            @Override
                            public void handle(Message<Object> m) {
                                // Will run on the context used for the consumer registration.
                                // It's a duplicated context, but we need to mark it as safe.
                                // The safety comes from the fact that it's instantiated by Vert.x for every
                                // message.
                                setCurrentContextSafe(true);
                                if (blocking) {
                                    if (runOnVirtualThread) {
                                        VirtualThreadsRecorder.getCurrent().execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    invoker.invoke(m);
                                                } catch (Exception e) {
                                                    if (m.replyAddress() == null) {
                                                        // No reply handler
                                                        throw wrapIfNecessary(e);
                                                    } else {
                                                        m.fail(ConsumeEvent.FAILURE_CODE, e.toString());
                                                    }
                                                }
                                            }
                                        });
                                    } else {
                                        Future<Void> future = Vertx.currentContext().executeBlocking(new Callable<Void>() {
                                            @Override
                                            public Void call() {
                                                try {
                                                    invoker.invoke(m);
                                                } catch (Exception e) {
                                                    if (m.replyAddress() == null) {
                                                        // No reply handler
                                                        throw wrapIfNecessary(e);
                                                    } else {
                                                        m.fail(ConsumeEvent.FAILURE_CODE, e.toString());
                                                    }
                                                }
                                                return null;
                                            }
                                        }, ordered);
                                        future.onFailure(context::reportException);
                                    }
                                } else {
                                    try {
                                        invoker.invoke(m);
                                    } catch (Exception e) {
                                        if (m.replyAddress() == null) {
                                            // No reply handler
                                            throw wrapIfNecessary(e);
                                        } else {
                                            m.fail(ConsumeEvent.FAILURE_CODE, e.toString());
                                        }
                                    }
                                }
                            }
                        });

                        consumer.completionHandler(new Handler<AsyncResult<Void>>() {
                            @Override
                            public void handle(AsyncResult<Void> ar) {
                                latch.countDown();
                                if (ar.failed()) {
                                    registrationFailures.add(ar.cause());
                                }
                            }
                        });
                        messageConsumers.add(consumer);
                    }
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to register all message consumer methods", e);
            }
            if (!registrationFailures.isEmpty()) {
                // just log/raise the first failure
                throw new RuntimeException("Registration of one or more message consumers failed", registrationFailures.get(0));
            }
        }
    }

    static RuntimeException wrapIfNecessary(Throwable e) {
        if (e instanceof Error) {
            throw (Error) e;
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else {
            return new RuntimeException(e);
        }
    }

    void unregisterMessageConsumers() {
        CountDownLatch latch = new CountDownLatch(messageConsumers.size());
        for (MessageConsumer<?> messageConsumer : messageConsumers) {
            messageConsumer.unregister(ar -> {
                latch.countDown();
                if (ar.failed()) {
                    LOGGER.warn("Message consumer unregistration failed", ar.cause());
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to unregister all message consumer methods", e);
        }
        messageConsumers.clear();
    }

    @SuppressWarnings("unchecked")
    private void registerCodecs(Map<Class<?>, Class<?>> codecByClass, List<Class<?>> selectorTypes) {
        EventBus eventBus = vertx.eventBus();
        boolean isDevMode = LaunchMode.current() == LaunchMode.DEVELOPMENT;
        for (Map.Entry<Class<?>, Class<?>> codecEntry : codecByClass.entrySet()) {
            Class<?> target = codecEntry.getKey();
            Class<?> codec = codecEntry.getValue();
            try {
                if (MessageCodec.class.isAssignableFrom(codec)) {
                    @SuppressWarnings("rawtypes")
                    MessageCodec messageCodec = (MessageCodec) codec.getDeclaredConstructor().newInstance();
                    if (isDevMode) {
                        // we need to unregister the codecs because in dev mode vert.x is not reloaded
                        // which means that if we don't unregister, we get an exception mentioning that the
                        // codec has already been registered
                        eventBus.unregisterDefaultCodec(target);
                    }
                    eventBus.registerDefaultCodec(target, messageCodec);
                } else {
                    LOGGER.error(String.format("The codec %s does not inherit from MessageCodec ", target.toString()));
                }
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                LOGGER.error("Cannot instantiate the MessageCodec " + target.toString(), e);
            }
        }

        String localCodecName = "quarkus_default_local_codec";
        if (isDevMode) {
            eventBus.unregisterCodec(localCodecName);
        }
        eventBus.registerCodec(new LocalEventBusCodec<>(localCodecName));
        eventBus.codecSelector(new Function<Object, String>() {
            @Override
            public String apply(Object messageBody) {
                for (Class<?> selectorType : selectorTypes) {
                    if (selectorType.isAssignableFrom(messageBody.getClass())) {
                        return localCodecName;
                    }
                }
                return null;
            }
        });
    }

    public RuntimeValue<Vertx> forceStart(Supplier<Vertx> vertx) {
        return new RuntimeValue<>(vertx.get());
    }

    /**
     * Looks up the property value by checking whether the value is a configuration key and resolves it if so.
     *
     * @param propertyValue property value to look up.
     * @return the resolved property value.
     */
    private static String lookUpPropertyValue(String propertyValue) {
        String value = propertyValue.stripLeading();
        if (!value.isEmpty() && isConfigExpression(value)) {
            value = resolvePropertyExpression(value);
        }
        return value;
    }

    /**
     * Adapted from {@link io.smallrye.config.ExpressionConfigSourceInterceptor}
     */
    private static String resolvePropertyExpression(String expr) {
        final Config config = ConfigProvider.getConfig();
        final Expression expression = Expression.compile(expr, LENIENT_SYNTAX, NO_TRIM);
        final String expanded = expression.evaluate(new BiConsumer<ResolveContext<RuntimeException>, StringBuilder>() {
            @Override
            public void accept(ResolveContext<RuntimeException> resolveContext, StringBuilder stringBuilder) {
                final Optional<String> resolve = config.getOptionalValue(resolveContext.getKey(), String.class);
                if (resolve.isPresent()) {
                    stringBuilder.append(resolve.get());
                } else if (resolveContext.hasDefault()) {
                    resolveContext.expandDefault();
                } else {
                    throw new NoSuchElementException(String.format("Could not expand value %s in property %s",
                            resolveContext.getKey(), expr));
                }
            }
        });
        return expanded;
    }

    private static boolean isConfigExpression(String val) {
        if (val == null) {
            return false;
        }
        int exprStart = val.indexOf("${");
        int exprEnd = -1;
        if (exprStart >= 0) {
            exprEnd = val.indexOf('}', exprStart + 2);
        }
        return exprEnd > 0;
    }
}
