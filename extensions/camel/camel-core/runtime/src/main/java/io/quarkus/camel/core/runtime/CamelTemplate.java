package io.quarkus.camel.core.runtime;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.spi.Registry;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.camel.core.runtime.support.FastCamelRuntime;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Template;

@Template
public class CamelTemplate {

    public RuntimeValue<CamelRuntime> create(
            Registry registry,
            Properties properties,
            List<RuntimeValue<?>> builders) {

        FastCamelRuntime runtime = new FastCamelRuntime();

        runtime.setRegistry(registry);
        runtime.setProperties(properties);
        runtime.setBuilders(builders.stream()
                .map(RuntimeValue::getValue)
                .map(RoutesBuilder.class::cast)
                .collect(Collectors.toList()));

        return new RuntimeValue<>(runtime);
    }

    public void init(
            BeanContainer beanContainer,
            RuntimeValue<CamelRuntime> runtime,
            CamelConfig.BuildTime buildTimeConfig) throws Exception {

        ((FastCamelRuntime) runtime.getValue()).setBeanContainer(beanContainer);
        runtime.getValue().init(buildTimeConfig);
    }

    public void start(
            ShutdownContext shutdown,
            RuntimeValue<CamelRuntime> runtime,
            CamelConfig.Runtime runtimeConfig) throws Exception {

        runtime.getValue().start(runtimeConfig);

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

    public BeanContainerListener initRuntimeInjection(RuntimeValue<CamelRuntime> runtime) {
        return container -> container.instance(CamelProducers.class).setCamelRuntime(runtime.getValue());
    }

}
