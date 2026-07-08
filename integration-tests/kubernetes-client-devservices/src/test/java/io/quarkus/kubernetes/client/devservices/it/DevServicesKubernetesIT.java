package io.quarkus.kubernetes.client.devservices.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.test.utils.KubernetesClientDevServicesTestHelper;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Same checks as {@link DevServicesKubernetesTest}, but the tested artifact runs in a separate process,
 * so the {@link KubernetesClient} can't be obtained via CDI injection. Instead, it is built from the Dev
 * Services connection properties, exposed through the {@link KubernetesClientDevServicesTestHelper} field.
 */
@QuarkusIntegrationTest
public class DevServicesKubernetesIT {

    final KubernetesClientDevServicesTestHelper k8s = new KubernetesClientDevServicesTestHelper();

    @Test
    public void resourceManifestIsApplied() {
        Assertions.assertNotNull(k8s.getClient().namespaces().withName("example-namespace").get());
    }

    @Test
    public void urlManifestIsApplied() {
        // Applied by https://k8s.io/examples/admin/namespace-dev.yaml
        Assertions.assertNotNull(k8s.getClient().namespaces().withName("development").get());
    }
}
