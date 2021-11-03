package io.quarkus.kubernetes.service.binding.runtime;

import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KubernetesServiceBindingRecorder {

    private static final Logger log = Logger.getLogger(KubernetesServiceBindingRecorder.class);

    final KubernetesServiceBindingConfig kubernetesServiceBindingConfig;

    public KubernetesServiceBindingRecorder(KubernetesServiceBindingConfig kubernetesServiceBindingConfig) {
        this.kubernetesServiceBindingConfig = kubernetesServiceBindingConfig;
    }

    public RuntimeValue<ConfigSourceProvider> configSources() {
        if (!kubernetesServiceBindingConfig.enabled) {
            log.debug(
                    "No attempt will be made to bind configuration based on Kubernetes ServiceBinding because the feature was not enabled.");
            return emptyRuntimeValue();
        }
        if (!kubernetesServiceBindingConfig.root.isPresent()) {
            log.debug(
                    "No attempt will be made to bind configuration based on Kubernetes Service Binding because the binding root was not specified.");
            return emptyRuntimeValue();
        }

        return new RuntimeValue<>(new KubernetesServiceBindingConfigSourceProvider(kubernetesServiceBindingConfig.root.get()));
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
