package io.quarkus.vertx.runtime;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.eventbus.MessageConsumer;

@Recorder
public class VertxRecorder {

    private static final Logger LOGGER = Logger.getLogger(VertxRecorder.class.getName());

    static volatile Vertx vertx;
    static volatile List<MessageConsumer<?>> messageConsumers;

    public void configureVertx(RuntimeValue<Vertx> vertx, Map<String, ConsumeEvent> messageConsumerConfigurations,
            LaunchMode launchMode, ShutdownContext shutdown, Map<Class<?>, Class<?>> codecByClass) {
        this.vertx = vertx.getValue();
        this.messageConsumers = new ArrayList<>();

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

    public static Vertx getVertx() {
        return vertx;
    }

    void destroy() {
        messageConsumers = null;
    }

    void registerMessageConsumers(Map<String, ConsumeEvent> messageConsumerConfigurations) {
        if (!messageConsumerConfigurations.isEmpty()) {
            EventBus eventBus = vertx.eventBus();
            CountDownLatch latch = new CountDownLatch(messageConsumerConfigurations.size());
            for (Entry<String, ConsumeEvent> entry : messageConsumerConfigurations.entrySet()) {
                EventConsumerInvoker invoker = createInvoker(entry.getKey());
                String address = entry.getValue().value();
                MessageConsumer<Object> consumer;
                if (entry.getValue().local()) {
                    consumer = eventBus.localConsumer(address);
                } else {
                    consumer = eventBus.consumer(address);
                }
                consumer.handler(m -> {
                    try {
                        invoker.invoke(m);
                    } catch (Throwable e) {
                        m.fail(ConsumeEvent.FAILURE_CODE, e.getMessage());
                    }
                });
                consumer.completionHandler(ar -> {
                    if (ar.succeeded()) {
                        latch.countDown();
                    }
                });
                messageConsumers.add(consumer);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Unable to register all message consumer methods", e);
            }
        }
    }

    void unregisterMessageConsumers() {
        CountDownLatch latch = new CountDownLatch(messageConsumers.size());
        for (MessageConsumer<?> messageConsumer : messageConsumers) {
            messageConsumer.unregister(ar -> {
                if (ar.succeeded()) {
                    latch.countDown();
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

    private void registerCodecs(Map<Class<?>, Class<?>> codecByClass) {
        for (Map.Entry<Class<?>, Class<?>> codecEntry : codecByClass.entrySet()) {
            registerCodec(codecEntry.getKey(), codecEntry.getValue());
        }
    }

    private void registerCodec(Class<?> typeToAdd, Class<?> messageCodecClass) {
        try {
            if (MessageCodec.class.isAssignableFrom(messageCodecClass)) {
                MessageCodec messageCodec = (MessageCodec) messageCodecClass.newInstance();
                registerCodec(typeToAdd, messageCodec);
            } else {
                LOGGER.error(String.format("The codec %s does not inherit from MessageCodec ", messageCodecClass.toString()));
            }
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Cannot instantiate the MessageCodec " + messageCodecClass.toString(), e);
        }
    }

    private void registerCodec(Class<?> typeToAdd, MessageCodec codec) {
        EventBus eventBus = vertx.eventBus();
        eventBus.registerDefaultCodec(typeToAdd, codec);
    }
}
