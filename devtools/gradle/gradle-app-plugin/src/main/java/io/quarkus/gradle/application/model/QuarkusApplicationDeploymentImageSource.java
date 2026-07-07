package io.quarkus.gradle.application.model;

/**
 * Selects the image reference consumed by a deployment task.
 */
public enum QuarkusApplicationDeploymentImageSource {

    /**
     * Deploy an explicitly supplied image reference without building or
     * pushing it.
     */
    EXISTING_IMAGE,
    /**
     * Depend on and deploy the normal image-push result of the named build.
     */
    NORMAL_IMAGE_PUSH,
    /**
     * Depend on and deploy the startup-optimized image-push result.
     */
    STARTUP_OPTIMIZED_IMAGE_PUSH
}
