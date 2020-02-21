package io.quarkus.kubernetes.deployment;

import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class KubernetesDeployerPrerequisite {

    private final Logger LOGGER = Logger.getLogger(KubernetesDeploy.class);

    @Inject
    BuildProducer<ContainerImageBuildRequestBuildItem> buildRequestProducer;

    @Inject
    BuildProducer<ContainerImagePushRequestBuildItem> pushRequestProducer;

    @BuildStep(onlyIf = KubernetesDeploy.class)
    public void prepare(ContainerImageInfoBuildItem containerImage) {
        //Let's communicate to the container-image plugin that we need an image build and an image push.
        buildRequestProducer.produce(new ContainerImageBuildRequestBuildItem());
        if (containerImage.getRegistry().isPresent()) {
            pushRequestProducer.produce(new ContainerImagePushRequestBuildItem());
        } else {
            LOGGER.warn(
                    "A Kubernetes deployment was requested, but the container image to be built will not be pushed to any registry"
                            +
                            " because \"quarkus.container-image.registry\" has not been set. The Kubernetes deployment will only work properly"
                            +
                            " if the cluster is using the local Docker daemon.");
        }
    }
}
