package io.quarkus.kubernetes.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

/**
 * The namespace the deployer reports and looks resources up in must be the one the generated manifest targets
 * (quarkus.kubernetes.namespace), falling back to the client's namespace only when the manifest has none.
 */
class KubernetesDeployerNamespaceTest {

    @Test
    void manifestNamespaceWins() {
        List<HasMetadata> items = List.of(
                new ServiceBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata().build(),
                new DeploymentBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata()
                        .build());
        assertEquals("manifest-ns",
                KubernetesDeployer.effectiveNamespace(items, DeploymentResourceKind.Deployment, "context-ns"));
    }

    @Test
    void clientNamespaceWhenTheManifestHasNone() {
        List<HasMetadata> items = List.of(
                new DeploymentBuilder().withNewMetadata().withName("app").endMetadata().build());
        assertEquals("context-ns",
                KubernetesDeployer.effectiveNamespace(items, DeploymentResourceKind.Deployment, "context-ns"));
    }

    @Test
    void defaultWhenNeitherHasOne() {
        List<HasMetadata> items = List.of(
                new DeploymentBuilder().withNewMetadata().withName("app").endMetadata().build());
        assertEquals("default", KubernetesDeployer.effectiveNamespace(items, DeploymentResourceKind.Deployment, null));
    }

    @Test
    void clientNamespaceWhenTheDeploymentResourceIsMissing() {
        List<HasMetadata> items = List.of(
                new ServiceBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata().build());
        assertEquals("context-ns",
                KubernetesDeployer.effectiveNamespace(items, DeploymentResourceKind.Deployment, "context-ns"));
    }
}
