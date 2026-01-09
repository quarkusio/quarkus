package io.quarkus.arc.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import io.quarkus.arc.runtime.ValueRegistryRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.registry.ValueRegistry;

class ValueRegistryProcessor {
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void valueRegistry(
            ValueRegistryRecorder recorder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                .configure(ValueRegistry.class)
                .startup()
                .setRuntimeInit()
                .unremovable()
                .supplier(recorder.valueRegistry())
                .scope(ApplicationScoped.class)
                .addQualifier(Default.class);

        syntheticBeans.produce(configurator.done());
    }
}
