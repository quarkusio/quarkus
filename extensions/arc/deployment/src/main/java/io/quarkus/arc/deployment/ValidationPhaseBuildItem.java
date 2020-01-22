package io.quarkus.arc.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BeanProcessor;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;

/**
 * Validation phase can be used to validate the deployment.
 * <p>
 * An extension that needs to produce other build items during the "validation" phase should use this build item. The
 * build step should produce a {@link ValidationErrorBuildItem} or at least inject a {@link BuildProducer} for this build
 * item, otherwise it could be ignored or processed at the wrong time, e.g. after
 * {@link ArcProcessor#generateResources(io.quarkus.arc.runtime.ArcRecorder, io.quarkus.deployment.builditem.ShutdownContextBuildItem, ValidationPhaseBuildItem, List, List, BuildProducer, BuildProducer, BuildProducer, BuildProducer, BuildProducer)}.
 * 
 * @see ValidationErrorBuildItem
 */
public final class ValidationPhaseBuildItem extends SimpleBuildItem {

    private final BeanProcessor beanProcessor;
    private final BeanDeploymentValidator.ValidationContext context;

    public ValidationPhaseBuildItem(BeanDeploymentValidator.ValidationContext context, BeanProcessor beanProcessor) {
        this.context = context;
        this.beanProcessor = beanProcessor;
    }

    public BeanDeploymentValidator.ValidationContext getContext() {
        return context;
    }

    BeanProcessor getBeanProcessor() {
        return beanProcessor;
    }

    public static final class ValidationErrorBuildItem extends MultiBuildItem {

        private final List<Throwable> values;

        public ValidationErrorBuildItem(Throwable... values) {
            this.values = Arrays.asList(values);
        }

        public List<Throwable> getValues() {
            return values;
        }

    }

}
