
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
            KubernetesClientBuildItem kubernetesClientBuilder,
            BuildProducer<KubernetesDeploymentClusterBuildItem> deploymentCluster) {
        selectedDeploymentTarget.ifPresent(target -> {
            if (!KubernetesDeploy.INSTANCE.checkSilently(kubernetesClientBuilder)) {
                return;
            }
            if (target.getEntry().getName().equals(OPENSHIFT)) {
                try (var openShiftClient = kubernetesClientBuilder.buildClient().adapt(OpenShiftClient.class)) {
                    if (openShiftClient.hasApiGroup("openshift.io", false)) {
                        deploymentCluster.produce(new KubernetesDeploymentClusterBuildItem(OPENSHIFT));
                    } else {
                        throw new IllegalStateException(
                                "Openshift was requested as a deployment, but the target cluster is not an Openshift cluster!");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Failed to configure OpenShift. Make sure you have the Quarkus OpenShift extension.", e);
                }
            }
        });
    }
}
