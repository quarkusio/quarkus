package io.quarkus.it.bootstrap.config.extension.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.it.bootstrap.config.extension.DummyBootstrapRecorder;
import io.quarkus.it.bootstrap.config.extension.DummyBootstrapRecorder2;
import io.quarkus.it.bootstrap.config.extension.DummyConfig;

public class DummyBootstrapConfigBuildStep {
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
