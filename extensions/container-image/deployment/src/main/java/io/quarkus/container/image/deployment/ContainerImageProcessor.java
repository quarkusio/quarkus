package io.quarkus.container.image.deployment;

import java.util.Collections;
import java.util.Optional;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ImageReference;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ContainerImageProcessor {

    @BuildStep
    public ContainerImageInfoBuildItem publishImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig, Capabilities capabilities) {

        ensureSingleContainerImageExtension(capabilities);

        // additionalTags are used even containerImageConfig.image is set because that string cannot contain multiple tags
        if (containerImageConfig.additionalTags.isPresent()) {
            for (String additionalTag : containerImageConfig.additionalTags.get()) {
                if (!ImageReference.isValidTag(additionalTag)) {
                    throw new IllegalArgumentException(
                            "The supplied additional container-image tag '" + additionalTag + "' is invalid");
                }
            }
        }

        // if the user supplied the entire image string, use it
        if (containerImageConfig.image.isPresent()) {
            ImageReference imageReference = ImageReference.parse(containerImageConfig.image.get());
            String repository = imageReference.getRepository();
            return new ContainerImageInfoBuildItem(Optional.of(imageReference.getRegistry()), repository,
                    imageReference.getTag(), containerImageConfig.additionalTags.orElse(Collections.emptyList()));
        }

        String registry = containerImageConfig.registry.orElse(null);
        if ((registry != null) && !ImageReference.isValidRegistry(registry)) {
            throw new IllegalArgumentException("The supplied container-image registry '" + registry + "' is invalid");
        }

        String effectiveName = containerImageConfig.name.orElse(app.getName());
        String repository = (containerImageConfig.getEffectiveGroup().map(s -> s + "/").orElse("")) + effectiveName;
        if (!ImageReference.isValidRepository(repository)) {
            throw new IllegalArgumentException("The supplied combination of container-image group '"
                    + containerImageConfig.getEffectiveGroup().orElse("") + "' and name '" + effectiveName + "' is invalid");
        }

        final String effectiveTag = containerImageConfig.tag.orElse(app.getVersion());
        if (!ImageReference.isValidTag(effectiveTag)) {
            throw new IllegalArgumentException("The supplied container-image tag '" + effectiveTag + "' is invalid");
        }

        return new ContainerImageInfoBuildItem(containerImageConfig.registry,
                containerImageConfig.getEffectiveGroup(),
                effectiveName, effectiveTag,
                containerImageConfig.additionalTags.orElse(Collections.emptyList()));
    }

    private void ensureSingleContainerImageExtension(Capabilities capabilities) {
        ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities);
    }
}
