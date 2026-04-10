
package io.quarkus.kubernetes.client.runtime;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.runtime.internal.KubernetesClientUtils;

class KubernetesClientUtilsTest {

    @BeforeEach
    public void setUp() {
        System.getProperties().remove(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY);
        System.getProperties().remove(Config.KUBERNETES_DISABLE_HOSTNAME_VERIFICATION_SYSTEM_PROPERTY);
        System.getProperties().remove(Config.KUBERNETES_KUBECONFIG_FILE);
    }

    @Test
    void shouldGetConfigWithTrustCerts() throws Exception {
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE,
                new File(getClass().getResource("/test-kubeconfig").toURI()).getAbsolutePath());
        Config config = Config.autoConfigure(null);
        assertTrue(config.isTrustCerts());
    }

    @Test
    void shouldGetClientWithTrustCerts() throws Exception {
        io.smallrye.config.Config config = io.smallrye.config.Config.getOrCreate();
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE,
                new File(getClass().getResource("/test-kubeconfig").toURI()).getAbsolutePath());
        try (KubernetesClient client = KubernetesClientUtils.createClient()) {
            assertTrue(client.getConfiguration().isTrustCerts());
        }
        ConfigProviderResolver.instance().releaseConfig(config);
    }
}
