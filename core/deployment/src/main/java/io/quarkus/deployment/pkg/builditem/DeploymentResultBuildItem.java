
package io.quarkus.deployment.pkg.builditem;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item representing the result of a Kubernetes or OpenShift deployment process.
 * <p>
 * This build item encapsulates the name and labels of the main resource created or updated
 * during deployment. It is typically produced by deployment steps and can be consumed by
 * other build steps that need information about the deployed resource.
 * </p>
 *
 * <p>
 * The {@code name} usually refers to the primary resource (such as a Deployment, StatefulSet,
 * or DeploymentConfig) that was applied to the cluster. The {@code labels} map contains
 * metadata labels associated with this resource, which can be used for identification,
 * filtering, or further processing.
 * </p>
 */
public final class DeploymentResultBuildItem extends SimpleBuildItem {

    /**
     * The name of the main deployed resource (e.g., Deployment, StatefulSet, or DeploymentConfig).
     */
    private final String name;

    /**
     * The labels associated with the deployed resource.
     * <p>
     * These labels provide metadata that can be used for resource selection, grouping,
     * or integration with other tools and processes.
     * </p>
     */
    private final Map<String, String> labels;

    /**
     * Constructs a new {@link DeploymentResultBuildItem}.
     *
     * @param name the name of the main deployed resource
     * @param labels a map of labels associated with the deployed resource
     */
    public DeploymentResultBuildItem(String name, Map<String, String> labels) {
        this.name = name;
        this.labels = labels;
    }

    /**
     * Returns the name of the main deployed resource.
     *
     * @return the resource name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the labels associated with the deployed resource.
     *
     * @return a map of resource labels
     */
    public Map<String, String> getLabels() {
        return this.labels;
    }

}