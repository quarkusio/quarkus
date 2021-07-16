package io.quarkus.jaeger.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.jaeger.runtime.JaegerDeploymentRecorder;
import io.quarkus.jaeger.runtime.ZipkinConfig;
import io.quarkus.jaeger.runtime.ZipkinReporterProvider;

public class ZipkinProcessor {

    static final String REGISTRY_CLASS_NAME = "zipkin2.reporter.urlconnection.URLConnectionSender";
    static final Class<?> REGISTRY_CLASS = JaegerDeploymentRecorder.getClassForName(REGISTRY_CLASS_NAME);

    public static class ZipkinEnabled implements BooleanSupplier {
        ZipkinConfig config;

        public boolean getAsBoolean() {
            return REGISTRY_CLASS != null && config.compatibilityMode;
        }
    }

    @BuildStep(onlyIf = ZipkinEnabled.class)
    void addZipkinClasses(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {

        // Add Zipkin classes
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(ZipkinReporterProvider.class)
                .setUnremovable().build());

    }
}
