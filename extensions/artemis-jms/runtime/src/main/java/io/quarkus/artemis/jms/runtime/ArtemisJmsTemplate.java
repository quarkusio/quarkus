package io.quarkus.artemis.jms.runtime;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.artemis.core.runtime.ArtemisRuntimeConfig;
import io.quarkus.runtime.annotations.Template;

@Template
public class ArtemisJmsTemplate {

    public void setConfig(ArtemisRuntimeConfig config, BeanContainer container) {
        container.instance(ArtemisJmsProducer.class).setConfig(config);
    }
}
