package io.quarkus.kubernetes.deployment;

import java.util.List;
import java.util.Optional;

import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.container.spi.FallbackContainerImageRegistryBuildItem;
import io.quarkus.deployment.IsNormalNotRemoteDev;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;

public class KubernetesDeployerPrerequisite {

    @BuildStep(onlyIf = IsNormalNotRemoteDev.class)
    public void prepare(ContainerImageInfoBuildItem containerImage, KubernetesClientBuildItem kubernetesClientBuilder,
            Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            Optional<FallbackContainerImageRegistryBuildItem> fallbackRegistry,
            List<PreventImplicitContainerImagePushBuildItem> preventImplicitContainerImagePush,
            BuildProducer<ContainerImageBuildRequestBuildItem> buildRequestProducer,
            BuildProducer<ContainerImagePushRequestBuildItem> pushRequestProducer) {

        // we don't want to throw an exception at this step and fail the build because it could prevent
        // the Kubernetes resources from being generated
        if (!KubernetesDeploy.INSTANCE.checkSilently(kubernetesClientBuilder)
                || !selectedDeploymentTarget.isPresent()) {
            return;
        }

        // Let's communicate to the container-image plugin that we need an image build and an image push.
        buildRequestProducer.produce(new ContainerImageBuildRequestBuildItem());
        // When a registry is present, we want to push the image
        // However we need to make sure we don't push to the registry when deploying to a local cluster
        // since all updates are meant to find the image from the docker daemon
        boolean registryIsPresent = containerImage.getRegistry().isPresent() || fallbackRegistry.isPresent();
        if (registryIsPresent && preventImplicitContainerImagePush.isEmpty()) {
            pushRequestProducer.produce(new ContainerImagePushRequestBuildItem());
        }
    }
}
