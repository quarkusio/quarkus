package io.quarkus.test.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import io.quarkus.runtime.logging.LogRuntimeConfig.FileConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

public class TestConfigCustomizer implements SmallRyeConfigBuilderCustomizer {
    @Override
    public void configBuilder(SmallRyeConfigBuilder builder) {
        builder.withDefaultValue("quarkus.log.file.path", String.join(File.separator, getLogFileLocationParts()));
    }

    private static List<String> getLogFileLocationParts() {
        // TODO - This can probably be smarter. We can probably keep target as a default and have another source in Gradle to override with the Gradle directory layout
        if (Files.isDirectory(Paths.get("build"))) {
            return Arrays.asList("build", FileConfig.DEFAULT_LOG_FILE_NAME);
        }
        return Arrays.asList("target", FileConfig.DEFAULT_LOG_FILE_NAME);
    }
}
