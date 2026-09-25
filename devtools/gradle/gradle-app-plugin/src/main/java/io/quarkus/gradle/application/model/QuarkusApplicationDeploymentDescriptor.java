package io.quarkus.gradle.application.model;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

/**
 * Immutable deployment selection derived from one named deployment DSL
 * element.
 *
 * @param name the non-blank deployment name
 * @param target the deployment provider
 * @param imageSource which image-producing path supplies the deployment
 * @param imageReference an explicit reference, required for
 *        {@link QuarkusApplicationDeploymentImageSource#EXISTING_IMAGE}
 */
public record QuarkusApplicationDeploymentDescriptor(String name,
        QuarkusApplicationDeploymentTarget target, QuarkusApplicationDeploymentImageSource imageSource,
        Optional<String> imageReference) {

    /**
     * Creates and validates a deployment descriptor.
     */
    public QuarkusApplicationDeploymentDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Quarkus application deployment requires a name");
        }
        if (target == null) {
            throw new IllegalArgumentException("Quarkus application deployment requires a target");
        }
        if (imageSource == null) {
            throw new IllegalArgumentException("Quarkus application deployment requires an image source");
        }
        requireNonNull(imageReference, "imageReference");
        if (imageSource == QuarkusApplicationDeploymentImageSource.EXISTING_IMAGE && imageReference.isEmpty()) {
            throw new IllegalArgumentException("Existing-image deployments require an image reference");
        }
    }

    /**
     * Creates a deployment that consumes the normal named-build image-push
     * result.
     *
     * @param name the deployment name
     * @param target the deployment provider
     * @return the deployment descriptor
     */
    public static QuarkusApplicationDeploymentDescriptor of(String name, QuarkusApplicationDeploymentTarget target) {
        return new QuarkusApplicationDeploymentDescriptor(name, target,
                QuarkusApplicationDeploymentImageSource.NORMAL_IMAGE_PUSH, Optional.empty());
    }
}
