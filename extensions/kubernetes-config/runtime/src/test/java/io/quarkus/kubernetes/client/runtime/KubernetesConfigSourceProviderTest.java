package io.quarkus.kubernetes.client.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

public class KubernetesConfigSourceProviderTest {

    @Test
    public void testEmptyConfigSources() {
        KubernetesConfigSourceConfig config = defaultConfig();
        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isEmpty();
    }

    @Test
    public void testRetrieveNamespacedConfigSources() {
        KubernetesConfigSourceConfig config = defaultConfig();
        config.namespace = Optional.of("demo");
        List<String> configMaps = Lists.list("cm1");
        config.configMaps = Optional.of(configMaps);

        ConfigMap configMap = configMapBuilder("cm1")
                .addToData("some.key", "someValue").addToData("some.other", "someOtherValue").build();

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubNamespacedConfigMap(kubernetesClient, configMap, "cm1");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isNotEmpty();
        ConfigSource next = configSources.iterator().next();
        assertThat(next.getProperties()).containsKeys("some.key", "some.other");
    }

    @Test
    public void testNamespacedConfigSourcesAbsents() {
        KubernetesConfigSourceConfig config = defaultConfig();
        config.namespace = Optional.of("demo");
        List<String> configMaps = Lists.list("cm2");
        config.configMaps = Optional.of(configMaps);

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubNamespacedConfigMap(kubernetesClient, null, "cm2");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isEmpty();
    }

    @Test
    public void testRetrieveConfigSources() {
        KubernetesConfigSourceConfig config = defaultConfig();
        List<String> configMaps = Lists.list("cm1");
        config.configMaps = Optional.of(configMaps);

        ConfigMap configMap = configMapBuilder("cm1")
                .addToData("some.key", "someValue").addToData("some.other", "someOtherValue").build();

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubConfigMap(kubernetesClient, configMap, "cm1");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isNotEmpty();
        ConfigSource next = configSources.iterator().next();
        assertThat(next.getProperties()).containsKeys("some.key", "some.other");
    }

    @Test
    public void testConfigSourcesAbsent() {
        KubernetesConfigSourceConfig config = defaultConfig();
        List<String> configMaps = Lists.list("cm2");
        config.configMaps = Optional.of(configMaps);

        KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
        stubConfigMap(kubernetesClient, null, "cm2");

        KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, kubernetesClient);
        Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
        assertThat(configSources).isEmpty();
    }

    @Test
    public void testConfigSourcesAbsentFailOnMissing() {
        try {
            KubernetesConfigSourceConfig config = defaultConfig();
            config.namespace = Optional.of("demo");
            List<String> configMaps = Lists.list("cm2");
            config.configMaps = Optional.of(configMaps);
            config.failOnMissingConfig = true;

            KubernetesClient kubernetesClient = Mockito.mock(KubernetesClient.class);
            stubNamespacedConfigMap(kubernetesClient, null, "cm2");

            KubernetesConfigSourceProvider kcsp = new KubernetesConfigSourceProvider(config, kubernetesClient);
            Iterable<ConfigSource> configSources = kcsp.getConfigSources(null);
            fail("an exception should be raised");
        } catch (RuntimeException expected) {
        }
    }

    private void stubNamespacedConfigMap(KubernetesClient kubernetesClient, ConfigMap configMap, String configMapName) {
        MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>> mixedOperation = (MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>) mock(
                MixedOperation.class);
        when(kubernetesClient.configMaps()).thenReturn(mixedOperation);
        Resource<ConfigMap, DoneableConfigMap> resource = (Resource<ConfigMap, DoneableConfigMap>) mock(Resource.class);
        NonNamespaceOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>> nsClient = (NonNamespaceOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>) mock(
                NonNamespaceOperation.class);
        when(mixedOperation.inNamespace("demo")).thenReturn(nsClient);
        when(nsClient.withName(configMapName)).thenReturn(resource);

        when(resource.get()).thenReturn(configMap);
        Config kubernetesConfig = mock(Config.class);
        when(kubernetesClient.getConfiguration()).thenReturn(kubernetesConfig);
        when(kubernetesConfig.getMasterUrl()).thenReturn("url");

    }

    private void stubConfigMap(KubernetesClient kubernetesClient, ConfigMap configMap, String configMapName) {
        MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>> mixedOperation = (MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>>) mock(
                MixedOperation.class);
        when(kubernetesClient.configMaps()).thenReturn(mixedOperation);
        Resource<ConfigMap, DoneableConfigMap> resource = (Resource<ConfigMap, DoneableConfigMap>) mock(Resource.class);
        when(mixedOperation.withName(configMapName)).thenReturn(resource);
        when(resource.get()).thenReturn(configMap);
        Config kubernetesConfig = mock(Config.class);
        when(kubernetesClient.getConfiguration()).thenReturn(kubernetesConfig);
        when(kubernetesConfig.getMasterUrl()).thenReturn("url");

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
