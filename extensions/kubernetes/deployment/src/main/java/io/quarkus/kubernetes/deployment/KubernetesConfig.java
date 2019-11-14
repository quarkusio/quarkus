
package io.quarkus.kubernetes.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class KubernetesConfig {

    private static final String KUBERNETES = "kubernetes";
    private static final String OPENSHIFT = "openshift";
    private static final String KNATIVE = "knative";

    /**
     * The target deployment platform.
     * Defaults to kubernetes. Can be kubernetes, openshift, knative or any combination of the above as coma separated list.
     */
    @ConfigItem(defaultValue = KUBERNETES)
    String deploymentTarget = KUBERNETES;

    public String getDeploymentTarget() {
        return this.deploymentTarget;
    }
}
