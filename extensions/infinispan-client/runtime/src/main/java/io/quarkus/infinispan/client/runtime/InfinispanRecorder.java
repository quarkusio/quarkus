package io.quarkus.infinispan.client.runtime;

import java.util.Properties;

import io.quarkus.arc.Arc;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class InfinispanRecorder {

    public BeanContainerListener configureInfinispan(Properties properties) {
        return container -> {
            InfinispanClientProducer instance = container.instance(InfinispanClientProducer.class);
            instance.configure(properties);
        };
    }

    public void configureRuntimeProperties(InfinispanClientRuntimeConfig infinispanClientRuntimeConfig) {
        Arc.container().instance(InfinispanClientProducer.class).get().setRuntimeConfig(infinispanClientRuntimeConfig);
    }
}
