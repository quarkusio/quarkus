package org.infinispan.protean.runtime;

import java.util.Properties;

import org.jboss.shamrock.runtime.annotations.Template;
import org.jboss.shamrock.arc.runtime.BeanContainerListener;

@Template
public class InfinispanTemplate {

    public BeanContainerListener configureInfinispan(Properties properties) {
        return container -> {
            InfinispanClientProducer instance = container.instance(InfinispanClientProducer.class);
            instance.configure(properties);
        };
    }
}
