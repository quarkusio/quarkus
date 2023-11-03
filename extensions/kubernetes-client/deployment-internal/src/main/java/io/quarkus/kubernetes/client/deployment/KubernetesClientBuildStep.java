package io.quarkus.kubernetes.client.deployment;

import static io.quarkus.kubernetes.client.runtime.KubernetesClientUtils.createConfig;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.QuarkusBuildCloseablesBuildItem;
import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.QuarkusHttpClientFactory;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.runtime.TlsConfig;

public class KubernetesClientBuildStep {

    private KubernetesClientBuildConfig buildConfig;

    @BuildStep
    public KubernetesClientBuildItem process(TlsConfig tlsConfig, QuarkusBuildCloseablesBuildItem closeablesBuildItem) {
        QuarkusHttpClientFactory httpClientFactory = new QuarkusHttpClientFactory();
        closeablesBuildItem.add(httpClientFactory);
        return new KubernetesClientBuildItem(createConfig(buildConfig, tlsConfig), httpClientFactory);
    }
}
