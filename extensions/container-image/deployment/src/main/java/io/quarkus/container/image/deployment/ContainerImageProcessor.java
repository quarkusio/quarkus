package io.quarkus.container.image.deployment;

import java.util.Collections;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.FallbackContainerImageRegistryBuildItem;
import io.quarkus.container.spi.ImageReference;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.SuppressNonRuntimeConfigChangedWarningBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.steps.NativeSourcesBuild;

public class ContainerImageProcessor {

    private static final String UNKNOWN_USER = "?";
    private static final Logger log = Logger.getLogger(ContainerImageProcessor.class);

    @BuildStep(onlyIf = NativeSourcesBuild.class)
    void failForNativeSources(BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {
        throw new IllegalArgumentException(
                "The Container Image extensions are incompatible with the 'native-sources' package type.");
    }

    @BuildStep
    public void ignoreCredentialsChange(BuildProducer<SuppressNonRuntimeConfigChangedWarningBuildItem> producer) {
        producer.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.container-image.username"));
        producer.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.container-image.password"));
    }

    @BuildStep
    public void publishImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig,
            Optional<FallbackContainerImageRegistryBuildItem> containerImageRegistry,
            Capabilities capabilities,
            BuildProducer<ContainerImageInfoBuildItem> containerImage) {

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

        String effectiveGroup = containerImageConfig.getEffectiveGroup().orElse("");
        //This can be the case when running inside kubernetes/minikube in dev-mode. Instead of failing, we should just catch and log.
        if (UNKNOWN_USER.equals(effectiveGroup)) {
            log.warn(
                    "Can't get the current user name, which is used as default for container image group. Can't publish container image info.");
            return;
        }

        // if the user supplied the entire image string, use it
        if (containerImageConfig.image.isPresent()) {
            ImageReference imageReference = ImageReference.parse(containerImageConfig.image.get());
            String repository = imageReference.getRepository();
            containerImage.produce(new ContainerImageInfoBuildItem(Optional.of(imageReference.getRegistry()), repository,
                    imageReference.getTag(), containerImageConfig.additionalTags.orElse(Collections.emptyList())));
            return;
        }

        String registry = containerImageConfig.registry
                .orElseGet(() -> containerImageRegistry.map(FallbackContainerImageRegistryBuildItem::getRegistry).orElse(null));
        if ((registry != null) && !ImageReference.isValidRegistry(registry)) {
            throw new IllegalArgumentException("The supplied container-image registry '" + registry + "' is invalid");
        }

        String effectiveName = containerImageConfig.name.orElse(app.getName());
        String repository = (containerImageConfig.getEffectiveGroup().map(s -> s + "/").orElse("")) + effectiveName;
        if (!ImageReference.isValidRepository(repository)) {
            throw new IllegalArgumentException("The supplied combination of container-image group '"
                    + effectiveGroup + "' and name '" + effectiveName + "' is invalid");
        }

        final String effectiveTag = containerImageConfig.tag.orElse(app.getVersion());
        if (!ImageReference.isValidTag(effectiveTag)) {
            throw new IllegalArgumentException("The supplied container-image tag '" + effectiveTag + "' is invalid");
        }

        containerImage.produce(new ContainerImageInfoBuildItem(Optional.ofNullable(registry),
                containerImageConfig.getEffectiveGroup(),
                effectiveName, effectiveTag,
                containerImageConfig.additionalTags.orElse(Collections.emptyList())));
    }

    private void ensureSingleContainerImageExtension(Capabilities capabilities) {
        ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities);
    }
}
