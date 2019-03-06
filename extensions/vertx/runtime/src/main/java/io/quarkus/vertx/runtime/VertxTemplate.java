package io.quarkus.vertx.runtime;

import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;
import io.quarkus.vertx.ConsumeEvent;

@Template
public class VertxTemplate {

    public void configureVertx(BeanContainer container, VertxConfiguration config,
            Map<String, ConsumeEvent> messageConsumerConfigurations) {
        VertxProducer instance = container.instance(VertxProducer.class);
        instance.configure(config);
        instance.registerMessageConsumers(messageConsumerConfigurations);
    }
}
