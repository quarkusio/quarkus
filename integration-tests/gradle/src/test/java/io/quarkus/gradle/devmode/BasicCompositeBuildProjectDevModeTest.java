package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.common.collect.ImmutableMap;

public class BasicCompositeBuildProjectDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "basic-composite-build-project";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { ":application:clean", ":application:quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse())
                .contains("ready")
                .contains("my-quarkus-project")
                .contains("org.acme.quarkus.sample")
                .contains("1.0-SNAPSHOT");

        assertThat(getHttpResponse("/hello")).contains("hello LibA");

        replace("libraries/libraryA/src/main/java/org/acme/liba/LibA.java",
                ImmutableMap.of("return \"LibA\";", "return \"modified\";"));

        assertUpdatedResponseContains("/hello", "hello modified");
    }

    @Override
    protected File getProjectDir() {
        File projectDir = super.getProjectDir();
        final File appProperties = new File(projectDir, "application/gradle.properties");
        final File libsProperties = new File(projectDir, "libraries/gradle.properties");
        final Path projectProperties = projectDir.toPath().resolve("gradle.properties");

        try {
            Files.copy(projectProperties, appProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(projectProperties, libsProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy gradle.properties file", e);
        }
        return projectDir;
    }
}
