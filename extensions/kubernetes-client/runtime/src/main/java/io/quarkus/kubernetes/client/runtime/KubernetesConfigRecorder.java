package io.quarkus.kubernetes.client.runtime;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KubernetesConfigRecorder {

    private static final Logger log = Logger.getLogger(KubernetesConfigRecorder.class);

    public RuntimeValue<ConfigSourceProvider> configMaps(KubernetesConfigSourceConfig kubernetesConfigSourceConfig,
            KubernetesClientBuildConfig clientConfig) {
        if (!kubernetesConfigSourceConfig.enabled) {
            log.debug(
                    "No attempt will be made to obtain configuration from the Kubernetes API server because the functionality has been disabled via configuration");
            return emptyRuntimeValue();
        }

        return new RuntimeValue<>(new KubernetesConfigSourceProvider(kubernetesConfigSourceConfig,
                KubernetesClientUtils.createClient(clientConfig)));
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
