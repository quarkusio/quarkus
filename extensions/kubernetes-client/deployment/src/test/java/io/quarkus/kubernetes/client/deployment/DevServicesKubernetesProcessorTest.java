package io.quarkus.kubernetes.client.deployment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dajudge.kindcontainer.client.config.Cluster;
import com.dajudge.kindcontainer.client.config.ClusterSpec;
import com.dajudge.kindcontainer.client.config.Context;
import com.dajudge.kindcontainer.client.config.ContextSpec;
import com.dajudge.kindcontainer.client.config.KubeConfig;
import com.dajudge.kindcontainer.client.config.User;
import com.dajudge.kindcontainer.client.config.UserSpec;

/**
 * Unit tests for DevServicesKubernetesProcessor
 */
class DevServicesKubernetesProcessorTest {

    private KubeConfig createTestKubeConfig() {
        ClusterSpec clusterSpec = new ClusterSpec();
        clusterSpec.setServer("https://localhost:6443");
        clusterSpec.setCertificateAuthorityData("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUNwakNDQVk0Q0NRRGRKc...");

        Cluster cluster = new Cluster();
        cluster.setName("test-cluster");
        cluster.setCluster(clusterSpec);

        UserSpec userSpec = new UserSpec();
        userSpec.setClientCertificateData("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUM3akNDQWRhZ0F3SUJBZ0...");
        userSpec.setClientKeyData("LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFb3dJQkFBS0NBUUVBMz...");

        User user = new User();
        user.setName("test-user");
        user.setUser(userSpec);

        ContextSpec contextSpec = new ContextSpec();
        contextSpec.setCluster("test-cluster");
        contextSpec.setUser("test-user");

        Context context = new Context();
        context.setName("test-context");
        context.setContext(contextSpec);

        KubeConfig kubeConfig = new KubeConfig();
        kubeConfig.setApiVersion("v1");
        kubeConfig.setKind("Config");
        kubeConfig.setClusters(List.of(cluster));
        kubeConfig.setUsers(List.of(user));
        kubeConfig.setContexts(List.of(context));
        kubeConfig.setCurrentContext("test-context");

        return kubeConfig;
    }

    @Test
    @DisplayName("should include all expected Kubernetes client configuration properties")
    void shouldIncludeKubernetesClientConfigProperties() {
        DevServicesKubernetesProcessor processor = new DevServicesKubernetesProcessor();
        KubeConfig kubeConfig = createTestKubeConfig();

        Map<String, String> config = invokeGetKubernetesClientConfigFromKubeConfig(processor, kubeConfig);

        assertNotNull(config, "Configuration map should not be null");
        assertTrue(config.containsKey("quarkus.kubernetes-client.api-server-url"),
                "Should contain API server URL");
        assertTrue(config.containsKey("quarkus.kubernetes-client.ca-cert-data"),
                "Should contain CA certificate data");
        assertTrue(config.containsKey("quarkus.kubernetes-client.client-cert-data"),
                "Should contain client certificate data");
        assertTrue(config.containsKey("quarkus.kubernetes-client.client-key-data"),
                "Should contain client key data");
        assertTrue(config.containsKey("quarkus.kubernetes-client.client-key-algo"),
                "Should contain client key algorithm");
        assertTrue(config.containsKey("quarkus.kubernetes-client.namespace"),
                "Should contain namespace");

        assertEquals("https://localhost:6443", config.get("quarkus.kubernetes-client.api-server-url"),
                "API server URL should match");
        assertEquals("default", config.get("quarkus.kubernetes-client.namespace"),
                "Namespace should be default");
    }

    @Test
    @DisplayName("should include kubeconfig with metadata prefix for Dev UI download")
    void shouldIncludeKubeconfigWithMetadataPrefix() {
        DevServicesKubernetesProcessor processor = new DevServicesKubernetesProcessor();
        KubeConfig kubeConfig = createTestKubeConfig();

        Map<String, String> config = invokeGetKubernetesClientConfigFromKubeConfig(processor, kubeConfig);

        String expectedKey = DevServicesKubernetesProcessor.KUBECONFIG_DOWNLOAD_KEY;
        assertTrue(config.containsKey(expectedKey),
                "Should contain kubeconfig with metadata prefix: " + expectedKey);

        String kubeconfigYaml = config.get(expectedKey);
        assertNotNull(kubeconfigYaml, "Kubeconfig YAML should not be null");
        assertFalse(kubeconfigYaml.isEmpty(), "Kubeconfig YAML should not be empty");
        assertTrue(kubeconfigYaml.contains("apiVersion"), "Kubeconfig should contain apiVersion");
        assertTrue(kubeconfigYaml.contains("kind"), "Kubeconfig should contain kind");
        assertTrue(kubeconfigYaml.contains("clusters"), "Kubeconfig should contain clusters");
    }

