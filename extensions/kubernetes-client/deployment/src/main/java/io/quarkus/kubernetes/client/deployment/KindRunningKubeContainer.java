package io.quarkus.kubernetes.client.deployment;

import com.github.dockerjava.api.command.InspectContainerResponse;

import io.quarkus.devservices.common.ContainerAddress;

class KindRunningKubeContainer extends RunningKubeContainer {
    private static final String KIND_KUBECONFIG = "/etc/kubernetes/admin.conf";

    KindRunningKubeContainer(ContainerAddress containerAddress, InspectContainerResponse containerInfo) {
        super(containerAddress, containerInfo);
    }

    @Override
    protected String kubeConfigFilePath() {
        return KIND_KUBECONFIG;
    }
}
