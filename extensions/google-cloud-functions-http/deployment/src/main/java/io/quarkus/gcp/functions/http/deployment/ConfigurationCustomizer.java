package io.quarkus.gcp.functions.http.deployment;

import java.util.OptionalInt;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigSourceInterceptorFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * A configuration customizer to change the default packaging to uber-jar.
 */
public final class ConfigurationCustomizer implements SmallRyeConfigBuilderCustomizer {
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withInterceptorFactories(new ConfigSourceInterceptorFactory() {
            public ConfigSourceInterceptor getInterceptor(final ConfigSourceInterceptorContext context) {
                return (ic, name) -> switch (name) {
                    case "quarkus.package.jar.type" ->
                        ConfigValue.builder().withName(name).withValue("uber-jar").build();
                    default -> ic.proceed(name);
                };
            }

            public OptionalInt getPriority() {
                return OptionalInt.of(Integer.MIN_VALUE + 100);
            }
        });
    }
}
