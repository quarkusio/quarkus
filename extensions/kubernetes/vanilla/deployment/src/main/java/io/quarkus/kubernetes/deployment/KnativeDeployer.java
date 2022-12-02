package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;

import java.util.List;
import java.util.Optional;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentClusterBuildItem;

public class KnativeDeployer {

    @BuildStep
    public void checkEnvironment(Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            List<GeneratedKubernetesResourceBuildItem> resources,
            KubernetesClientBuildItem client, BuildProducer<KubernetesDeploymentClusterBuildItem> deploymentCluster) {
        selectedDeploymentTarget.ifPresent(target -> {
            if (!KubernetesDeploy.INSTANCE.checkSilently()) {
                return;
            }
            if (target.getEntry().getName().equals(KNATIVE)) {
                // use 'isSupported' once https://github.com/fabric8io/kubernetes-client/issues/4447 is resolved
                if (client.getClient().hasApiGroup("knative.dev", false)) {
                    deploymentCluster.produce(new KubernetesDeploymentClusterBuildItem(KNATIVE));
                } else {
                    throw new IllegalStateException(
                            "Knative was requested as a deployment, but the target cluster is not a Knative cluster!");
                }
            }
        });
    }
}
