package io.quarkus.artemis.core.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ArtemisCoreRecorder {

    public void setConfig(ArtemisRuntimeConfig config, BeanContainer container) {
        container.instance(ArtemisCoreProducer.class).setConfig(config);
    }
}
