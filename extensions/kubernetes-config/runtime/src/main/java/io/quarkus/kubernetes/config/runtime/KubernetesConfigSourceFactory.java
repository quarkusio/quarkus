package io.quarkus.kubernetes.config.runtime;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceContext.ConfigSourceContextConfigSource;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

class KubernetesConfigSourceFactory implements ConfigSourceFactory {

    private static final Logger log = Logger.getLogger(KubernetesConfigSourceFactory.class);

    private final KubernetesClient client;

    private final ConfigMapConfigSourceUtil configMapConfigSourceUtil;
    private final SecretConfigSourceUtil secretConfigSourceUtil;

    /**
     * @param client A Kubernetes Client that is specific to this extension - it must not be shared with any other
     *        parts of the application
     */
    public KubernetesConfigSourceFactory(KubernetesClient client) {
        this.client = client;
        this.configMapConfigSourceUtil = new ConfigMapConfigSourceUtil();
        this.secretConfigSourceUtil = new SecretConfigSourceUtil();
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigSourceContextConfigSource(context))
                .withMapping(KubernetesConfigBuildTimeConfig.class)
                .withMapping(KubernetesConfigSourceConfig.class)
                .build();

        KubernetesConfigBuildTimeConfig kubernetesConfigBuildTimeConfig = config
                .getConfigMapping(KubernetesConfigBuildTimeConfig.class);
        KubernetesConfigSourceConfig kubernetesConfigSourceConfig = config.getConfigMapping(KubernetesConfigSourceConfig.class);

        // TODO - radcortez - Move the check that uses the build time config to the processor and skip the builder registration
        if ((!kubernetesConfigSourceConfig.enabled() && !kubernetesConfigBuildTimeConfig.secretsEnabled())
                || isExplicitlyDisabled(context)) {
            log.debug(
                    "No attempt will be made to obtain configuration from the Kubernetes API server because the functionality has been disabled via configuration");
            return emptyList();
        }

        return getConfigSources(kubernetesConfigSourceConfig, kubernetesConfigBuildTimeConfig.secretsEnabled());
    }

    Iterable<ConfigSource> getConfigSources(final KubernetesConfigSourceConfig config, final boolean secrets) {
        if (config.configMaps().isEmpty() && config.secrets().isEmpty()) {
            log.debug("No ConfigMaps or Secrets were configured for config source lookup");
            return emptyList();
        }

        List<ConfigSource> result = new ArrayList<>();
        if (config.enabled() && config.configMaps().isPresent()) {
            result.addAll(getConfigMapConfigSources(config.configMaps().get(), config));
        }
        if (secrets && config.secrets().isPresent()) {
            result.addAll(getSecretConfigSources(config.secrets().get(), config));
        }
        try {
            client.close(); // we no longer need the client, so we must close it to avoid resource leaks
        } catch (Exception e) {
            log.debug("Error in closing kubernetes client", e);
        }
        return result;
    }

    private boolean isExplicitlyDisabled(ConfigSourceContext context) {
        ConfigValue configValue = context.getValue("quarkus.kubernetes-config.enabled");
        if (DefaultValuesConfigSource.NAME.equals(configValue.getConfigSourceName())) {
            return false;
        }
        if (configValue.getValue() != null) {
            return !Converters.getImplicitConverter(Boolean.class).convert(configValue.getValue());
        }
        return false;
    }

    private List<ConfigSource> getConfigMapConfigSources(List<String> configMapNames, KubernetesConfigSourceConfig config) {
        List<ConfigSource> result = new ArrayList<>(configMapNames.size());

        try {
            for (int i = 0; i < configMapNames.size(); i++) {
                String configMapName = configMapNames.get(i);
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to read ConfigMap " + configMapName);
                }
                ConfigMap configMap;
                String namespace;
                if (config.namespace().isPresent()) {
                    namespace = config.namespace().get();
                    configMap = client.configMaps().inNamespace(namespace).withName(configMapName).get();
                } else {
                    namespace = client.getNamespace();
                    configMap = client.configMaps().withName(configMapName).get();
                }
                if (configMap == null) {
                    logMissingOrFail(configMapName, namespace, "ConfigMap", config.failOnMissingConfig());
                } else {
                    result.addAll(configMapConfigSourceUtil.toConfigSources(configMap.getMetadata(), configMap.getData(), i));
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

    private List<ConfigSource> getSecretConfigSources(List<String> secretNames, KubernetesConfigSourceConfig config) {
        List<ConfigSource> result = new ArrayList<>(secretNames.size());

        try {
            for (int i = 0; i < secretNames.size(); i++) {
                String secretName = secretNames.get(i);
                if (log.isDebugEnabled()) {
                    log.debug("Attempting to read Secret " + secretName);
                }
                Secret secret;
                String namespace;
                if (config.namespace().isPresent()) {
                    namespace = config.namespace().get();
                    secret = client.secrets().inNamespace(namespace).withName(secretName).get();
                } else {
                    namespace = client.getNamespace();
                    secret = client.secrets().withName(secretName).get();
                }
                if (secret == null) {
                    logMissingOrFail(secretName, namespace, "Secret", config.failOnMissingConfig());
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
