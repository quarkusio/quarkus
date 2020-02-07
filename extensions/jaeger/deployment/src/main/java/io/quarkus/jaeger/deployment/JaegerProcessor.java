package io.quarkus.jaeger.deployment;

import javax.inject.Inject;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jaeger.runtime.JaegerBuildTimeConfig;
import io.quarkus.jaeger.runtime.JaegerConfig;
import io.quarkus.jaeger.runtime.JaegerDeploymentRecorder;
import io.quarkus.runtime.ApplicationConfig;

public class JaegerProcessor {

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void setupTracer(JaegerDeploymentRecorder jdr, JaegerBuildTimeConfig buildTimeConfig, JaegerConfig jaeger,
            ApplicationConfig appConfig) {

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.JAEGER));

        if (buildTimeConfig.enabled) {
            jdr.registerTracer(jaeger, appConfig);
        }
    }

    @BuildStep
    public void build(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.JAEGER));
    }

}
