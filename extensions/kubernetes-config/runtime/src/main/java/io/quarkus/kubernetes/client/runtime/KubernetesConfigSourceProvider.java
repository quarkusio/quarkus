package io.quarkus.kubernetes.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;

class KubernetesConfigSourceProvider implements ConfigSourceProvider {

    private static final Logger log = Logger.getLogger(KubernetesConfigSourceProvider.class);

    private final KubernetesConfigSourceConfig config;
    private final KubernetesClient client;

    private final ConfigMapConfigSourceUtil configMapConfigSourceUtil;

    public KubernetesConfigSourceProvider(KubernetesConfigSourceConfig config, KubernetesClient client) {
        this.config = config;
        this.client = client;

        this.configMapConfigSourceUtil = new ConfigMapConfigSourceUtil();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        if (!config.configMaps.isPresent()) {
            log.debug("No ConfigMaps were configured for config source lookup");
            return Collections.emptyList();
        }

        List<ConfigSource> result = new ArrayList<>();
        if (config.configMaps.isPresent()) {
            result.addAll(getConfigMapConfigSources(config.configMaps.get()));
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
                    logMissingOrFail(configMapName, client.getNamespace(), "ConfigMap", config.failOnMissingConfig);
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

    private void logMissingOrFail(String name, String namespace, String type, boolean failOnMissingConfig) {
        String message = type + " '" + name + "' not found";
        if (namespace == null) {
            message = message
                    + ". No Kubernetes namespace was set (most likely because the application is running outside the Kubernetes cluster). Consider setting 'quarkus.kubernetes-client.namespace=my-namespace' to specify the namespace in which to look up the ConfigMap";
        } else {
            message = message + " in namespace '" + namespace + "'";
        }
        if (failOnMissingConfig) {
            throw new RuntimeException(message);
        } else {
            log.info(message);
        }
    }
}
