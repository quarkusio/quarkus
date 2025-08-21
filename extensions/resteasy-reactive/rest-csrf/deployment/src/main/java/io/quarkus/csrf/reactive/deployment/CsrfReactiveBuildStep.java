package io.quarkus.csrf.reactive.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.csrf.reactive.runtime.CsrfRequestResponseReactiveFilter;
import io.quarkus.csrf.reactive.runtime.CsrfTokenParameterProvider;
import io.quarkus.csrf.reactive.runtime.RestCsrfBuilder;
import io.quarkus.csrf.reactive.runtime.RestCsrfConfigHolder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.vertx.http.deployment.CsrfBuilderClassBuildItem;

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
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(RestCsrfConfigHolder.class));
    }

    @BuildStep
    CsrfBuilderClassBuildItem registerCsrfBuilder() {
        return new CsrfBuilderClassBuildItem(RestCsrfBuilder.class);
    }

    public static class IsEnabled implements BooleanSupplier {
        RestCsrfBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled();
        }
    }
}
