package io.quarkus.jaeger.deployment;

import java.util.Optional;

import io.jaegertracing.internal.JaegerTracer;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.jaeger.runtime.JaegerBuildTimeConfig;
import io.quarkus.jaeger.runtime.JaegerConfig;
import io.quarkus.jaeger.runtime.JaegerDeploymentRecorder;
import io.quarkus.jaeger.runtime.ZipkinConfig;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.metrics.MetricsFactory;

public class JaegerProcessor {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    @Record(ExecutionTime.STATIC_INIT)
    void setVersion(JaegerDeploymentRecorder jdr) {
        jdr.setJaegerVersion(JaegerTracer.getVersionFromProperties());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    ExtensionSslNativeSupportBuildItem setupTracer(JaegerDeploymentRecorder jdr, JaegerBuildTimeConfig buildTimeConfig,
            JaegerConfig jaeger, ApplicationConfig appConfig, Optional<MetricsCapabilityBuildItem> metricsCapability,
            ZipkinConfig zipkinConfig) {

        if (buildTimeConfig.enabled) {
            if (buildTimeConfig.metricsEnabled && metricsCapability.isPresent()) {
                if (metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
                    jdr.registerTracerWithMicrometerMetrics(jaeger, appConfig, zipkinConfig);
                } else {
                    jdr.registerTracerWithMpMetrics(jaeger, appConfig, zipkinConfig);
                }
            } else {
                jdr.registerTracerWithoutMetrics(jaeger, appConfig, zipkinConfig);
            }
        }

        // Indicates that this extension would like the SSL support to be enabled
        return new ExtensionSslNativeSupportBuildItem(Feature.JAEGER.getName());
    }

    @BuildStep
    public FeatureBuildItem build() {
        return new FeatureBuildItem(Feature.JAEGER);
    }

    @BuildStep
    public ReflectiveClassBuildItem reflectiveClasses() {
        return ReflectiveClassBuildItem
                .builder("io.jaegertracing.internal.samplers.http.SamplingStrategyResponse",
                        "io.jaegertracing.internal.samplers.http.ProbabilisticSamplingStrategy")
                .finalFieldsWritable(true).build();
    }
}
