package io.quarkus.it.smallrye.config;

import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;

public class PongConfigSourceInterceptorFactory implements ConfigSourceInterceptorFactory {
    @Override
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
        return (ConfigSourceInterceptor) (context1, name) -> {
            final ConfigValue value = context1.proceed(name);
            if (name.equals("interceptor.play")) {
                return value.withValue(value.getValue() + "pong");
            }

            return value;
        };
    }

    @Override
    public OptionalInt getPriority() {
        return OptionalInt.of(Priorities.LIBRARY + 1000);
    }
}
