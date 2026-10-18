package io.quarkus.it.openshift.client;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServer;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@WithKubernetesTestServer
@QuarkusTest
public class KubernetesTestServerTest {

    @KubernetesTestServer
    KubernetesServer mockServer;
    @Inject
    KubernetesClient kubernetesClient;
    @Inject
    OpenShiftClient openShiftClient;

    @Test
    public void clientsInjectedWithValidConfiguration() {
        assertThat(kubernetesClient)
                .isSameAs(openShiftClient)
                .extracting(c -> c.getConfiguration().getMasterUrl())
                .isEqualTo(mockServer.getKubernetesMockServer().url("/"));
    }

    @Test
    public void openShiftClientInjectionWorks() throws InterruptedException {
        openShiftClient.routes().resource(
                new RouteBuilder()
                        .withNewMetadata().withName("the-route").endMetadata()
                        .withNewSpec().withHost("example.com").endSpec()
                        .build())
                .createOr(NonDeletingOperation::update);
        assertThat(mockServer.getLastRequest().getPath())
                .isEqualTo("/apis/route.openshift.io/v1/namespaces/test/routes");
    }
}
