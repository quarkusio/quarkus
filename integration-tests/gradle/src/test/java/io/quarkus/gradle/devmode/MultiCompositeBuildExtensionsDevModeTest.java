package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.common.collect.ImmutableMap;

public class MultiCompositeBuildExtensionsDevModeTest extends QuarkusDevGradleTestBase {
    @Override
    protected String projectDirectoryName() {
        return "multi-composite-build-extensions-project";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { ":application:clean", ":application:quarkusDev" };
    }

    protected void testDevMode() throws Exception {

        assertThat(getHttpResponse())
                .contains("ready")
                .contains("my-quarkus-project")
                .contains("org.acme.quarkus.sample")
                .contains("1.0-SNAPSHOT");

        assertThat(getHttpResponse("/hello")).contains("hello from LibB and LibA extension enabled: false");

        replace("libraries/libraryA/src/main/java/org/acme/liba/LibA.java",
                ImmutableMap.of("return \"LibA\";", "return \"modifiedA\";"));
        replace("libraries/libraryB/src/main/java/org/acme/libb/LibB.java",
                ImmutableMap.of("return \"LibB\";", "return \"modifiedB\";"));
        replace("application/src/main/resources/application.properties",
                ImmutableMap.of("false", "true"));

        assertThat(getHttpResponse("/hello")).contains("hello from LibB and LibA extension enabled: true");
    }

    @Override
    protected File getProjectDir() {
        File projectDir = super.getProjectDir();
        final File appProperties = new File(projectDir, "application/gradle.properties");
        final File libsProperties = new File(projectDir, "libraries/gradle.properties");
        final File extensionProperties = new File(projectDir, "extensions/example-extension/gradle.properties");
        final File anotherExtensionProperties = new File(projectDir, "extensions/another-example-extension/gradle.properties");
        final Path projectProperties = projectDir.toPath().resolve("gradle.properties");

        try {
            Files.copy(projectProperties, appProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(projectProperties, libsProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(projectProperties, extensionProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(projectProperties, anotherExtensionProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy gradle.properties file", e);
        }
        return projectDir;
    }
}
