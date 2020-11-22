package io.quarkus.unleash.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class UnleashRecorder {

    public void initializeProducers(BeanContainer container, UnleashRuntimeTimeConfig config) {
        UnleashProducer producer = container.instance(UnleashProducer.class);
        producer.initialize(config);
    }

}
