
package io.quarkus.kubernetes.deployment;

import java.util.function.BooleanSupplier;

import io.quarkus.container.deployment.ContainerConfig;
import io.quarkus.container.deployment.DockerBuild.OutputFilter;
import io.quarkus.deployment.util.ExecUtil;

public class KubernetesDeploy implements BooleanSupplier {

    private KubernetesConfig kubernetesConfig;
    private ContainerConfig containerConfig;

    KubernetesDeploy(ContainerConfig containerConfig, KubernetesConfig kubernetesConfig) {
        this.containerConfig = containerConfig;
        this.kubernetesConfig = kubernetesConfig;
    }

    @Override
    public boolean getAsBoolean() {
        if (containerConfig.deploy) {
            try {
                if (kubernetesConfig.getDeploymentTarget().contains(DeploymentTarget.OPENSHIFT)) {
                    return ExecUtil.exec("oc", "version");
                }
                return ExecUtil.exec("kubectl", "version");
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
