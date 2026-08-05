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
     * The default output directory is derived from the default packaging directory.
     *
     * @param ctxt the interceptor context (must not be {@code null})
     * @return the config value, or {@code null} if it is not found
     */
    public ConfigValue outputDirectory(ConfigSourceInterceptorContext ctxt) {
        return ctxt.restart("quarkus.package.output-directory");
    }

    /**
     * The default JLink launcher name is derived from the packaging out put name.
     *
     * @param ctxt the interceptor context (must not be {@code null})
     * @return the config value, or {@code null} if it is not found
     */
    public ConfigValue launcherName(ConfigSourceInterceptorContext ctxt) {
        return ctxt.restart("quarkus.package.output-name");
    }
}
