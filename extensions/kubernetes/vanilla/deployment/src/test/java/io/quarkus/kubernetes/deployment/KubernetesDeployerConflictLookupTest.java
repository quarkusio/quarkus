package io.quarkus.kubernetes.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.quarkus.kubernetes.spi.DeployStrategy;

/**
 * The conflict check must look for resources of the other deployment kinds in the namespace the manifest targets,
 * not in the namespace of the client's context.
 */
@EnableKubernetesMockClient(crud = true)
class KubernetesDeployerConflictLookupTest {

    KubernetesClient client;

    private static final DeploymentTargetEntry TARGET = new DeploymentTargetEntry(Constants.KUBERNETES,
            DeploymentResourceKind.Deployment, 0, DeployStrategy.CreateOrUpdate);

    private static final List<HasMetadata> GENERATED = List.of(
            new DeploymentBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata().build());

    @Test
    void findsTheConflictInTheManifestNamespace() {
        client.apps().statefulSets().inNamespace("manifest-ns").resource(
                new StatefulSetBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata()
                        .build())
                .create();

        Optional<GenericKubernetesResource> conflict = KubernetesDeployer.findConflictingResource(client, "manifest-ns",
                TARGET, GENERATED);

        assertThat(conflict).isPresent();
        assertThat(conflict.get().getKind()).isEqualTo("StatefulSet");
        assertThat(conflict.get().getMetadata().getNamespace()).isEqualTo("manifest-ns");
    }

    @Test
    void ignoresTheSameNameInAnotherNamespace() {
        client.apps().statefulSets().inNamespace("context-ns").resource(
                new StatefulSetBuilder().withNewMetadata().withName("app").withNamespace("context-ns").endMetadata()
                        .build())
                .create();

        assertThat(KubernetesDeployer.findConflictingResource(client, "manifest-ns", TARGET, GENERATED)).isEmpty();
    }
}
