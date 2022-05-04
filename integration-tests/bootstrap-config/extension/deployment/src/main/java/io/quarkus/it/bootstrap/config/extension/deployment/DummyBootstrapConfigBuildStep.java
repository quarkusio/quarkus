package io.quarkus.it.bootstrap.config.extension.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.deployment.builditem.StaticInitConfigBuilderBuildItem;
import io.quarkus.it.bootstrap.config.extension.DummyBootstrapRecorder;
import io.quarkus.it.bootstrap.config.extension.DummyBootstrapRecorder2;
import io.quarkus.it.bootstrap.config.extension.DummyConfig;
import io.quarkus.it.bootstrap.config.extension.DummyConfigBuilder;

public class DummyBootstrapConfigBuildStep {
    @BuildStep
    public void configBuilders(BuildProducer<StaticInitConfigBuilderBuildItem> staticConfigBuilders,
            BuildProducer<RunTimeConfigBuilderBuildItem> runtimeConfigBuilders) {
        staticConfigBuilders.produce(new StaticInitConfigBuilderBuildItem(DummyConfigBuilder.class.getName()));
        runtimeConfigBuilders.produce(new RunTimeConfigBuilderBuildItem(DummyConfigBuilder.class.getName()));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem dummyRecorder(DummyBootstrapRecorder recorder, DummyConfig dummyConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(recorder.create(dummyConfig));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void dummyRecorder2(DummyBootstrapRecorder2 recorder,
            BuildProducer<RunTimeConfigurationSourceValueBuildItem> producer) {
        producer.produce(new RunTimeConfigurationSourceValueBuildItem(recorder.create()));
    }
}
