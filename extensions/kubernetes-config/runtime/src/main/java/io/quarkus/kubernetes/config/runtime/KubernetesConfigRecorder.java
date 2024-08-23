package io.quarkus.kubernetes.config.runtime;

import org.jboss.logging.Logger;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class KubernetesConfigRecorder {

    private static final Logger log = Logger.getLogger(KubernetesConfigRecorder.class);

    public void warnAboutSecrets(KubernetesConfigBuildTimeConfig buildTimeConfig, KubernetesConfigSourceConfig config) {
        if (config.secrets().isPresent()
                && !config.secrets().get().isEmpty()
                && !buildTimeConfig.secretsEnabled()) {
            log.warn("Configuration is read from Secrets " + config.secrets().get()
                    + ", but quarkus.kubernetes-config.secrets.enabled is false."
                    + " Check if your application's service account has enough permissions to read secrets.");
        }
    }
}
