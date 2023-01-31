package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.KNATIVE;

import java.util.List;
import java.util.Optional;

import io.fabric8.knative.client.DefaultKnativeClient;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;
import io.quarkus.kubernetes.spi.GeneratedKubernetesResourceBuildItem;
import io.quarkus.kubernetes.spi.KubernetesDeploymentClusterBuildItem;

public class KnativeDeployer {

    @BuildStep
    public void checkEnvironment(Optional<SelectedKubernetesDeploymentTargetBuildItem> selectedDeploymentTarget,
            List<GeneratedKubernetesResourceBuildItem> resources,
            KubernetesClientBuildItem clientSupplier, BuildProducer<KubernetesDeploymentClusterBuildItem> deploymentCluster) {
        selectedDeploymentTarget.ifPresent(target -> {
            if (!KubernetesDeploy.INSTANCE.checkSilently()) {
                return;
            }
            if (target.getEntry().getName().equals(KNATIVE)) {
                try (DefaultKnativeClient client = clientSupplier.getClient().get().adapt(DefaultKnativeClient.class)) {
                    if (client.isSupported()) {
                        deploymentCluster.produce(new KubernetesDeploymentClusterBuildItem(KNATIVE));
                    } else {
                        throw new IllegalStateException(
                                "Knative was requested as a deployment, but the target cluster is not a Knative cluster!");
                    }
                }
            }
        });
    }
}
