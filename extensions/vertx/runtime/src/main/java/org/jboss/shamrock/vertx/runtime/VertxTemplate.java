package org.jboss.shamrock.vertx.runtime;

import java.util.Map;

import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.runtime.annotations.Template;

@Template
public class VertxTemplate {

    public void configureVertx(BeanContainer container, VertxConfiguration config, Map<String, ConsumeEvent> messageConsumerConfigurations) {
        VertxProducer instance = container.instance(VertxProducer.class);
        instance.configure(config);
        instance.registerMessageConsumers(messageConsumerConfigurations);
    }
}
