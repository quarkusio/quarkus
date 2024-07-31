package io.quarkus.deployment.configuration;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.LogBuildTimeConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

/**
 * Even if the Log and Console mappings are marked as runtime, they are also used during build time.
 * <p>
 * We cannot register the mappings in the core runtime module because {@link io.smallrye.config.SmallRyeConfig}
 * requires ASM to load the mappings. When we run a Quarkus test, Quarkus will generate the bytecode for the mappings,
 * so we don't need ASM. In a non-Quarkus tests, ASM must be present in the classpath, which we want
 * to avoid (even if they are in the test scope). The logging mappings shouldn't be loaded when running a non-Quarkus
 * test because they are not required.
 *
 * @see LoggingSetupRecorder#initializeBuildTimeLogging(LogRuntimeConfig, LogBuildTimeConfig, ConsoleRuntimeConfig, Map, List,
 *      LaunchMode)
 */
public class BuildTimeConfigBuilderCustomizer implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(final SmallRyeConfigBuilder builder) {
        builder.withMapping(LogBuildTimeConfig.class)
                .withMapping(LogRuntimeConfig.class)
                .withMapping(ConsoleRuntimeConfig.class);
    }
}
