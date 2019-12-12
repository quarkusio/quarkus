package io.quarkus.kubernetes.deployment;

import java.util.OptionalInt;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.spi.KubernetesPortBuildItem;

class KubernetesPortProcessor {

    @BuildStep(onlyIf = IsNormal.class)
    public KubernetesPortBuildItem buildTimePort() {
        return new KubernetesPortBuildItem(
                ConfigProvider.getConfig().getValue("quarkus.http.port", OptionalInt.class).orElse(8080),
                "http");
    }
}
