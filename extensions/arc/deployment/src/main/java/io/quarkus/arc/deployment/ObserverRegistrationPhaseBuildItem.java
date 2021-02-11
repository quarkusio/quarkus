package io.quarkus.arc.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.arc.processor.ObserverConfigurator;
import io.quarkus.arc.processor.ObserverRegistrar;
import io.quarkus.arc.processor.ObserverRegistrar.RegistrationContext;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;

/**
 * An extension that needs to produce other build items during the "synthetic observer registration" phase should use this build
 * item. The
 * build step should produce a {@link ObserverConfiguratorBuildItem} or at least inject a {@link BuildProducer} for this build
 * item,
 * otherwise it could be ignored or processed at the wrong time, e.g. after
 * {@link ArcProcessor#validate(ObserverRegistrationPhaseBuildItem, List)}.
 * 
 * @see ObserverConfiguratorBuildItem
 * @see ObserverRegistrar
 */
public final class ObserverRegistrationPhaseBuildItem extends SimpleBuildItem {

    private final BeanProcessor beanProcessor;
    private final RegistrationContext context;

    public ObserverRegistrationPhaseBuildItem(RegistrationContext context, BeanProcessor beanProcessor) {
        this.context = context;
        this.beanProcessor = beanProcessor;
    }

    public RegistrationContext getContext() {
        return context;
    }

    BeanProcessor getBeanProcessor() {
        return beanProcessor;
    }

    public static final class ObserverConfiguratorBuildItem extends MultiBuildItem {

        private final List<ObserverConfigurator> values;

        public ObserverConfiguratorBuildItem(ObserverConfigurator... values) {
            this.values = Arrays.asList(values);
        }

        public List<ObserverConfigurator> getValues() {
            return values;
        }

    }

}
