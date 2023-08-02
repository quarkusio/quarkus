package io.quarkus.kubernetes.runtime.config;

import java.util.function.Function;

import io.smallrye.config.FallbackConfigSourceInterceptor;

public class KubernetesConfigFallback extends FallbackConfigSourceInterceptor {
    private static final String QUARKUS_KUBERNETES_CONFIG_PREFIX = "quarkus.kubernetes.";
    private static final String QUARKUS_OPENSHIFT_CONFIG_PREFIX = "quarkus.openshift.";
    private static final int OPENSHIFT_CONFIG_NAME_BEGIN = QUARKUS_OPENSHIFT_CONFIG_PREFIX.length();
    private static final String QUARKUS_KNATIVE_CONFIG_PREFIX = "quarkus.knative.";
    private static final int KNATIVE_CONFIG_NAME_BEGIN = QUARKUS_KNATIVE_CONFIG_PREFIX.length();

    public KubernetesConfigFallback() {
        super(new Function<String, String>() {
            @Override
            public String apply(final String name) {
                if (name.startsWith(QUARKUS_OPENSHIFT_CONFIG_PREFIX)) {
                    return QUARKUS_KUBERNETES_CONFIG_PREFIX + name.substring(OPENSHIFT_CONFIG_NAME_BEGIN);
                } else if (name.startsWith(QUARKUS_KNATIVE_CONFIG_PREFIX)) {
                    return QUARKUS_KUBERNETES_CONFIG_PREFIX + name.substring(KNATIVE_CONFIG_NAME_BEGIN);
                }
                return name;
            }
        });
    }
}
