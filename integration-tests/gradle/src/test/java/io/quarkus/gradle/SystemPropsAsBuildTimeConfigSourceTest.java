package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class SystemPropsAsBuildTimeConfigSourceTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("system-props-as-build-time-config-source");

        final File appProperties = new File(projectDir, "application/gradle.properties");
        final File extensionProperties = new File(projectDir, "extensions/example-extension/gradle.properties");

        final Path projectProperties = projectDir.toPath().resolve("gradle.properties");

        try {
            Files.copy(projectProperties, appProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(projectProperties, extensionProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy gradle.properties file", e);
        }

        gradleConfigurationCache(false);
        runGradleWrapper(projectDir,
                "-Dquarkus.example.name=cheburashka",
                "-Dquarkus.example.runtime-name=crocodile",
                "-Dquarkus.package.jar.type=mutable-jar",
                ":example-extension:example-extension-deployment:build",
                // this quarkusIntTest will make sure runtime config properties passed as env vars when launching the app are effective
                ":application:quarkusIntTest");

        final Path buildSystemPropsPath = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app")
                .resolve("quarkus").resolve("build-system.properties");
        assertThat(buildSystemPropsPath).exists();
        var props = new Properties();
        try (var reader = Files.newBufferedReader(buildSystemPropsPath)) {
            props.load(reader);
        }
        assertThat(props).doesNotContainKey("quarkus.example.name");
        assertThat(props).doesNotContainKey("quarkus.example.runtime-name");
    }
}
