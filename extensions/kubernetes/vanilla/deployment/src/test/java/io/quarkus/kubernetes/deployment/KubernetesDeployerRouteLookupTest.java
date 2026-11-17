package io.quarkus.kubernetes.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;

/**
 * The route the deployer reports after an OpenShift deploy must be read from the namespace the manifest targets, and
 * a route the cluster does not return must not fail the build.
 */
@EnableKubernetesMockClient(crud = true)
class KubernetesDeployerRouteLookupTest {

    KubernetesClient client;

    private static final KubernetesList GENERATED = new KubernetesListBuilder()
            .addToItems(new RouteBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata()
                    .build())
            .build();

    @Test
    void readsTheRouteFromTheManifestNamespace() {
        client.resources(Route.class).inNamespace("manifest-ns").resource(
                new RouteBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata()
                        .withNewSpec().withHost("app.manifest-ns.example.com").withNewTls().withTermination("edge")
                        .endTls().endSpec().build())
                .create();

        assertThat(KubernetesDeployer.printExposeInformation(client, "manifest-ns", GENERATED, "app"))
                .contains("https://app.manifest-ns.example.com");
    }

    @Test
    void ignoresTheRouteOfAnotherNamespace() {
        client.resources(Route.class).inNamespace("context-ns").resource(
                new RouteBuilder().withNewMetadata().withName("app").withNamespace("context-ns").endMetadata()
                        .withNewSpec().withHost("app.context-ns.example.com").endSpec().build())
                .create();

        assertThat(KubernetesDeployer.printExposeInformation(client, "manifest-ns", GENERATED, "app")).isEmpty();
    }

    @Test
    void missingRouteDoesNotFail() {
        assertThat(KubernetesDeployer.printExposeInformation(client, "manifest-ns", GENERATED, "app")).isEmpty();
    }

    @Test
    void routeWithoutSpecDoesNotFail() {
        client.resources(Route.class).inNamespace("manifest-ns").resource(
                new RouteBuilder().withNewMetadata().withName("app").withNamespace("manifest-ns").endMetadata().build())
                .create();

        assertThat(KubernetesDeployer.printExposeInformation(client, "manifest-ns", GENERATED, "app")).isEmpty();
    }
}
