package io.quarkus.kubernetes.deployment;

import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class KubernetesDeployerPrerequisite {

    @BuildStep(onlyIf = IsNormal.class)
    public void prepare(ContainerImageInfoBuildItem containerImage,
            BuildProducer<ContainerImageBuildRequestBuildItem> buildRequestProducer,
            BuildProducer<ContainerImagePushRequestBuildItem> pushRequestProducer) {

        // we don't want to throw an exception at this step and fail the build because it could prevent
        // the Kubernetes resources from being generated
        if (!KubernetesDeploy.INSTANCE.checkSilently()) {
            return;
        }

        //Let's communicate to the container-image plugin that we need an image build and an image push.
        buildRequestProducer.produce(new ContainerImageBuildRequestBuildItem());
        if (containerImage.getRegistry().isPresent()) {
            pushRequestProducer.produce(new ContainerImagePushRequestBuildItem());
        }
    }
}
