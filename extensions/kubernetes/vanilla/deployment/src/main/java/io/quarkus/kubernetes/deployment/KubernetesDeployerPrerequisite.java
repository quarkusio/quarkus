package io.quarkus.kubernetes.deployment;

import org.jboss.logging.Logger;

import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;

public class KubernetesDeployerPrerequisite {

    private static final Logger log = Logger.getLogger(KubernetesDeploy.class);

    @BuildStep(onlyIf = KubernetesDeploy.class)
    public void prepare(ContainerImageInfoBuildItem containerImage,
            BuildProducer<ContainerImageBuildRequestBuildItem> buildRequestProducer,
            BuildProducer<ContainerImagePushRequestBuildItem> pushRequestProducer) {
        //Let's communicate to the container-image plugin that we need an image build and an image push.
        buildRequestProducer.produce(new ContainerImageBuildRequestBuildItem());
        if (containerImage.getRegistry().isPresent()) {
            pushRequestProducer.produce(new ContainerImagePushRequestBuildItem());
        }
    }
}
