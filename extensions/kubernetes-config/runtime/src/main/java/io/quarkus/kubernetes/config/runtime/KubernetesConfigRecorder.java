package io.quarkus.kubernetes.config.runtime;

import org.jboss.logging.Logger;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KubernetesConfigRecorder {
    private static final Logger log = Logger.getLogger(KubernetesConfigRecorder.class);

    private final KubernetesConfigBuildTimeConfig buildTimeConfig;
    private final RuntimeValue<KubernetesConfigSourceConfig> runtimeConfig;

    public KubernetesConfigRecorder(
            final KubernetesConfigBuildTimeConfig buildTimeConfig,
            final RuntimeValue<KubernetesConfigSourceConfig> runtimeConfig) {
        this.buildTimeConfig = buildTimeConfig;
        this.runtimeConfig = runtimeConfig;
    }

    public void warnAboutSecrets() {
        if (runtimeConfig.getValue().secrets().isPresent()
                && !runtimeConfig.getValue().secrets().get().isEmpty()
                && !buildTimeConfig.secretsEnabled()) {
            log.warn("Configuration is read from Secrets " + runtimeConfig.getValue().secrets().get()
                    + ", but quarkus.kubernetes-config.secrets.enabled is false."
                    + " Check if your application's service account has enough permissions to read secrets.");
        }
    }
}
