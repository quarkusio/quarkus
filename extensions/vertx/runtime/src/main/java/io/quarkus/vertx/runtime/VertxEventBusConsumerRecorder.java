package io.quarkus.vertx.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setCurrentContextSafe;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.vertx.ConsumeEvent;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
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

    public void configureVertx(Supplier<Vertx> vertx, Map<String, ConsumeEvent> messageConsumerConfigurations,
            LaunchMode launchMode, ShutdownContext shutdown, Map<Class<?>, Class<?>> codecByClass) {
        VertxEventBusConsumerRecorder.vertx = vertx.get();
        VertxEventBusConsumerRecorder.messageConsumers = new CopyOnWriteArrayList<>();

        registerMessageConsumers(messageConsumerConfigurations);
        registerCodecs(codecByClass);

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

    void registerMessageConsumers(Map<String, ConsumeEvent> messageConsumerConfigurations) {
        if (!messageConsumerConfigurations.isEmpty()) {
            EventBus eventBus = vertx.eventBus();
            VertxInternal vi = (VertxInternal) VertxEventBusConsumerRecorder.vertx;
            CountDownLatch latch = new CountDownLatch(messageConsumerConfigurations.size());
            final List<Throwable> registrationFailures = new ArrayList<>();
            for (Entry<String, ConsumeEvent> entry : messageConsumerConfigurations.entrySet()) {
                EventConsumerInvoker invoker = createInvoker(entry.getKey());
                String address = entry.getValue().value();
                // Create a context attached to each consumer
                // If we don't all consumers will use the same event loop and so published messages (dispatched to all
                // consumers) delivery is serialized.
                ContextInternal context = vi.createEventLoopContext();
                context.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void x) {
                        MessageConsumer<Object> consumer;
                        if (entry.getValue().local()) {
                            consumer = eventBus.localConsumer(address);
                        } else {
                            consumer = eventBus.consumer(address);
                        }

                        consumer.handler(new Handler<Message<Object>>() {
                            @Override
                            public void handle(Message<Object> m) {
                                if (invoker.isBlocking()) {
                                    // We need to create a duplicated context from the "context"
                                    Context dup = VertxContext.getOrCreateDuplicatedContext(context);
                                    setContextSafe(dup, true);

                                    if (invoker.isRunningOnVirtualThread()) {
                                        // Switch to a Vert.x context to capture it and use it during the invocation.
                                        dup.runOnContext(new Handler<Void>() {
                                            @Override
                                            public void handle(Void event) {
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
                                            }
                                        });
                                    } else {
                                        dup.executeBlocking(new Callable<Void>() {
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
                                        }, invoker.isOrdered());
                                    }
                                } else {
                                    // Will run on the context used for the consumer registration.
                                    // It's a duplicated context, but we need to mark it as safe.
                                    // The safety comes from the fact that it's instantiated by Vert.x for every
                                    // message.
                                    setCurrentContextSafe(true);
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
    private EventConsumerInvoker createInvoker(String invokerClassName) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = VertxProducer.class.getClassLoader();
            }
            Class<? extends EventConsumerInvoker> invokerClazz = (Class<? extends EventConsumerInvoker>) cl
                    .loadClass(invokerClassName);
            return invokerClazz.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException
                | InvocationTargetException e) {
            throw new IllegalStateException("Unable to create invoker: " + invokerClassName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerCodecs(Map<Class<?>, Class<?>> codecByClass) {
        EventBus eventBus = vertx.eventBus();
        boolean isDevMode = ProfileManager.getLaunchMode() == LaunchMode.DEVELOPMENT;
        for (Map.Entry<Class<?>, Class<?>> codecEntry : codecByClass.entrySet()) {
            Class<?> target = codecEntry.getKey();
            Class<?> codec = codecEntry.getValue();
            try {
                if (MessageCodec.class.isAssignableFrom(codec)) {
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
    }

    public RuntimeValue<Vertx> forceStart(Supplier<Vertx> vertx) {
        return new RuntimeValue<>(vertx.get());
    }
}
