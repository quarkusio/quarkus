package io.quarkus.kubernetes.client.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.KubernetesConfigCustomizer;
import io.quarkus.test.QuarkusUnitTest;

public class KubernetesClientCDITest {

    @Inject
    KubernetesClient client;

    @Test
    public void test() {
        assertEquals("-1", client.getConfiguration().getApiVersion());
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().withApplicationRoot((jar) -> jar.addClasses(Customizer.class))
            .overrideConfigKey("quarkus.kubernetes-client.devservices.enabled", "false");

    @Singleton
    public static class Customizer implements KubernetesConfigCustomizer {
        @Override
        public void customize(Config config) {
            config.setApiVersion("-1");
        }
    }

}
