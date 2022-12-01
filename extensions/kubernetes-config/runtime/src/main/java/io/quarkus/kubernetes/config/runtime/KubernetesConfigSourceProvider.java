package io.quarkus.kubernetes.config.runtime;

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
    private final KubernetesConfigBuildTimeConfig buildTimeConfig;
    private final KubernetesClient client;

    private final ConfigMapConfigSourceUtil configMapConfigSourceUtil;
    private final SecretConfigSourceUtil secretConfigSourceUtil;

    public KubernetesConfigSourceProvider(KubernetesConfigSourceConfig config, KubernetesConfigBuildTimeConfig buildTimeConfig,
            KubernetesClient client) {
        this.config = config;
        this.buildTimeConfig = buildTimeConfig;
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
        if (config.enabled && config.configMaps.isPresent()) {
            result.addAll(getConfigMapConfigSources(config.configMaps.get()));
        }
        if (buildTimeConfig.secretsEnabled && config.secrets.isPresent()) {
            result.addAll(getSecretConfigSources(config.secrets.get()));
        }
        return result;
    }

    private List<ConfigSource> getConfigMapConfigSources(List<String> configMapNames) {
        List<ConfigSource> result = new ArrayList<>(configMapNames.size());

        try {
            for (int i = 0; i < configMapNames.size(); i++) {
                String configMapName = configMapNames.get(i);
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to read ConfigMap " + configMapName);
                }
                ConfigMap configMap;
                String namespace;
                if (config.namespace.isPresent()) {
                    namespace = config.namespace.get();
                    configMap = client.configMaps().inNamespace(namespace).withName(configMapName).get();
                } else {
                    namespace = client.getNamespace();
                    configMap = client.configMaps().withName(configMapName).get();
                }
                if (configMap == null) {
                    logMissingOrFail(configMapName, namespace, "ConfigMap", config.failOnMissingConfig);
                } else {
                    result.addAll(
                            configMapConfigSourceUtil.toConfigSources(configMap.getMetadata(), configMap.getData(),
                                    i));
                    if (log.isDebugEnabled()) {
                        log.debug("Done reading ConfigMap " + configMap.getMetadata().getName());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain configuration for ConfigMap objects from Kubernetes API Server at: "
                    + client.getConfiguration().getMasterUrl(), e);
        }
    }

    private List<ConfigSource> getSecretConfigSources(List<String> secretNames) {
        List<ConfigSource> result = new ArrayList<>(secretNames.size());

        try {
            for (int i = 0; i < secretNames.size(); i++) {
                String secretName = secretNames.get(i);
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to read Secret " + secretName);
                }
                Secret secret;
                String namespace;
                if (config.namespace.isPresent()) {
                    namespace = config.namespace.get();
                    secret = client.secrets().inNamespace(namespace).withName(secretName).get();
                } else {
                    namespace = client.getNamespace();
                    secret = client.secrets().withName(secretName).get();
                }
                if (secret == null) {
                    logMissingOrFail(secretName, namespace, "Secret", config.failOnMissingConfig);
                } else {
                    result.addAll(secretConfigSourceUtil.toConfigSources(secret.getMetadata(), secret.getData(), i));
                    if (log.isDebugEnabled()) {
                        log.debug("Done reading Secret " + secret.getMetadata().getName());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain configuration for Secret objects from Kubernetes API Server at: "
                    + client.getConfiguration().getMasterUrl(), e);
        }
    }

    private void logMissingOrFail(String name, String namespace, String type, boolean failOnMissingConfig) {
        String message = type + " '" + name + "' not found";
        if (namespace == null) {
            message = message
                    + ". No Kubernetes namespace was set (most likely because the application is running outside the Kubernetes cluster). Consider setting 'quarkus.kubernetes-client.namespace=my-namespace' to specify the namespace in which to look up the "
                    + type;
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
