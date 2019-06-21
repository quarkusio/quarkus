package io.quarkus.artemis.core.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;

@Template
public class ArtemisCoreTemplate {

    public void setConfig(ArtemisRuntimeConfig config, BeanContainer container) {
        container.instance(ArtemisCoreProducer.class).setConfig(config);
    }
}
