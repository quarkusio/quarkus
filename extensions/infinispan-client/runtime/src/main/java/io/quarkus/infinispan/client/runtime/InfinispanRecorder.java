package io.quarkus.infinispan.client.runtime;

import java.util.Properties;

import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.annotations.RelaxedValidation;

@Recorder
public class InfinispanRecorder {

    public BeanContainerListener configureInfinispan(@RelaxedValidation Properties properties) {
        return container -> {
            InfinispanClientProducer instance = container.instance(InfinispanClientProducer.class);
            instance.configure(properties);
        };
    }
}
