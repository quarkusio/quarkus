package io.quarkus.kind.deployment;

import java.util.List;

import org.jboss.logging.Logger;

import io.dekorate.kubernetes.annotation.ImagePullPolicy;
import io.quarkus.container.spi.ContainerImageBuilderBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.util.ExecUtil;
import io.quarkus.kubernetes.deployment.KubernetesConfig;

public class KindProcessor {

    private static final Logger LOGGER = Logger.getLogger(KindProcessor.class);

    @BuildStep
    public void postBuild(ContainerImageInfoBuildItem image, KubernetesConfig kubernetesConfig,
            List<ContainerImageBuilderBuildItem> builders,
            @SuppressWarnings("unused") BuildProducer<ArtifactResultBuildItem> artifactResults) {
        boolean isLoadSupported = builders.stream().anyMatch(b -> b.getBuilder().equals("docker")
                || b.getBuilder().equals("jib")
                || b.getBuilder().equals("buildpack"));
        if (isLoadSupported) {
            if (kubernetesConfig.getImagePullPolicy() == ImagePullPolicy.Always) {
                LOGGER.warn("The image pull policy is `always` (this is the default value), so Kubernetes will always pull the "
                        + "image from the registry, not from the Kind images. If this was not intentional, set the property "
                        + "`quarkus.kubernetes.image-pull-policy` to either `if-not-present` or `never`.");
            } else {
                ExecUtil.exec("kind", "load", "docker-image", image.getImage());
            }
        }
    }
}
