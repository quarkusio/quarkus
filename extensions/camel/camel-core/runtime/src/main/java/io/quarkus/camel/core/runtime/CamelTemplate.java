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

    public CamelRuntime create(
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

        return runtime;
    }

    public void init(
            BeanContainer beanContainer,
            CamelRuntime runtime,
            CamelConfig.BuildTime buildTimeConfig) throws Exception {

        ((FastCamelRuntime) runtime).setBeanContainer(beanContainer);
        runtime.init(buildTimeConfig);
    }

    public void start(
            ShutdownContext shutdown,
            CamelRuntime runtime,
            CamelConfig.Runtime runtimeConfig) throws Exception {

        runtime.start(runtimeConfig);

        //in development mode undertow is started eagerly
        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                try {
                    runtime.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public BeanContainerListener initRuntimeInjection(CamelRuntime runtime) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                container.instance(CamelProducers.class).setCamelRuntime(runtime);
            }
        };
    }
}
