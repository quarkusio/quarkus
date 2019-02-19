package io.quarkus.infinispan.client.runtime;

import java.util.Properties;

import io.quarkus.runtime.annotations.Template;
import io.quarkus.arc.runtime.BeanContainerListener;

@Template
public class InfinispanTemplate {

    public BeanContainerListener configureInfinispan(Properties properties) {
        return container -> {
            InfinispanClientProducer instance = container.instance(InfinispanClientProducer.class);
            instance.configure(properties);
        };
    }
}
