package io.quarkus.it.smallrye.config;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;

@Priority(Priorities.LIBRARY + 900)
public class PingConfigSourceInterceptor implements ConfigSourceInterceptor {
    @Override
    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        final ConfigValue value = context.proceed(name);
        if (name.equals("interceptor.play")) {
            return ConfigValue.builder().withName(name).withValue("ping").build();
        }

        return value;
    }
}
