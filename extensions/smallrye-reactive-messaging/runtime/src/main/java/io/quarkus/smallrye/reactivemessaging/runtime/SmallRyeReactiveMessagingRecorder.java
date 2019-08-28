package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.reactive.messaging.extension.MediatorManager;

/**
 * @author Martin Kouba
 */
@Recorder
public class SmallRyeReactiveMessagingRecorder {

    public void configureEmitter(BeanContainer container, String name, String strategy, int bufferSize,
            int defaultBufferSize) {
        MediatorManager mediatorManager = container.instance(MediatorManager.class);
        mediatorManager.initializeEmitter(name, strategy, bufferSize, defaultBufferSize);
    }

    public void registerMediators(Map<String, String> beanClassToBeanId, BeanContainer container) {
        MediatorManager mediatorManager = container.instance(MediatorManager.class);
        for (Entry<String, String> entry : beanClassToBeanId.entrySet()) {
            try {
                Class<?> beanClass = Thread.currentThread()
                        .getContextClassLoader()
                        .loadClass(entry.getKey());
                mediatorManager.analyze(beanClass, Arc.container()
                        .bean(entry.getValue()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }

    }

}
