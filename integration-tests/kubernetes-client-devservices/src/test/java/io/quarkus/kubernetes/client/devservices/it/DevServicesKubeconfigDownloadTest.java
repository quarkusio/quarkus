package io.quarkus.kubernetes.client.devservices.it;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.devservices.it.profiles.DevServiceKubernetes;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DevServiceKubernetes.class)
public class DevServicesKubeconfigDownloadTest {

    private static final String DEVSERVICES_METADATA_PREFIX = "quarkus.devservices.internal.";
    private static final String KUBECONFIG_DOWNLOAD_KEY = DEVSERVICES_METADATA_PREFIX + "kubeconfig.yaml";

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    Config config;

    @Test
    @DisplayName("Kubernetes dev service should be running and accessible")
    public void kubernetesDevServiceShouldBeRunning() {
        assertNotNull(kubernetesClient, "Kubernetes client should be injected");

        assertNotNull(kubernetesClient.getKubernetesVersion(),
                "Should be able to get Kubernetes version from dev service");

        assertEquals("v" + DevServiceKubernetes.API_VERSION,
                kubernetesClient.getKubernetesVersion().getGitVersion(),
                "Kubernetes version should match configured dev service version");
    }

    @Test
    @DisplayName("Configuration should contain Kubernetes client properties from dev service")
    public void configurationShouldContainKubernetesProperties() {
        assertTrue(config.getOptionalValue("quarkus.kubernetes-client.api-server-url", String.class).isPresent(),
                "Should have api-server-url configured by dev service");

        assertTrue(config.getOptionalValue("quarkus.kubernetes-client.ca-cert-data", String.class).isPresent(),
                "Should have CA cert data configured by dev service");

        assertTrue(config.getOptionalValue("quarkus.kubernetes-client.client-cert-data", String.class).isPresent(),
                "Should have client cert data configured by dev service");

        assertTrue(config.getOptionalValue("quarkus.kubernetes-client.client-key-data", String.class).isPresent(),
                "Should have client key data configured by dev service");

        assertTrue(config.getOptionalValue("quarkus.kubernetes-client.namespace", String.class).isPresent(),
                "Should have namespace configured by dev service");
    }

    @Test
    @DisplayName("Metadata prefix constant should match expected value")
    public void metadataPrefixShouldMatchExpectedValue() {
        assertEquals("quarkus.devservices.internal.", DEVSERVICES_METADATA_PREFIX,
                "Metadata prefix should be 'quarkus.devservices.internal.'");

        assertEquals("quarkus.devservices.internal.kubeconfig.yaml", KUBECONFIG_DOWNLOAD_KEY,
                "Kubeconfig key should be 'quarkus.devservices.internal.kubeconfig.yaml'");
    }

    @Test
    @DisplayName("API server URL should point to localhost for dev service")
    public void apiServerUrlShouldPointToLocalhost() {
        String apiServerUrl = config.getValue("quarkus.kubernetes-client.api-server-url", String.class);

        assertNotNull(apiServerUrl, "API server URL should be configured");
        assertTrue(apiServerUrl.contains("localhost") || apiServerUrl.contains("127.0.0.1"),
                "API server URL should point to localhost for dev service");
        assertTrue(apiServerUrl.startsWith("https://"),
                "API server URL should use HTTPS");
    }

    @Test
    @DisplayName("Client certificates should be configured for authentication")
    public void clientCertificatesShouldBeConfigured() {
        String caCertData = config.getValue("quarkus.kubernetes-client.ca-cert-data", String.class);
        String clientCertData = config.getValue("quarkus.kubernetes-client.client-cert-data", String.class);
        String clientKeyData = config.getValue("quarkus.kubernetes-client.client-key-data", String.class);

        assertNotNull(caCertData, "CA certificate data should be configured");
        assertNotNull(clientCertData, "Client certificate data should be configured");
        assertNotNull(clientKeyData, "Client key data should be configured");

        assertFalse(caCertData.isEmpty(), "CA certificate data should not be empty");
        assertFalse(clientCertData.isEmpty(), "Client certificate data should not be empty");
        assertFalse(clientKeyData.isEmpty(), "Client key data should not be empty");

        // Base64 encoded certificate data should start with specific patterns
        assertTrue(caCertData.startsWith("LS0tLS1CRUdJTi"),
                "CA cert should be Base64 encoded PEM certificate");
        assertTrue(clientCertData.startsWith("LS0tLS1CRUdJTi"),
                "Client cert should be Base64 encoded PEM certificate");
        assertTrue(clientKeyData.startsWith("LS0tLS1CRUdJTi"),
                "Client key should be Base64 encoded PEM key");
    }
}
