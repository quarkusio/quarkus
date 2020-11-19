package io.quarkus.kubernetes.client.deployment;

import static io.quarkus.kubernetes.client.runtime.KubernetesClientUtils.*;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.runtime.TlsConfig;

public class KubernetesClientBuildStep {

    private KubernetesClientBuildConfig buildConfig;

    @BuildStep
    public KubernetesClientBuildItem process(TlsConfig tlsConfig) {
        return new KubernetesClientBuildItem(createClient(buildConfig, tlsConfig));
    }
}
