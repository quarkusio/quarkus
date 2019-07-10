package io.quarkus.arc.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BeanRegistrar.RegistrationContext;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;

/**
 * An extension that needs to produce other build items during the "bean registration" phase should use this build item. The
 * build step should produce a {@link BeanConfiguratorBuildItem} or at least inject a {@link BuildProducer} for this build item,
 * otherwise it could be ignored or processed at the wrong time, e.g. after
 * {@link ArcProcessor#validate(BeanRegistrationPhaseBuildItem, List)}.
 * 
 * @see BeanConfiguratorBuildItem
 */
public final class BeanRegistrationPhaseBuildItem extends SimpleBuildItem {

    private final BeanProcessor beanProcessor;
    private final BeanRegistrar.RegistrationContext context;

    public BeanRegistrationPhaseBuildItem(RegistrationContext context, BeanProcessor beanProcessor) {
        this.context = context;
        this.beanProcessor = beanProcessor;
    }

    public BeanRegistrar.RegistrationContext getContext() {
        return context;
    }

    BeanProcessor getBeanProcessor() {
        return beanProcessor;
    }

    public static final class BeanConfiguratorBuildItem extends MultiBuildItem {

        private final List<BeanConfigurator<?>> values;

        public BeanConfiguratorBuildItem(BeanConfigurator<?>... values) {
            this.values = Arrays.asList(values);
        }

        public List<BeanConfigurator<?>> getValues() {
            return values;
        }

    }

}
