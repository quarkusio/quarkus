package io.quarkus.neo4j.deployment;

import java.util.function.Consumer;

import org.neo4j.driver.Driver;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.metrics.MetricsFactoryConsumerBuildItem;
import io.quarkus.neo4j.runtime.Neo4jConfiguration;
import io.quarkus.neo4j.runtime.Neo4jDriverRecorder;
import io.quarkus.runtime.metrics.MetricsFactory;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class Neo4jDriverProcessor {

    @BuildStep
    FeatureBuildItem createFeature(BuildProducer<ExtensionSslNativeSupportBuildItem> extensionSslNativeSupport) {

        // Indicates that this extension would like the SSL support to be enabled
        extensionSslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.NEO4J));

        return new FeatureBuildItem(Feature.NEO4J);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    Neo4jDriverBuildItem configureDriverProducer(Capabilities capabilities,
            Neo4jDriverRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            Neo4jConfiguration configuration,
            ShutdownContextBuildItem shutdownContext) {

        var transactionsPresent = capabilities.isPresent(Capability.TRANSACTIONS);
        var driverHolder = recorder.initializeDriver(configuration, shutdownContext, transactionsPresent);
        syntheticBeans
                .produce(SyntheticBeanBuildItem.configure(Driver.class).runtimeValue(driverHolder).setRuntimeInit().done());

        return new Neo4jDriverBuildItem(driverHolder);
    }

    @BuildStep
    HealthBuildItem addHealthCheck(Neo4jBuildTimeConfig buildTimeConfig) {
        return new HealthBuildItem("io.quarkus.neo4j.runtime.health.Neo4jHealthCheck",
                buildTimeConfig.healthEnabled);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void metrics(Neo4jConfiguration configuration,
            Neo4jDriverRecorder recorder,
            BuildProducer<MetricsFactoryConsumerBuildItem> metrics) {
        Consumer<MetricsFactory> metricsFactoryConsumer = recorder.registerMetrics(configuration);
        // If metrics for neo4j are disabled, the returned consumer will be null,
        // but in a processor we can't know that (it's controlled by a runtime config property)
        // so the BuildItem might contain null and in that case will be ignored by the metrics recorder
        metrics.produce(new MetricsFactoryConsumerBuildItem(metricsFactoryConsumer));
    }

}
