package io.quarkus.kubernetes.client.deployment;

import static io.quarkus.kubernetes.client.deployment.DevServicesKubernetesProcessor.KUBERNETES_CLIENT_API_SERVER_URL;

import java.util.Map;
import java.util.function.Function;

import org.testcontainers.DockerClientFactory;

import com.dajudge.kindcontainer.KubernetesContainer;
import com.dajudge.kindcontainer.client.KubeConfigUtils;

import io.quarkus.deployment.builditem.Startable;
import io.quarkus.deployment.dev.devservices.RunningContainer;
import io.quarkus.devservices.common.ContainerAddress;
import io.quarkus.devservices.common.ContainerUtil;

public class StartableKubernetesContainer implements Startable {
    private final KubernetesContainer<?> container;
    private Map<String, String> quarkusClientConfiguration;

    public StartableKubernetesContainer(KubernetesContainer<?> container) {
        this.container = container;
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public String getConnectionInfo() {
        return container.getKubeconfig();
    }

    @Override
    public String getContainerId() {
        return container.getContainerId();
    }

    @Override
    public void close() {
        container.stop();
    }

    public KubernetesContainer<?> getContainer() {
        return container;
    }

    public String getServerURL() {
        RunningContainer runningContainer = ContainerUtil.toRunningContainer(container.getContainerInfo());
        final var defaultPort = DevServicesKubernetesProcessor.KUBERNETES_PORT;
        final var port = runningContainer.getPortMapping(defaultPort).orElseThrow(
                () -> new IllegalStateException("No public port found for " + defaultPort));
        final var address = new ContainerAddress(runningContainer, DockerClientFactory.instance().dockerHostIpAddress(), port);
        return address.getUrl();
    }

    String getKubeClientConfigFor(String key) {
        if (quarkusClientConfiguration == null) {
            final var kubeConfig = KubeConfigUtils.parseKubeConfig(getConnectionInfo());
            quarkusClientConfiguration = RunningKubeContainer.getKubernetesClientConfigFromKubeConfig(kubeConfig,
                    getServerURL());
        }
        return quarkusClientConfiguration.get(key);
    }

    private static Function<StartableKubernetesContainer, String> mapperFor(String key) {
        return (StartableKubernetesContainer container) -> container.getKubeClientConfigFor(key);
    }

    static Map<String, Function<StartableKubernetesContainer, String>> getKubernetesClientConfigFromRunningContainerKubeConfig() {
        return Map.of(
                KUBERNETES_CLIENT_API_SERVER_URL, mapperFor(KUBERNETES_CLIENT_API_SERVER_URL),
                "quarkus.kubernetes-client.ca-cert-data",
                mapperFor("quarkus.kubernetes-client.ca-cert-data"),
                "quarkus.kubernetes-client.client-cert-data",
                mapperFor("quarkus.kubernetes-client.client-cert-data"),
                "quarkus.kubernetes-client.client-key-data", mapperFor("quarkus.kubernetes-client.client-key-data"),
                "quarkus.kubernetes-client.client-key-algo", mapperFor("quarkus.kubernetes-client.client-key-algo"),
                "quarkus.kubernetes-client.namespace", mapperFor("quarkus.kubernetes-client.namespace"));
    }
}
