package io.quarkus.deployment.logging;

import io.quarkus.runtime.configuration.ConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

/**
 * Set only for Dev and Test.
 * <p>
 * Because we set the log early in {@code io.quarkus.test.config.LoggingSetupExtension}, the log may end up in rotating
 * files at runtime when running tests, if components log messages before Quarkus sets up the final runtime log. Turning
 * off the rotation ensures that Quarkus log ends up in the same file (if any), set by the
 * {@code io.quarkus.test.config.LoggingSetupExtension}.
 */
public class LoggingConfigBuilder implements ConfigBuilder {
    @Override
    public SmallRyeConfigBuilder configBuilder(SmallRyeConfigBuilder builder) {
        return builder.withDefaultValue("quarkus.log.file.rotation.rotate-on-boot", "false");
    }
}
