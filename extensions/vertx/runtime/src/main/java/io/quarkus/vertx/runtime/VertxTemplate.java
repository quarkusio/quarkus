package io.quarkus.vertx.runtime;

import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;

@Template
public class VertxTemplate {

    public void configureVertx(BeanContainer container, VertxConfiguration config, List<Map<String, String>> messageConsumerConfigurations) {
        VertxProducer instance = container.instance(VertxProducer.class);
        instance.configure(config);
        instance.registerMessageConsumers(messageConsumerConfigurations);
    }
}
