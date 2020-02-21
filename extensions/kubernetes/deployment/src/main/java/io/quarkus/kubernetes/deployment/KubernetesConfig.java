
package io.quarkus.kubernetes.deployment;

import java.util.List;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class KubernetesConfig {

    /**
     * The target deployment platform.
     * Defaults to kubernetes. Can be kubernetes, openshift, knative or any combination of the above as comma separated list.
     */
    @ConfigItem(defaultValue = "kubernetes")
    List<DeploymentTarget> deploymentTarget;

    public List<DeploymentTarget> getDeploymentTarget() {
        return this.deploymentTarget;
    }
}
