package io.quarkus.kubernetes.client.deployment;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.devservices.common.ContainerAddress;

class K3SRunningKubeContainer extends RunningKubeContainer {
    private static final String K3S_KUBECONFIG = "/etc/rancher/k3s/k3s.yaml";

    K3SRunningKubeContainer(ContainerAddress containerAddress, InspectContainerResponse containerInfo) {
        super(containerAddress, containerInfo);
    }

    @Override
    protected String kubeConfigFilePath() {
        return K3S_KUBECONFIG;
    }
}
