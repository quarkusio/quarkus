package io.quarkus.kubernetes.service.binding.runtime;

import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;

public class KubernetesServiceBindingProcessor {

    @BuildStep
    void capabilities(BuildProducer<CapabilityBuildItem> capability) {
        capability.produce(new CapabilityBuildItem(Capability.KUBERNETES_SERVICE_BINDING));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem configure(KubernetesServiceBindingRecorder recorder,
            KubernetesServiceBindingConfig config) {
        return new RunTimeConfigurationSourceValueBuildItem(recorder.configSources(config));
    }
}
