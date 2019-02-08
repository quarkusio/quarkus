package org.jboss.shamrock.vertx.runtime;

import java.util.List;
import java.util.Map;

import org.jboss.shamrock.arc.runtime.BeanContainer;
import org.jboss.shamrock.runtime.annotations.Template;

@Template
public class VertxTemplate {

    public void configureVertx(BeanContainer container, List<Map<String, String>> messageConsumerConfigurations) {
        VertxProducer instance = container.instance(VertxProducer.class);
        instance.registerMessageConsumers(messageConsumerConfigurations);
    }
}
