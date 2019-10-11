package io.quarkus.infinispan.embedded.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class InfinispanRecorder {

    public void configureRuntimeProperties(InfinispanEmbeddedRuntimeConfig infinispanEmbeddedRuntimeConfig) {
        InfinispanEmbeddedProducer iep = Arc.container().instance(InfinispanEmbeddedProducer.class).get();
        iep.setRuntimeConfig(infinispanEmbeddedRuntimeConfig);
    }
}