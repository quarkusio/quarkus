package io.quarkus.openshift.client.deployment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.QuarkusUnitTest;

public class BeanDefaultsTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.kubernetes-client.devservices.enabled", "false");

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    OpenShiftClient openShiftClient;

    @Test
    public void openShiftClientIsInstantiated() {
        assertNotNull(openShiftClient);
    }

    @Test
    public void kubernetesClientIsInstantiatedAsOpenShiftClient() {
        assertNotNull(kubernetesClient);
        assertTrue(kubernetesClient instanceof OpenShiftClient);
    }
}
