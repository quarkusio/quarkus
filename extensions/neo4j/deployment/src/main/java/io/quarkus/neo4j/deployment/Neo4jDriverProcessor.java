package io.quarkus.neo4j.deployment;

import javax.inject.Inject;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.neo4j.runtime.Neo4jConfiguration;
import io.quarkus.neo4j.runtime.Neo4jDriverProducer;
import io.quarkus.neo4j.runtime.Neo4jDriverRecorder;

class Neo4jDriverProcessor {

    @Inject
    BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport;

    @BuildStep
    FeatureBuildItem createFeature() {
        return new FeatureBuildItem(FeatureBuildItem.NEO4J);
    }

    @BuildStep
    AdditionalBeanBuildItem createDriverProducer() {
        return AdditionalBeanBuildItem.unremovableOf(Neo4jDriverProducer.class);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureDriverProducer(Neo4jDriverRecorder recorder, BeanContainerBuildItem beanContainerBuildItem,
            Neo4jConfiguration configuration,
            ShutdownContextBuildItem shutdownContext) {

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.NEO4J));

        recorder.configureNeo4jProducer(beanContainerBuildItem.getValue(), configuration, shutdownContext);
    }
}
