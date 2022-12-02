package io.quarkus.kubernetes.service.binding.runtime;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;

public class KubernetesServiceBindingProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem configure(KubernetesServiceBindingRecorder recorder) {
        return new RunTimeConfigurationSourceValueBuildItem(recorder.configSources());
    }
}
