package io.quarkus.jlink.deployment;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * The factory for the defaults interceptor.
 */
// todo: remove if we get @WithDynamicDefaults
public final class JLinkConfigCustomizer implements SmallRyeConfigBuilderCustomizer {
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        //builder.withInterceptors(new JLinkConfigDefaultsInterceptor());
    }

    public int priority() {
        return Integer.MIN_VALUE + 1;
    }
}
