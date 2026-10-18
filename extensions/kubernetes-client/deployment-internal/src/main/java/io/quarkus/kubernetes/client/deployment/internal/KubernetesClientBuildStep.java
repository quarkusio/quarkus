package io.quarkus.kubernetes.client.deployment.internal;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.QuarkusBuildCloseablesBuildItem;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientUtils;
import io.quarkus.kubernetes.client.runtime.internal.QuarkusHttpClientFactory;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;

public class KubernetesClientBuildStep {

    @BuildStep
    public KubernetesClientBuildItem process(QuarkusBuildCloseablesBuildItem closeablesBuildItem) {
        QuarkusHttpClientFactory httpClientFactory = new QuarkusHttpClientFactory();
        closeablesBuildItem.add(httpClientFactory);
        return new KubernetesClientBuildItem(KubernetesClientUtils.createConfig(), httpClientFactory);
    }
}
