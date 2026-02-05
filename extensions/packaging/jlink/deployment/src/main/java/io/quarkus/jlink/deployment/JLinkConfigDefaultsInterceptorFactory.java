package io.quarkus.jlink.deployment;

import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;

/**
 * The factory for the defaults interceptor.
 */
// todo: remove if we get @WithDynamicDefaults
public final class JLinkConfigDefaultsInterceptorFactory implements ConfigSourceInterceptorFactory {
    public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
        return new JLinkConfigDefaultsInterceptor();
    }

    public OptionalInt getPriority() {
        return OptionalInt.of(Integer.MIN_VALUE + 1);
    }
}
