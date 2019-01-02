package org.jboss.shamrock.reactivemessaging.runtime;

import java.util.Map;
import java.util.Map.Entry;

import org.jboss.protean.arc.Arc;
import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.runtime.annotations.Template;

import io.smallrye.reactive.messaging.extension.MediatorManager;

/**
 *
 * @author Martin Kouba
 */
@Template
public class ReactiveMessagingTemplate {

    public void registerMediators(Map<String, String> beanClassToBeanId, BeanContainer container) {
        // Extract the configuration and register mediators
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
