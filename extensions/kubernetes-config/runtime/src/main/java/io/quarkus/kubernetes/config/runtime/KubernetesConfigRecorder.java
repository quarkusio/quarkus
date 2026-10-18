package io.quarkus.kubernetes.config.runtime;

import org.jboss.logging.Logger;

public class KubernetesConfigRecorder {
    private static final Logger log = Logger.getLogger(KubernetesConfigRecorder.class);

    public static void warnAboutSecrets(boolean secretsEnabled, KubernetesConfigSourceConfig runtimeConfig) {
        if (runtimeConfig.secrets().isPresent()
                && !runtimeConfig.secrets().get().isEmpty()
                && !secretsEnabled) {
            log.warn("Configuration is read from Secrets " + runtimeConfig.secrets().get()
                    + ", but quarkus.kubernetes-config.secrets.enabled is false."
                    + " Check if your application's service account has enough permissions to read secrets.");
        }
    }
}
