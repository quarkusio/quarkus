package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.reactive.messaging.extension.MediatorManager;

/**
 *
 * @author Martin Kouba
 */
@Recorder
public class SmallRyeReactiveMessagingRecorder {

    public void registerMediators(Map<String, String> beanClassToBeanId, BeanContainer container, List<String> emitters) {
        // Extract the configuration and register mediators
        MediatorManager mediatorManager = container.instance(MediatorManager.class);
        mediatorManager.initializeEmitters(emitters);
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
