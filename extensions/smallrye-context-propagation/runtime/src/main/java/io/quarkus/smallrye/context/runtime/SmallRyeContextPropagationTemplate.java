package io.quarkus.smallrye.context.runtime;

import java.util.concurrent.ExecutorService;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;

/**
 * The runtime value service used to create values related to the MP-JWT services
 */
@Template
public class SmallRyeContextPropagationTemplate {

    public void configure(BeanContainer container, ExecutorService executorService) {
        SmallRyeContextPropagationProvider cpProvider = container.instance(SmallRyeContextPropagationProvider.class);
        cpProvider.initialize(executorService);
    }
}
