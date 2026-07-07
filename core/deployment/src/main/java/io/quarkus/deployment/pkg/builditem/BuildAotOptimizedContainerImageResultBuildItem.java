package io.quarkus.deployment.pkg.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Reports a container image produced in response to a startup-optimized image request.
 * <p>
 * This is a multi build item because container-image providers contribute results independently. Consumers that
 * require an unambiguous image must enforce the number of acceptable results.
 */
public final class BuildAotOptimizedContainerImageResultBuildItem extends MultiBuildItem {

    private final String containerImage;

    /**
     * @param containerImage the reference of the produced container image
     */
    public BuildAotOptimizedContainerImageResultBuildItem(String containerImage) {
        this.containerImage = containerImage;
    }

    /**
     * @return the reference of the produced container image
     */
    public String getContainerImage() {
        return containerImage;
    }
}
