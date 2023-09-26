package io.quarkus.arc.deployment;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.arc.runtime.NativeBuildConfigCheck;
import io.quarkus.arc.runtime.NativeBuildConfigCheckInterceptor;
import io.quarkus.arc.runtime.NativeBuildConfigContext;
import io.quarkus.arc.runtime.NativeBuildConfigContextCreator;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

public class NativeBuildConfigSteps {

    @BuildStep(onlyIf = NativeBuild.class)
    SyntheticBeanBuildItem registerNativeBuildConfigContext(ConfigurationBuildItem config,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinished) {

        // Collect all @ConfigProperty injection points
        Set<String> injectedProperties = new HashSet<>();
        for (InjectionPointInfo injectionPoint : beanDiscoveryFinished.getInjectionPoints()) {
            if (injectionPoint.hasDefaultedQualifier()) {
                continue;
            }
            AnnotationInstance configProperty = injectionPoint.getRequiredQualifier(ConfigBuildStep.MP_CONFIG_PROPERTY_NAME);
            if (configProperty != null) {
                injectedProperties.add(configProperty.value("name").asString());
            }
        }

        // Retain only BUILD_AND_RUN_TIME_FIXED properties
        injectedProperties.retainAll(config.getReadResult().getBuildTimeRunTimeValues().keySet());

        return SyntheticBeanBuildItem.configure(NativeBuildConfigContext.class)
                .param(
                        "buildAndRunTimeFixed",
                        injectedProperties.toArray(String[]::new))
                .creator(NativeBuildConfigContextCreator.class)
                .scope(Singleton.class)
                .done();

    }

    @BuildStep(onlyIf = NativeBuild.class)
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(NativeBuildConfigCheckInterceptor.class,
                NativeBuildConfigCheck.class).build();
    }

    @BuildStep(onlyIf = NativeBuild.class)
    AnnotationsTransformerBuildItem transformConfigProducer() {
        DotName configProducerName = DotName.createSimple("io.smallrye.config.inject.ConfigProducer");

        return new AnnotationsTransformerBuildItem(AnnotationsTransformer.appliedToMethod().whenMethod(m -> {
            // Apply to all producer methods declared on io.smallrye.config.inject.ConfigProducer
            return m.declaringClass().name().equals(configProducerName)
                    && m.hasAnnotation(DotNames.PRODUCES)
                    && m.hasAnnotation(ConfigBuildStep.MP_CONFIG_PROPERTY_NAME);
        }).thenTransform(t -> {
            t.add(NativeBuildConfigCheck.class);
        }));
    }

}
