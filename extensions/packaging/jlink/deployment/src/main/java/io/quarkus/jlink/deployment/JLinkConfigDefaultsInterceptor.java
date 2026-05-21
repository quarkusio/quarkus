package io.quarkus.jlink.deployment;

import jakarta.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

/**
 * This class connects to {@link JLinkConfigDefaults} until such time that the config
 * infrastructure can do this itself.
 */
// todo: remove if we get @WithDynamicDefaults
@Priority(Integer.MIN_VALUE + 1)
public final class JLinkConfigDefaultsInterceptor implements ConfigSourceInterceptor {
    private final JLinkConfigDefaults defaults = new JLinkConfigDefaults();

    public ConfigValue getValue(final ConfigSourceInterceptorContext context, final String name) {
        return switch (name) {
            case "quarkus.jlink.launcher-name" -> defaults.launcherName(context);
            case "quarkus.jlink.output-directory" -> defaults.outputDirectory(context);
            // override legacy packaging config to not produce a JAR when this is enabled
            case "quarkus.package.jar.enabled" -> context.proceed(name).withValue("false");
            default -> context.proceed(name);
        };
    }
}
