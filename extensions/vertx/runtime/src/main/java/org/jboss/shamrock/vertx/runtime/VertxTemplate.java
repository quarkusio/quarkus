package org.jboss.shamrock.vertx.runtime;

import org.jboss.shamrock.arc.runtime.BeanContainerListener;
import org.jboss.shamrock.runtime.Template;

@Template
public class VertxTemplate {

    public BeanContainerListener configureVertx(VertxConfiguration config) {
        return container -> {
            VertxProducer instance = container.instance(VertxProducer.class);
            //require a null check, if nothing is using it then this can be auto removed by ArC
            if(instance != null) {
                instance.configure(config);
            }
        };
    }
}
