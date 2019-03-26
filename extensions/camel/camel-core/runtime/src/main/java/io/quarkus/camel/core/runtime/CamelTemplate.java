package io.quarkus.camel.core.runtime;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.camel.RoutesBuilder;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

@Template
public class CamelTemplate {

    public RuntimeValue<CamelRuntime> init(
            RuntimeValue<?> iruntime,
            RuntimeRegistry registry,
            Properties properties,
            List<RuntimeValue<?>> builders) {
        CamelRuntime runtime = CamelRuntime.class.cast(iruntime.getValue());
        runtime.setRegistry(registry);
        runtime.setProperties(properties);
        runtime.setBuilders(builders.stream()
                .map(RuntimeValue::getValue)
                .map(RoutesBuilder.class::cast)
                .collect(Collectors.toList()));
        runtime.init();
        return new RuntimeValue<>(runtime);
    }

    public void start(final ShutdownContext shutdown, final RuntimeValue<CamelRuntime> runtime) throws Exception {
        runtime.getValue().start();

        //in development mode undertow is started eagerly
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    runtime.getValue().stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

}
