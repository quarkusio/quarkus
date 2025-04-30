package io.quarkus.extest.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.extest.runtime.def.ControllerConfigurationProducer;
import io.quarkus.extest.runtime.def.ControllerConfigurationRecorder;
import io.quarkus.extest.runtime.def.QuarkusControllerConfiguration;

public class ControllerConfigurationBuildStep {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void record(ControllerConfigurationRecorder recorder, BuildProducer<AdditionalBeanBuildItem> producer) {
        producer.produce(AdditionalBeanBuildItem.builder().addBeanClasses(ControllerConfigurationProducer.class).build());
        recorder.setControllerConfiguration(new QuarkusControllerConfiguration("test1", "test2"));
    }
}
