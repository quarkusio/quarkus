package io.quarkus.container.image.deployment;

import java.util.Collections;
import java.util.Optional;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ImageReference;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;

public class ContainerProcessor {

    @BuildStep
    public ContainerImageInfoBuildItem publishImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig, Capabilities capabilities) {

        ensureSingleContainerImageExtension(capabilities);

        if (containerImageConfig.image.isPresent()) {
            ImageReference imageReference = ImageReference.parse(containerImageConfig.image.get());
            String repository = imageReference.getRepository();
            Optional<String> group = Optional.empty();
            String name = repository;
            int separatorIndex = repository.lastIndexOf('/');
            if (separatorIndex != -1) {
                group = Optional.of(repository.substring(0, separatorIndex));
                name = repository.substring(separatorIndex + 1);
            }
            return new ContainerImageInfoBuildItem(Optional.of(imageReference.getRegistry()), group, name,
                    imageReference.getTag(), Collections.emptyList());
        }

        return new ContainerImageInfoBuildItem(containerImageConfig.registry,
                containerImageConfig.getEffectiveGroup(),
                containerImageConfig.name.orElse(app.getName()),
                containerImageConfig.tag.orElse(app.getVersion()),
                containerImageConfig.additionalTags.orElse(Collections.emptyList()));
    }

    private void ensureSingleContainerImageExtension(Capabilities capabilities) {
        ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities);
    }
}
