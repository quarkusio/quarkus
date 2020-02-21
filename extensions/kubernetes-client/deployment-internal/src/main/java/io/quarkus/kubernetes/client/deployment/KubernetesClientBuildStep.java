package io.quarkus.kubernetes.client.deployment;

import static io.quarkus.kubernetes.client.runtime.KubernetesClientUtils.*;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;

public class KubernetesClientBuildStep {

    private KubernetesClientBuildConfig buildConfig;

    @BuildStep
    public KubernetesClientBuildItem process() {
        return new KubernetesClientBuildItem(createClient(buildConfig));
    }
}
