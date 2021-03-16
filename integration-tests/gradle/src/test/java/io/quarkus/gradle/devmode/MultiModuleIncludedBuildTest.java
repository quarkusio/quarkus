package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MultiModuleIncludedBuildTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "multi-module-included-build";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "--include-build", "../external-library", "clean", "quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/hello")).contains("foo bar");
    }

    @Override
    protected File getProjectDir() {
        File projectDir = super.getProjectDir();
        final File appProjectProperties = new File(projectDir, "app/gradle.properties");
        final File libProjectProperties = new File(projectDir, "external-library/gradle.properties");
        final Path projectProperties = projectDir.toPath().resolve("gradle.properties");

        try {
            Files.copy(projectProperties, appProjectProperties.toPath());
            Files.copy(projectProperties, libProjectProperties.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy gradle.properties file", e);
        }
        this.projectDir = appProjectProperties.getParentFile();
        return this.projectDir;
    }
}
