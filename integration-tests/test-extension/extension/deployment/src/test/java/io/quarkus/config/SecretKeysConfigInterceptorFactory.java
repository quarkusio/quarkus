package io.quarkus.config;

import java.util.Set;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.PropertyNamesMatcher;
import io.smallrye.config.SecretKeysConfigSourceInterceptor;

public class SecretKeysConfigInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext configSourceInterceptorContext) {
        return new SecretKeysConfigSourceInterceptor(new Secrets(Set.of("secrets.my.secret")));
    }

    static class Secrets extends PropertyNamesMatcher<String> {
        Secrets(Set<String> names) {
            add(names);
        }
    }
}
