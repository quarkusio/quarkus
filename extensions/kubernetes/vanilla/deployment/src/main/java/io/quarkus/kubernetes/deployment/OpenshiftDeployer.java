
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.OPENSHIFT;

import java.util.List;
import java.util.Optional;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentClusterBuildItem;

public class OpenshiftDeployer {

    @BuildStep
    public void checkEnvironment(Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            List<GeneratedKubernetesResourceBuildItem> resources,
            KubernetesClientBuildItem client, BuildProducer<KubernetesDeploymentClusterBuildItem> deploymentCluster) {
        selectedDeploymentTarget.ifPresent(target -> {
            if (!KubernetesDeploy.INSTANCE.checkSilently()) {
                return;
            }
            if (target.getEntry().getName().equals(OPENSHIFT)) {
                if (client.getClient().isAdaptable(OpenShiftClient.class)) {
                    deploymentCluster.produce(new KubernetesDeploymentClusterBuildItem(OPENSHIFT));
                } else {
                    throw new IllegalStateException(
                            "Openshift was requested as a deployment, but the target cluster is not an Openshift cluster!");
                }
            }
        });
    }
}
