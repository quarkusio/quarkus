package io.quarkus.kubernetes.config.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class KubernetesConfigSourceProviderTest {

    @Test
    public void testEmptyConfigSources() {
        KubernetesConfigSourceConfig config = defaultConfig();
        config.enabled = true;
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        KubernetesConfigBuildTimeConfig buildTimeConfig = new KubernetesConfigBuildTimeConfig();
        buildTimeConfig.secretsEnabled = true;
        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, buildTimeConfig, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isEmpty();
    }

    @Test
    public void testRetrieveNamespacedConfigSources() {
        KubernetesConfigSourceConfig config = defaultConfig();
        config.enabled = true;
        config.namespace = Optional.of("demo");
        List<String> configMaps = Lists.list("cm1");
        config.configMaps = Optional.of(configMaps);

        KubernetesConfigBuildTimeConfig buildTimeConfig = new KubernetesConfigBuildTimeConfig();

        ConfigMap configMap = configMapBuilder("cm1")
                .addToData("some.key", "someValue").addToData("some.other", "someOtherValue").build();

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubNamespacedConfigMap(kubernetesClient, configMap, "cm1");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, buildTimeConfig, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isNotEmpty();
        ConfigSource next = configSources.iterator().next();
        assertThat(next.getProperties()).containsKeys("some.key", "some.other");
    }

    @Test
    public void testNamespacedConfigSourcesAbsents() {
        KubernetesConfigSourceConfig config = defaultConfig();
        config.enabled = true;
        config.namespace = Optional.of("demo");
        List<String> configMaps = Lists.list("cm2");
        config.configMaps = Optional.of(configMaps);

        KubernetesConfigBuildTimeConfig buildTimeConfig = new KubernetesConfigBuildTimeConfig();

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubNamespacedConfigMap(kubernetesClient, null, "cm2");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, buildTimeConfig, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isEmpty();
    }

    @Test
    public void testRetrieveConfigSources() {
        KubernetesConfigSourceConfig config = defaultConfig();
        config.enabled = true;
        List<String> configMaps = Lists.list("cm1");
        config.configMaps = Optional.of(configMaps);

        KubernetesConfigBuildTimeConfig buildTimeConfig = new KubernetesConfigBuildTimeConfig();

        ConfigMap configMap = configMapBuilder("cm1")
                .addToData("some.key", "someValue").addToData("some.other", "someOtherValue").build();

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubConfigMap(kubernetesClient, configMap, "cm1");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, buildTimeConfig, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isNotEmpty();
        ConfigSource next = configSources.iterator().next();
        assertThat(next.getProperties()).containsKeys("some.key", "some.other");
    }

    @Test
    public void testRetrieveSecretsWithoutConfigEnable() {
        KubernetesConfigSourceConfig config = defaultConfig();
        KubernetesConfigBuildTimeConfig buildTimeConfig = new KubernetesConfigBuildTimeConfig();
        buildTimeConfig.secretsEnabled = true;

        Secret secret = secretMapBuilder("testOnlySingleMatchingPropertiesData")
                .addToData("application.properties", encodeValue("key1=value1\nsome.key=someValue")).build();
        List<String> secrets = Lists.list("cm1");
        config.secrets = Optional.of(secrets);

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubSecrets(kubernetesClient, secret, "cm1");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, buildTimeConfig, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isNotEmpty();
        ConfigSource next = configSources.iterator().next();
        assertThat(next.getProperties()).containsKeys("key1", "some.key");
    }

    @Test
    public void testConfigSourcesAbsent() {
        KubernetesConfigSourceConfig config = defaultConfig();
        config.enabled = true;
        List<String> configMaps = Lists.list("cm2");
        config.configMaps = Optional.of(configMaps);

        KubernetesConfigBuildTimeConfig buildTimeConfig = new KubernetesConfigBuildTimeConfig();

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubConfigMap(kubernetesClient, null, "cm2");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, buildTimeConfig, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isEmpty();
    }

    @Test
    public void testConfigSourcesAbsentFailOnMissing() {
        try {
            KubernetesConfigSourceConfig config = defaultConfig();
            config.enabled = true;
            config.namespace = Optional.of("demo");
            List<String> configMaps = Lists.list("cm2");
            config.configMaps = Optional.of(configMaps);
            config.failOnMissingConfig = true;

            KubernetesConfigBuildTimeConfig buildTimeConfig = new KubernetesConfigBuildTimeConfig();

            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            stubNamespacedConfigMap(kubernetesClient, null, "cm2");

            KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, buildTimeConfig, kubernetesClient);
            Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
            fail("an exception should be raised");
        } catch (RuntimeException expected) {
        }
    }

    private void stubNamespacedConfigMap(KubernetesClient kubernetesClient, ConfigMap configMap, String configMapName) {
        MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> mixedOperation = (MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>>) mock(
                MixedOperation.class);
        when(kubernetesClient.configMaps()).thenReturn(mixedOperation);
        Resource<ConfigMap> resource = (Resource<ConfigMap>) mock(Resource.class);
        NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> nsClient = (NonNamespaceOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>>) mock(
                NonNamespaceOperation.class);
        when(mixedOperation.inNamespace("demo")).thenReturn(nsClient);
        when(nsClient.withName(configMapName)).thenReturn(resource);

        when(resource.get()).thenReturn(configMap);
        Config kubernetesConfig = mock(Config.class);
        when(kubernetesClient.getConfiguration()).thenReturn(kubernetesConfig);
        when(kubernetesConfig.getMasterUrl()).thenReturn("url");

    }

    private void stubConfigMap(KubernetesClient kubernetesClient, ConfigMap configMap, String configMapName) {
        MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> mixedOperation = (MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>>) mock(
                MixedOperation.class);
        when(kubernetesClient.configMaps()).thenReturn(mixedOperation);
        Resource<ConfigMap> resource = (Resource<ConfigMap>) mock(Resource.class);
        when(mixedOperation.withName(configMapName)).thenReturn(resource);
        when(resource.get()).thenReturn(configMap);
        Config kubernetesConfig = mock(Config.class);
        when(kubernetesClient.getConfiguration()).thenReturn(kubernetesConfig);
        when(kubernetesConfig.getMasterUrl()).thenReturn("url");

    }

    private void stubSecrets(KubernetesClient kubernetesClient, Secret secret, String secretName) {
        MixedOperation<Secret, SecretList, Resource<Secret>> mixedOperation = (MixedOperation<Secret, SecretList, Resource<Secret>>) mock(
                MixedOperation.class);
        when(kubernetesClient.secrets()).thenReturn(mixedOperation);
        Resource<Secret> resource = (Resource<Secret>) mock(Resource.class);
        when(mixedOperation.withName(secretName)).thenReturn(resource);
        when(resource.get()).thenReturn(secret);
        Config kubernetesConfig = mock(Config.class);
        when(kubernetesClient.getConfiguration()).thenReturn(kubernetesConfig);
        when(kubernetesConfig.getMasterUrl()).thenReturn("url");

    }

    private SecretBuilder secretMapBuilder(String name) {
        return new SecretBuilder().withNewMetadata()
                .withName(name).endMetadata();
    }

    private String encodeValue(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    private KubernetesConfigSourceConfig defaultConfig() {
        KubernetesConfigSourceConfig config = new KubernetesConfigSourceConfig();
        config.namespace = Optional.empty();
        config.configMaps = Optional.empty();
        config.secrets = Optional.empty();
        return config;
    }

    private ConfigMapBuilder configMapBuilder(String name) {
        return new ConfigMapBuilder().withNewMetadata()
                .withName(name).endMetadata();
    }

}
