package io.quarkus.kubernetes.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

class ConfigMapConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(ConfigMapConfigSourceProvider.class);

    private final KubernetesConfigSourceConfig config;
    private final KubernetesClient client;

    public ConfigMapConfigSourceProvider(KubernetesConfigSourceConfig config, KubernetesClient client) {
        this.config = config;
        this.client = client;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        if (!config.configMaps.isPresent()) {
            log.debug("No ConfigMaps were configured for config source lookup");
            return Collections.emptyList();
        }

        List<String> configMapNames = config.configMaps.orElse(Collections.emptyList());
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
                    result.addAll(ConfigMapUtil.toConfigSources(configMap));
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
}
