package org.jboss.shamrock.infinispan.client.runtime;

import java.util.Properties;

import org.jboss.shamrock.arc.runtime.BeanContainerListener;
import org.jboss.shamrock.runtime.annotations.Template;

@Template
public class InfinispanTemplate {

    public BeanContainerListener configureInfinispan(Properties properties) {
        return container -> {
            InfinispanClientProducer instance = container.instance(InfinispanClientProducer.class);
            instance.configure(properties);
        };
    }
}
