package io.quarkus.jlink.deployment;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

/**
 * The dynamic configuration default values for {@link JLinkConfig}.
 */
public final class JLinkConfigDefaults {
    public JLinkConfigDefaults() {
    }

    /**
     * The default JLink launcher name is derived from the package output name.
     *
     * @param ctxt the interceptor context (must not be {@code null})
     * @return the config value, or {@code null} if it is not found
     */
    public ConfigValue launcherName(ConfigSourceInterceptorContext ctxt) {
        return ctxt.restart("quarkus.package.output-name");
    }
}
