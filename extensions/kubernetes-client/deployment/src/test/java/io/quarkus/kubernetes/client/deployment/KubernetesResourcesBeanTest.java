package io.quarkus.kubernetes.client.deployment;

import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.kubernetes.client.KubernetesResources;
import io.quarkus.test.QuarkusExtensionTest;

public class KubernetesResourcesBeanTest {

    @Inject
    @KubernetesResources
    Class[] resourceClasses;

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.kubernetes-client.devservices.enabled", "false");

    @Test
    public void test() {
        assertTrue(resourceClasses.length > 0, "No Kubernetes resource classes were found");
        for (Class<?> resourceClass : resourceClasses) {
            assertTrue(KubernetesResource.class.isAssignableFrom(resourceClass), resourceClass.getName());
        }
    }
}
