package io.quarkus.config;

import java.util.Set;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.SecretKeysConfigSourceInterceptor;

public class SecretKeysConfigInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext configSourceInterceptorContext) {
        return new SecretKeysConfigSourceInterceptor(Set.of("secrets.my.secret"));
    }
}
