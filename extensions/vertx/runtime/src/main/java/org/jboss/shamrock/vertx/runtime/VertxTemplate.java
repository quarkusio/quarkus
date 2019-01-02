package org.jboss.shamrock.vertx.runtime;

import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.cdi.BeanContainerListener;

@Template
public class VertxTemplate {

    public BeanContainerListener configureVertx(VertxConfiguration config) {
        return container -> {
            VertxProducer instance = container.instance(VertxProducer.class);
            instance.configure(config);
        };
    }
}
