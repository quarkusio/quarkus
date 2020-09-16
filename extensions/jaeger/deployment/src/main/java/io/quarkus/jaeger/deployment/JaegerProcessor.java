package io.quarkus.jaeger.deployment;

import java.util.Optional;

import javax.inject.Inject;

import io.jaegertracing.internal.JaegerTracer;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.jaeger.runtime.JaegerBuildTimeConfig;
import io.quarkus.jaeger.runtime.JaegerConfig;
import io.quarkus.jaeger.runtime.JaegerDeploymentRecorder;
import io.quarkus.runtime.ApplicationConfig;
import io.quarkus.runtime.metrics.MetricsFactory;

public class JaegerProcessor {

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    @BuildStep(onlyIf = NativeBuild.class)
    @Record(ExecutionTime.STATIC_INIT)
    void setVersion(JaegerDeploymentRecorder jdr) {
        jdr.setJaegerVersion(JaegerTracer.getVersionFromProperties());
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupTracer(JaegerDeploymentRecorder jdr, JaegerBuildTimeConfig buildTimeConfig, JaegerConfig jaeger,
            ApplicationConfig appConfig, Optional<MetricsCapabilityBuildItem> metricsCapability) {

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.JAEGER.getName()));

        if (buildTimeConfig.enabled) {
            if (buildTimeConfig.metricsEnabled && metricsCapability.isPresent()) {
                if (metricsCapability.get().metricsSupported(MetricsFactory.MICROMETER)) {
                    jdr.registerTracerWithMicrometerMetrics(jaeger, appConfig);
                } else {
                    jdr.registerTracerWithMpMetrics(jaeger, appConfig);
                }
            } else {
                jdr.registerTracerWithoutMetrics(jaeger, appConfig);
            }
        }
    }

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.JAEGER));
    }

    @BuildStep
    public void capability(JaegerBuildTimeConfig buildTimeConfig,
            BuildProducer<CapabilityBuildItem> capability) {
        if (buildTimeConfig.enabled) {
            capability.produce(new CapabilityBuildItem(Capability.OPENTRACING));
        }
    }

    @BuildStep
    public void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder("io.jaegertracing.internal.samplers.http.SamplingStrategyResponse",
                        "io.jaegertracing.internal.samplers.http.ProbabilisticSamplingStrategy")
                .finalFieldsWritable(true)
                .build());
    }
}
