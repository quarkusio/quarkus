package io.quarkus.kubernetes.client.runtime;

import java.util.Collections;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.AbstractRawDefaultConfigSource;

@Recorder
public class KubernetesConfigRecorder {

    private static final Logger log = Logger.getLogger(KubernetesConfigRecorder.class);

    private static final String CONFIG_ENABLED_PROPERTY_NAME = "quarkus.kubernetes-config.enabled";

    public RuntimeValue<ConfigSourceProvider> configSources(KubernetesConfigSourceConfig kubernetesConfigSourceConfig,
            KubernetesConfigBuildTimeConfig buildTimeConfig,
            KubernetesClientBuildConfig clientConfig,
            TlsConfig tlsConfig) {
        if ((!kubernetesConfigSourceConfig.enabled && !buildTimeConfig.secretsEnabled) || isExplicitlyDisabled()) {
            log.debug(
                    "No attempt will be made to obtain configuration from the Kubernetes API server because the functionality has been disabled via configuration");
            return emptyRuntimeValue();
        }

        return new RuntimeValue<>(new KubernetesConfigSourceProvider(kubernetesConfigSourceConfig, buildTimeConfig,
                KubernetesClientUtils.createClient(clientConfig, tlsConfig)));
    }

    // We don't want to enable the reading of anything if 'quarkus.kubernetes-config.enabled' is EXPLICITLY set to false
    // In order to figure out whether the user has actually set the property, we take advantage of the fact that Smallrye
    // Config returns the ConfigSources by descending order - we thus check each ConfigSource to see if the value has been set
    private boolean isExplicitlyDisabled() {
        Config config = ConfigProvider.getConfig();
        Iterable<ConfigSource> configSources = config.getConfigSources();
        for (ConfigSource configSource : configSources) {
            // this is the ConfigSource that Quarkus Config creates and if we've reached this point, then we know that the
            // use has not explicitly configured the value
            if (configSource instanceof AbstractRawDefaultConfigSource) {
                return false;
            }
            if (configSource.getPropertyNames().contains(CONFIG_ENABLED_PROPERTY_NAME)) {
                // TODO: should probably use converter here
                return !Boolean.parseBoolean(configSource.getValue(CONFIG_ENABLED_PROPERTY_NAME));
            }
        }
        return false;
    }

    public void warnAboutSecrets(KubernetesConfigSourceConfig config, KubernetesConfigBuildTimeConfig buildTimeConfig) {
        if (config.secrets.isPresent()
                && !config.secrets.get().isEmpty()
                && !buildTimeConfig.secretsEnabled) {
            log.warn("Configuration is read from Secrets " + config.secrets.get()
                    + ", but quarkus.kubernetes-config.secrets.enabled is false."
                    + " Check if your application's service account has enough permissions to read secrets.");
        }
    }

    private RuntimeValue<ConfigSourceProvider> emptyRuntimeValue() {
        return new RuntimeValue<>(new EmptyConfigSourceProvider());
    }

    private static class EmptyConfigSourceProvider implements ConfigSourceProvider {

        @Override
        public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
            return Collections.emptyList();
        }
    }

}
