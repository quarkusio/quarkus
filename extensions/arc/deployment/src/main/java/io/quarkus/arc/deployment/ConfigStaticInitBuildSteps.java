package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.runtime.ConfigStaticInitCheck;
import io.quarkus.arc.runtime.ConfigStaticInitCheckInterceptor;
import io.quarkus.arc.runtime.ConfigStaticInitValues;
import io.quarkus.deployment.annotations.BuildStep;

public class ConfigStaticInitBuildSteps {

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(ConfigStaticInitCheckInterceptor.class, ConfigStaticInitValues.class,
                        ConfigStaticInitCheck.class)
                .build();
    }

    @BuildStep
    AnnotationsTransformerBuildItem transformConfigProducer() {
        DotName configProducerName = DotName.createSimple("io.smallrye.config.inject.ConfigProducer");

        return new AnnotationsTransformerBuildItem(AnnotationsTransformer.appliedToMethod().whenMethod(m -> {
            // Apply to all producer methods declared on io.smallrye.config.inject.ConfigProducer
            return m.declaringClass().name().equals(configProducerName)
                    && m.hasAnnotation(DotNames.PRODUCES)
                    && m.hasAnnotation(ConfigBuildStep.MP_CONFIG_PROPERTY_NAME);
        }).thenTransform(t -> {
            t.add(ConfigStaticInitCheck.class);
        }));
    }
}
