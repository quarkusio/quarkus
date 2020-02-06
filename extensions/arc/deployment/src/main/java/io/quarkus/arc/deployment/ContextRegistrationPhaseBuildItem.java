package io.quarkus.arc.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.ContextConfigurator;
import io.quarkus.arc.processor.ContextRegistrar;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;

/**
 * Context registration phase can be used to register custom CDI contexts.
 * <p>
 * An extension that needs to produce other build items during the "context registration" phase should use this build item. The
 * build step should produce a {@link ContextConfiguratorBuildItem} or at least inject a {@link BuildProducer} for this build
 * item, otherwise it could be ignored or processed at the wrong time, e.g. after
 * {@link ArcProcessor#registerBeans(ContextRegistrationPhaseBuildItem, List)}.
 * 
 * @see ContextConfiguratorBuildItem
 */
public final class ContextRegistrationPhaseBuildItem extends SimpleBuildItem {

    private final BeanProcessor beanProcessor;
    private final ContextRegistrar.RegistrationContext context;

    public ContextRegistrationPhaseBuildItem(ContextRegistrar.RegistrationContext context, BeanProcessor beanProcessor) {
        this.context = context;
        this.beanProcessor = beanProcessor;
    }

    public ContextRegistrar.RegistrationContext getContext() {
        return context;
    }

    BeanProcessor getBeanProcessor() {
        return beanProcessor;
    }

    public static final class ContextConfiguratorBuildItem extends MultiBuildItem {

        private final List<ContextConfigurator> values;

        public ContextConfiguratorBuildItem(ContextConfigurator... values) {
            this.values = Arrays.asList(values);
        }

        public List<ContextConfigurator> getValues() {
            return values;
        }

    }

}
