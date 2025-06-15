package io.quarkus.openshift.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.QuarkusUnitTest;

public class BeanOverridesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.kubernetes-client.devservices.enabled", "false");

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    OpenShiftClient openShiftClient;

    @Test
    public void openShiftClientCanBeOverridden() {
        assertEquals("https://example.com/overridden/", openShiftClient.getConfiguration().getMasterUrl());
    }

    @Test
    public void kubernetesClientCanIsOpenShiftInstance() {
        assertEquals("https://example.com/overridden/", kubernetesClient.getConfiguration().getMasterUrl());
    }

    @Singleton
    public static class BeanOverridesConfig {
        @Produces
        public OpenShiftClient openShiftClient() {
            return new KubernetesClientBuilder()
                    .withConfig(
                            new ConfigBuilder(Config.empty()).withMasterUrl("https://example.com/overridden/").build())
                    .build().adapt(OpenShiftClient.class);
        }
    }
}
