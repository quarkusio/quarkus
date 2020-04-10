package io.quarkus.kubernetes.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

class KubernetesConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(KubernetesConfigSourceProvider.class);

    private final KubernetesConfigSourceConfig config;
    private final KubernetesClient client;

    private final ConfigMapConfigSourceUtil configMapConfigSourceUtil;
    private final SecretConfigSourceUtil secretConfigSourceUtil;

    public KubernetesConfigSourceProvider(KubernetesConfigSourceConfig config, KubernetesClient client) {
        this.config = config;
        this.client = client;

        this.configMapConfigSourceUtil = new ConfigMapConfigSourceUtil();
        this.secretConfigSourceUtil = new SecretConfigSourceUtil();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        if (!config.configMaps.isPresent() && !config.secrets.isPresent()) {
            log.debug("No ConfigMaps or Secrets were configured for config source lookup");
            return Collections.emptyList();
        }

        List<ConfigSource> result = new ArrayList<>();
        if (config.configMaps.isPresent()) {
            result.addAll(getConfigMapConfigSources(config.configMaps.get()));
        }
        if (config.secrets.isPresent()) {
            result.addAll(getSecretConfigSources(config.secrets.get()));
        }
        return result;
    }

    private List<ConfigSource> getConfigMapConfigSources(List<String> configMapNames) {
        List<ConfigSource> result = new ArrayList<>(configMapNames.size());

        try {
            for (String configMapName : configMapNames) {
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to read ConfigMap " + configMapName);
                }
                ConfigMap configMap = client.configMaps().withName(configMapName).get();
                if (configMap == null) {
                    String message = "ConfigMap '" + configMap + "' not found in namespace '"
                            + client.getConfiguration().getNamespace() + "'";
                    if (config.failOnMissingConfig) {
                        throw new RuntimeException(message);
                    } else {
                        log.info(message);
                    }
                } else {
                    result.addAll(
                            configMapConfigSourceUtil.toConfigSources(configMap.getMetadata().getName(), configMap.getData()));
                    if (log.isDebugEnabled()) {
                        log.debug("Done reading ConfigMap " + configMap);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain configuration for ConfigMap objects for Kubernetes API Server at: "
                    + client.getConfiguration().getMasterUrl(), e);
        }
    }

    private List<ConfigSource> getSecretConfigSources(List<String> secretNames) {
        List<ConfigSource> result = new ArrayList<>(secretNames.size());

        try {
            for (String secretName : secretNames) {
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to read Secret " + secretName);
                }
                Secret secret = client.secrets().withName(secretName).get();
                if (secret == null) {
                    String message = "Secret '" + secret + "' not found in namespace '"
                            + client.getConfiguration().getNamespace() + "'";
                    if (config.failOnMissingConfig) {
                        throw new RuntimeException(message);
                    } else {
                        log.info(message);
                    }
                } else {
                    result.addAll(secretConfigSourceUtil.toConfigSources(secret.getMetadata().getName(), secret.getData()));
                    if (log.isDebugEnabled()) {
                        log.debug("Done reading Secret " + secret);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain configuration for Secret objects for Kubernetes API Server at: "
                    + client.getConfiguration().getMasterUrl(), e);
        }
    }
}
