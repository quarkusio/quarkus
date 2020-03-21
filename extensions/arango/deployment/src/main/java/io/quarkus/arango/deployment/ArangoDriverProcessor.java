package io.quarkus.arango.deployment;

import io.quarkus.arango.runtime.ArangoConfiguration;
import io.quarkus.arango.runtime.ArangoDriverProducer;
import io.quarkus.arango.runtime.ArangoDriverRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class ArangoDriverProcessor {

    @BuildStep
    FeatureBuildItem createFeatureItem(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.ARANGO_DB));

        return new FeatureBuildItem(FeatureBuildItem.ARANGO_DB);
    }

    @BuildStep
    AdditionalBeanBuildItem createArangoDriverProducer() {
        return AdditionalBeanBuildItem.unremovableOf(ArangoDriverProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureArangoDriverProducer(ArangoDriverRecorder recorder, BeanContainerBuildItem beanContainerBuildItem,
                                       ArangoConfiguration configuration,
                                       ShutdownContextBuildItem shutdownContext) {

        recorder.configureArangoProducer(beanContainerBuildItem.getValue(), configuration, shutdownContext);
    }

    @BuildStep
    HealthBuildItem addArangoHealthCheck(ArangoDBBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.arango.runtime.health.ArangoHealthCheck",
                buildTimeConfig.healthEnabled, "arango");
    }
}
