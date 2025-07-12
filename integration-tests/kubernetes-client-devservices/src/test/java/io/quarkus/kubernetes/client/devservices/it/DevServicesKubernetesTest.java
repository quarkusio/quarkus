package io.quarkus.kubernetes.client.devservices.it;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.devservices.it.profiles.DevServiceKubernetes;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServiceKubernetes.class)
public class DevServicesKubernetesTest {

    @Inject
    KubernetesClient kubernetesClient;

    @Test
    @DisplayName("given kubernetes container must communicate with it and return its version")
    public void shouldReturnAllKeys() {
        Assertions.assertEquals("v" + DevServiceKubernetes.API_VERSION,
                kubernetesClient.getKubernetesVersion().getGitVersion());
    }

    @Test
    @DisplayName("specified manifest must be applied to the cluster by the dev service")
    public void manifestIsApplied() {
        Assertions.assertNotNull(kubernetesClient.namespaces().withName("example-namespace").get());
    }
}
