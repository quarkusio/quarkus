package io.quarkus.openshift.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.QuarkusExtensionTest;

public class OpenShiftClientConfigTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.kubernetes-client.devservices.enabled", "false");

    @Inject
    OpenShiftClient openShiftClient;

    @Test
    public void openShiftClientUsesTheKubernetesConfig() {
        assertEquals("https://example.com/config/", openShiftClient.getConfiguration().getMasterUrl());
        assertTrue(openShiftClient.getConfiguration().isHttp2Disable());
    }

    @Singleton
    public static class ConfigProducer {
        @Produces
        public Config config() {
            return new ConfigBuilder(Config.empty())
                    .withMasterUrl("https://example.com/config/")
                    .withHttp2Disable(true)
                    .build();
        }
    }
}
