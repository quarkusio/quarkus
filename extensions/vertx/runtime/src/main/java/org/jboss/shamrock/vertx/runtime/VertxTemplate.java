package org.jboss.shamrock.vertx.runtime;

import org.jboss.shamrock.arc.runtime.BeanContainerListener;
import org.jboss.shamrock.runtime.Template;

@Template
public class VertxTemplate {

    public BeanContainerListener configureVertx(VertxConfiguration config) {
        return container -> {
            VertxProducer instance = container.instance(VertxProducer.class);
            instance.configure(config);
        };
    }
}