    @Test
    @DisplayName("should use correct metadata prefix constant")
    void shouldUseCorrectMetadataPrefix() {
        assertEquals("quarkus.devservices.internal.",
                DevServicesKubernetesProcessor.DEVSERVICES_METADATA_PREFIX,
                "Metadata prefix should match documented value");

        assertEquals("quarkus.devservices.internal.kubeconfig.yaml",
                DevServicesKubernetesProcessor.KUBECONFIG_DOWNLOAD_KEY,
                "Kubeconfig key should match documented value");
    }

    @Test
    @DisplayName("returned configuration map should be immutable")
    void shouldReturnImmutableMap() {
        DevServicesKubernetesProcessor processor = new DevServicesKubernetesProcessor();
        KubeConfig kubeConfig = createTestKubeConfig();

        Map<String, String> config = invokeGetKubernetesClientConfigFromKubeConfig(processor, kubeConfig);

        assertThrows(UnsupportedOperationException.class,
                () -> config.put("malicious.key", "hacker-value"),
                "Should not allow modification of returned map");

        assertThrows(UnsupportedOperationException.class,
                () -> config.remove("quarkus.kubernetes-client.api-server-url"),
                "Should not allow removal from returned map");

        assertThrows(UnsupportedOperationException.class,
                () -> config.clear(),
                "Should not allow clearing returned map");
    }

    @Test
    @DisplayName("kubeconfig key should start with metadata prefix")
    void kubeconfigKeyShouldStartWithMetadataPrefix() {
        String prefix = DevServicesKubernetesProcessor.DEVSERVICES_METADATA_PREFIX;
        String key = DevServicesKubernetesProcessor.KUBECONFIG_DOWNLOAD_KEY;

        assertTrue(key.startsWith(prefix),
                String.format("Kubeconfig key '%s' should start with metadata prefix '%s'", key, prefix));
    }

    @Test
    @DisplayName("regular config properties should not start with metadata prefix")
    void regularConfigPropertiesShouldNotStartWithMetadataPrefix() {
        DevServicesKubernetesProcessor processor = new DevServicesKubernetesProcessor();
        KubeConfig kubeConfig = createTestKubeConfig();

        Map<String, String> config = invokeGetKubernetesClientConfigFromKubeConfig(processor, kubeConfig);

        String metadataPrefix = DevServicesKubernetesProcessor.DEVSERVICES_METADATA_PREFIX;

        long metadataKeyCount = config.keySet().stream()
                .filter(key -> key.startsWith(metadataPrefix))
                .count();

        assertEquals(1, metadataKeyCount,
                "Only kubeconfig should have metadata prefix, found " + metadataKeyCount + " keys with prefix");

        assertFalse(config.get("quarkus.kubernetes-client.api-server-url").startsWith(metadataPrefix),
                "Regular properties should not start with metadata prefix");
    }

    @Test
    @DisplayName("configuration map should contain expected number of entries")
    void shouldContainExpectedNumberOfEntries() {
        DevServicesKubernetesProcessor processor = new DevServicesKubernetesProcessor();
        KubeConfig kubeConfig = createTestKubeConfig();

        Map<String, String> config = invokeGetKubernetesClientConfigFromKubeConfig(processor, kubeConfig);

        assertEquals(7, config.size(),
                "Should have 7 entries (6 config properties + 1 kubeconfig)");
    }

    /**
     * Helper method to invoke the private getKubernetesClientConfigFromKubeConfig method
     * using reflection for testing purposes.
     */
    private Map<String, String> invokeGetKubernetesClientConfigFromKubeConfig(
            DevServicesKubernetesProcessor processor, KubeConfig kubeConfig) {
        try {
            var method = DevServicesKubernetesProcessor.class
                    .getDeclaredMethod("getKubernetesClientConfigFromKubeConfig", KubeConfig.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) method.invoke(processor, kubeConfig);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getKubernetesClientConfigFromKubeConfig", e);
        }
    }
}
