package io.quarkus.axon.runtime;

import org.axonframework.config.Configuration;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.annotations.Template;

@Template
public class AxonRuntimeTemplate {

    private static final Logger log = Logger.getLogger(AxonRuntimeTemplate.class);

    public void setAxonBuildConfig(BeanContainer container, AxonRuntimeConfig axonRuntimeConfig) {
        container.instance(AxonClientProducer.class).setAxonRuntimeConfig(axonRuntimeConfig);
    }

    public void registerSaga(BeanContainer container, Class<?> axonAnnotatedClass) {
        container.instance(AxonClientProducer.class).registerSaga(container.instance(axonAnnotatedClass));
    }

    public void registerAggregate(BeanContainer container, Class<?> axonAnnotatedClass) {
        container.instance(AxonClientProducer.class).registerAggregate(container.instance(axonAnnotatedClass));
    }

    public void registerEventHandler(BeanContainer container, Class<?> axonAnnotatedClass) {
        container.instance(AxonClientProducer.class).registerEventHandler(container.instance(axonAnnotatedClass));
    }

    public void registerCommandHandler(BeanContainer container, Class<?> axonAnnotatedClass) {
        container.instance(AxonClientProducer.class).registerCommandHandler(container.instance(axonAnnotatedClass));
    }

    public void registerQueryHandler(BeanContainer container, Class<?> axonAnnotatedClass) {
        container.instance(AxonClientProducer.class).registerQueryHandler(container.instance(axonAnnotatedClass));
    }

    public Configuration initializeAxonClient(BeanContainer container) {
        Configuration configuration = container.instance(Configuration.class);

        configuration.onStart(() -> {
            log.info("Axon framework started");
        });

        configuration.onShutdown(() -> {
            log.info("Axon framework stopped");
        });

        return configuration;
    }

    public void injectBeanContainerIntoBeanResolverFactory(BeanContainer container) {
        AxonArcBeanResolverFactory.setBeanContainer(container);
    }
}
