package io.quarkus.csrf.reactive.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.csrf.reactive.runtime.CsrfRequestResponseReactiveFilter;
import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

@BuildSteps(onlyIf = CsrfReactiveBuildStep.IsEnabled.class)
public class CsrfReactiveBuildStep {

    @BuildStep
    void registerProvider(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CsrfRequestResponseReactiveFilter.class));
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(CsrfRequestResponseReactiveFilter.class)
                .reason(getClass().getName())
                .methods().fields().build());
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(CsrfTokenParameterProvider.class));
        additionalIndexedClassesBuildItem
                .produce(new AdditionalIndexedClassesBuildItem(CsrfRequestResponseReactiveFilter.class.getName()));
    }

    public static class IsEnabled implements BooleanSupplier {
        RestCsrfBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
