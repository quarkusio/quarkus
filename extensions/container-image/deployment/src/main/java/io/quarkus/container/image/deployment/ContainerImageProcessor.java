package io.quarkus.container.image.deployment;

import static io.quarkus.container.image.deployment.util.EnablementUtil.buildOrPushContainerImageNeeded;
import static io.quarkus.container.spi.ImageReference.DEFAULT_TAG;
import static io.quarkus.deployment.builditem.ApplicationInfoBuildItem.UNSET_VALUE;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.container.spi.ContainerImageBuildRequestBuildItem;
import io.quarkus.container.spi.ContainerImageCustomNameBuildItem;
import io.quarkus.container.spi.ContainerImageInfoBuildItem;
import io.quarkus.container.spi.ContainerImagePushRequestBuildItem;
import io.quarkus.container.spi.FallbackContainerImageRegistryBuildItem;
import io.quarkus.container.spi.ImageReference;
import io.quarkus.container.spi.SingleSegmentContainerImageRequestBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationInfoBuildItem;
import io.quarkus.deployment.builditem.SuppressNonRuntimeConfigChangedWarningBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.steps.NativeSourcesBuild;
import io.quarkus.runtime.util.StringUtil;

public class ContainerImageProcessor {

    private static final Logger log = Logger.getLogger(ContainerImageProcessor.class);

    @BuildStep(onlyIf = NativeSourcesBuild.class)
    void failForNativeSources(ContainerImageConfig containerImageConfig,
            Optional<ContainerImageBuildRequestBuildItem> buildRequest,
            Optional<ContainerImagePushRequestBuildItem> pushRequest,
            BuildProducer<ArtifactResultBuildItem> artifactResultProducer) {
        if (buildOrPushContainerImageNeeded(containerImageConfig, buildRequest, pushRequest)) {
            throw new IllegalArgumentException(
                    "The Container Image extensions are incompatible with the 'native-sources' package type.");
        }
    }

    @BuildStep
    public void ignoreCredentialsChange(BuildProducer<SuppressNonRuntimeConfigChangedWarningBuildItem> producer) {
        producer.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.container-image.username"));
        producer.produce(new SuppressNonRuntimeConfigChangedWarningBuildItem("quarkus.container-image.password"));
    }

    @BuildStep
    public void publishImageInfo(ApplicationInfoBuildItem app,
            ContainerImageConfig containerImageConfig,
            Optional<SingleSegmentContainerImageRequestBuildItem> singleSegmentImageRequest,
            Optional<FallbackContainerImageRegistryBuildItem> containerImageRegistry,
            Optional<ContainerImageCustomNameBuildItem> containerImageCustomName,
            Capabilities capabilities,
            BuildProducer<ContainerImageInfoBuildItem> containerImage) {

        ensureSingleContainerImageExtension(capabilities);

        // additionalTags are used even containerImageConfig.image is set because that
        // string cannot contain multiple tags
        if (containerImageConfig.additionalTags().isPresent()) {
            for (String additionalTag : containerImageConfig.additionalTags().get()) {
                if (!ImageReference.isValidTag(additionalTag)) {
                    throw new IllegalArgumentException(
                            "The supplied additional container-image tag '" + additionalTag + "' is invalid");
                }
            }
        }

        Optional<String> effectiveGroup = getEffectiveGroup(containerImageConfig.group(),
                singleSegmentImageRequest.isPresent());

        // if the user supplied the entire image string, use it
        if (containerImageConfig.image().isPresent()) {
            ImageReference imageReference = ImageReference.parse(containerImageConfig.image().get());
            String repository = imageReference.getRepository();
            if (singleSegmentImageRequest.isPresent() && imageReference.getRepository().contains("/")
                    && imageReference.getRegistry().filter(StringUtil::isNullOrEmpty).isPresent()) {
                log.warn("A single segment image is preferred, but a local multi segment has been provided: "
                        + containerImageConfig.image().get());
            }
            containerImage.produce(new ContainerImageInfoBuildItem(imageReference.getRegistry(),
                    containerImageConfig.username(), containerImageConfig.password(), repository,
                    imageReference.getTag(), containerImageConfig.additionalTags().orElse(Collections.emptyList())));
            return;
        }

        String registry = containerImageConfig.registry()
                .orElseGet(() -> containerImageRegistry.map(FallbackContainerImageRegistryBuildItem::getRegistry)
                        .orElse(null));
        if ((registry != null) && !ImageReference.isValidRegistry(registry)) {
            throw new IllegalArgumentException("The supplied container-image registry '" + registry + "' is invalid");
        }

        String effectiveName = containerImageCustomName.map(ContainerImageCustomNameBuildItem::getName)
                .orElse(containerImageConfig.name());
        String group = effectiveGroup.orElse("");
        String repository = group.isBlank() ? effectiveName : group + "/" + effectiveName;
        if (!ImageReference.isValidRepository(repository)) {
            throw new IllegalArgumentException("The supplied combination of container-image group '"
                    + group + "' and name '" + effectiveName + "' is invalid");
        }

        String effectiveTag = containerImageConfig.tag();
        if (effectiveTag.equals(UNSET_VALUE)) {
            effectiveTag = DEFAULT_TAG;
        }

        if (!ImageReference.isValidTag(effectiveTag)) {
            throw new IllegalArgumentException("The supplied container-image tag '" + effectiveTag + "' is invalid");
        }

        containerImage.produce(new ContainerImageInfoBuildItem(Optional.ofNullable(registry),
                containerImageConfig.username(), containerImageConfig.password(),
                effectiveGroup,
                effectiveName, effectiveTag,
                containerImageConfig.additionalTags().orElse(Collections.emptyList())));
    }

    private void ensureSingleContainerImageExtension(Capabilities capabilities) {
        ContainerImageCapabilitiesUtil.getActiveContainerImageCapability(capabilities);
    }

    /**
     * Since user.name which is default value can be uppercase and uppercase values
     * are not allowed
     * in the repository part of image references, we need to make the username
     * lowercase.
     * If spaces exist in the user name, we replace them with the dash character.
     *
     * We purposely don't change the value of an explicitly set group.
     */
    static Optional<String> getEffectiveGroup(Optional<String> group, boolean isSingleSegmentRequested) {
        return group.or(() -> isSingleSegmentRequested || isGroupSpecified()
                ? Optional.empty()
                : Optional.ofNullable(System.getProperty("user.name")).map(s -> s.replace(' ', '-')))
                .map(String::toLowerCase)
                .filter(Predicate.not(StringUtil::isNullOrEmpty));
    }

    static Optional<String> getEffectiveGroup() {
        return getEffectiveGroup(Optional.empty(), false);
    }

    /**
     * Users are allowed to specify an empty group, however this is mapped to Optional.emtpy().
     * We need to know if the user has actually specified a group or not.
     * The only way is to check the property names provided.
     **/
    static boolean isGroupSpecified() {
        return StreamSupport.stream(ConfigProvider.getConfig().getPropertyNames().spliterator(), false)
                .anyMatch(n -> n.equals("quarkus.container-image.group") || n.equals("QUARKUS_CONTAINER_IMAGE_GROUP"));
    }
}
