package io.quarkus.kubernetes.client.deployment;

import static io.quarkus.kubernetes.client.deployment.DevServicesKubernetesProcessor.KUBERNETES_CLIENT_API_SERVER_URL;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;

import com.dajudge.kindcontainer.client.KubeConfigUtils;
import com.dajudge.kindcontainer.client.config.ClusterSpec;
import com.dajudge.kindcontainer.client.config.KubeConfig;
import com.dajudge.kindcontainer.client.config.UserSpec;
import com.github.dockerjava.api.command.InspectContainerResponse;

import io.fabric8.kubernetes.client.Config;
import io.quarkus.devservices.common.ContainerAddress;

abstract class RunningKubeContainer implements ContainerState {
    private final ContainerAddress containerAddress;
    private final InspectContainerResponse containerInfo;
    private KubeConfig kubeConfig;

    RunningKubeContainer(ContainerAddress containerAddress, InspectContainerResponse containerInfo) {
        this.containerAddress = containerAddress;
        this.containerInfo = containerInfo;
    }

    static RunningKubeContainer create(ContainerAddress containerAddress) {
        try (final var docker = DockerClientFactory.lazyClient()) {
            final var containerInfo = docker.inspectContainerCmd(containerAddress.getId()).exec();
            var image = containerInfo.getConfig().getImage();
            assert image != null;
            if (image.contains("rancher/k3s")) {
                return new K3SRunningKubeContainer(containerAddress, containerInfo);
            } else if (image.contains("kindest/node")) {
                return new KindRunningKubeContainer(containerAddress, containerInfo);
            } else if (image.contains("k8s.gcr.io/kube-apiserver") ||
                    image.contains("registry.k8s.io/kube-apiserver")) {
                return new APIServerRunningKubeContainer(containerAddress, containerInfo);
            }

            throw new RuntimeException("Unable to proceed with Kubernetes Dev Services with container with image '" + image
                    + "'. Make sure to use a compatible test container flavor.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static KubeConfig getExternalKubeConfig(KubeConfig internalKubeConfig, String optionalServerURLToReplace) {
        return Optional.ofNullable(optionalServerURLToReplace)
                .map(serverURL -> {
                    ClusterSpec cluster = internalKubeConfig.getClusters().get(0).getCluster();
                    cluster.setServer(serverURL);
                    return internalKubeConfig;
                }).orElse(internalKubeConfig);
    }

    static Map<String, String> getKubernetesClientConfigFromKubeConfig(KubeConfig kubeConfig,
            String optionalServerURLToReplace) {
        ClusterSpec cluster = getExternalKubeConfig(kubeConfig, optionalServerURLToReplace)
                .getClusters().get(0).getCluster();
        UserSpec user = kubeConfig.getUsers().get(0).getUser();
        return Map.of(
                KUBERNETES_CLIENT_API_SERVER_URL, cluster.getServer(),
                "quarkus.kubernetes-client.ca-cert-data",
                cluster.getCertificateAuthorityData(),
                "quarkus.kubernetes-client.client-cert-data",
                user.getClientCertificateData(),
                "quarkus.kubernetes-client.client-key-data", user.getClientKeyData(),
                "quarkus.kubernetes-client.client-key-algo", Config.getKeyAlgorithm(null, user.getClientKeyData()),
                "quarkus.kubernetes-client.namespace", "default");
    }

    KubeConfig getKubeConfig() {
        if (kubeConfig == null) {
            kubeConfig = getInternalKubeConfig();
            kubeConfig = getExternalKubeConfig(kubeConfig, serverURLToReplace());
        }
        return kubeConfig;
    }

    String getKubeConfigAsString() {
        final var kubeConfig = getKubeConfig();
        return KubeConfigUtils.serializeKubeConfig(kubeConfig);
    }

    Map<String, String> getKubernetesClientConfig() {
        return getKubernetesClientConfigFromKubeConfig(getKubeConfig(), null);
    }

    protected KubeConfig getInternalKubeConfig() {
        return KubeConfigUtils.parseKubeConfig(getFileContentFromContainer(kubeConfigFilePath()));
    }

    protected abstract String kubeConfigFilePath();

    protected String serverURLToReplace() {
        return containerAddress.getUrl();
    }

    @Override
    public String getContainerId() {
        return containerAddress.getId();
    }

    @Override
    public List<Integer> getExposedPorts() {
        return List.of(containerAddress.getPort());
    }

    @Override
    public InspectContainerResponse getContainerInfo() {
        return containerInfo;
    }

    public String getFileContentFromContainer(String containerPath) {
        return copyFileFromContainer(containerPath, this::readString);
    }

    String readString(final InputStream is) throws IOException {
        return new String(readBytes(is), UTF_8);
    }

    private byte[] readBytes(final InputStream is) throws IOException {
        final byte[] buffer = new byte[1024];
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int read;
        while ((read = is.read(buffer)) > 0) {
            bos.write(buffer, 0, read);
        }
        return bos.toByteArray();
    }
}
