package org.acme.example.extension.deployment;

import jakarta.inject.Singleton;

import org.acme.example.extension.runtime.ExampleBuildOptions;
import org.acme.example.extension.runtime.ExampleRecorder;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class ExampleProcessor {

    private static final String FEATURE = "example";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    SyntheticBeanBuildItem syntheticBean(ExampleRecorder recorder, ExampleConfig config) {
        return SyntheticBeanBuildItem.configure(ExampleBuildOptions.class)
                .scope(Singleton.class)
                .runtimeValue(recorder.buildOptions(config.name))
                .done();
    }
}
