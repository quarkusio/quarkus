package io.quarkus.kubernetes.config.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RunTimeConfigurationSourceValueBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.KubernetesConfigRecorder;
import io.quarkus.kubernetes.client.runtime.KubernetesConfigSourceConfig;

public class KubernetesConfigProcessor {

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public RunTimeConfigurationSourceValueBuildItem configure(KubernetesConfigRecorder recorder,
            KubernetesConfigSourceConfig config, KubernetesClientBuildConfig clientConfig) {
        return new RunTimeConfigurationSourceValueBuildItem(
                recorder.configSources(config, clientConfig));
    }
}
