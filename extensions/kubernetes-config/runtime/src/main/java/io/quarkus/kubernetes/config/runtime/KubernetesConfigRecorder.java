package io.quarkus.kubernetes.config.runtime;

import java.util.Collections;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.kubernetes.client.runtime.KubernetesClientBuildConfig;
import io.quarkus.kubernetes.client.runtime.KubernetesClientUtils;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.AbstractRawDefaultConfigSource;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;

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

    private boolean isExplicitlyDisabled() {
        SmallRyeConfig smallRyeConfig = (SmallRyeConfig) ConfigProvider.getConfig();
        ConfigValue configValue = smallRyeConfig.getConfigValue(CONFIG_ENABLED_PROPERTY_NAME);
        if (AbstractRawDefaultConfigSource.NAME.equals(configValue.getConfigSourceName())) {
            return false;
        }
        if (configValue.getValue() != null) {
            return !smallRyeConfig.getValue(CONFIG_ENABLED_PROPERTY_NAME, Boolean.class);
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
