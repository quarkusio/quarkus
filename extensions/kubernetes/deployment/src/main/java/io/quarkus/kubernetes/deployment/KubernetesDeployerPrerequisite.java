package io.quarkus.kubernetes.deployment;

import javax.inject.Inject;

import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class KubernetesDeployerPrerequisite {
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
        }
    }
}
