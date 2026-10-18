package io.quarkus.deployment.configuration;

import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.AOT_JAR;
import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.FAST_JAR;
import static io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType.MUTABLE_JAR;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.deployment.pkg.PackageConfig.JarConfig.JarType;
import io.quarkus.deployment.pkg.jar.FastJarFormat;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.logging.LogBuildTimeConfig;
import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Converters;
import io.smallrye.config.DefaultValuesConfigSource;
import io.smallrye.config.PropertiesConfigSource;
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

        builder.withSources(new ConfigSourceFactory() {
            @Override
            public Iterable<ConfigSource> getConfigSources(ConfigSourceContext context) {
                ConfigValue outputDirectory = context.getValue("quarkus.package.output-directory");
                if (outputDirectory.getValue() != null) {
                    return Collections.emptyList();
                }

                ConfigValue jarType = context.getValue("quarkus.package.jar.type");
                if (jarType.getValue() == null) {
                    return Collections.emptyList();
                }

                Converter<JarType> jarTypeConverter = Converters.getImplicitConverter(JarType.class);
                JarType type = jarTypeConverter.convert(jarType.getValue());
                if (type.equals(FAST_JAR) || type.equals(MUTABLE_JAR) || type.equals(AOT_JAR)) {
                    Map<String, String> map = Map.of("quarkus.package.output-directory",
                            FastJarFormat.DEFAULT_FAST_JAR_DIRECTORY_NAME);
                    return Collections.singletonList(new PropertiesConfigSource(map, "BuildTimeConfigSource",
                            DefaultValuesConfigSource.ORDINAL + 1));
                }

                return Collections.emptyList();
            }
        });
    }
}
