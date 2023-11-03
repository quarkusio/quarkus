package io.quarkus.kubernetes.runtime.config;

import java.util.Iterator;
import java.util.function.Function;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.FallbackConfigSourceInterceptor;

public class KubernetesConfigFallback extends FallbackConfigSourceInterceptor {
    public KubernetesConfigFallback() {
        super(new FallbackToKubernetesConfig());
    }

    @Override
    public Iterator<String> iterateNames(final ConfigSourceInterceptorContext context) {
        return context.iterateNames();
    }

    private static class FallbackToKubernetesConfig implements Function<String, String> {
        @Override
        public String apply(final String name) {
            if (name.startsWith("quarkus.openshift.")) {
                return "quarkus.kubernetes." + name.substring(18);
            } else if (name.startsWith("quarkus.knative.")) {
                return "quarkus.kubernetes." + name.substring(16);
            }
            return name;
        }
    }
}
