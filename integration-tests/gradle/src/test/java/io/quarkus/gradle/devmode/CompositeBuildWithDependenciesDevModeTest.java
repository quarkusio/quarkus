package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;

@Disabled("flaky")
public class CompositeBuildWithDependenciesDevModeTest extends QuarkusDevGradleTestBase {

    @Override
    protected String projectDirectoryName() {
        return "composite-project-with-dependencies";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev" };
    }

    @Override
    protected void testDevMode() throws Exception {
        assertThat(getHttpResponse("/product")).contains("[]");
    }

    @Override
    protected File getProjectDir() {
        File projectDir = super.getProjectDir();
        final File restAppProjectProperties = new File(projectDir, "gradle-rest/gradle.properties");
        final File daoAppProjectProperties = new File(projectDir, "gradle-dao/gradle.properties");
        final Path projectProperties = projectDir.toPath().resolve("gradle.properties");

        try {
            Files.copy(projectProperties, restAppProjectProperties.toPath());
            Files.copy(projectProperties, daoAppProjectProperties.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to copy gradle.properties file", e);
        }
        this.projectDir = restAppProjectProperties.getParentFile();
        return this.projectDir;
    }
}
