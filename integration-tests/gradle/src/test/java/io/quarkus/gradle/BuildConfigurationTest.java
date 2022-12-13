package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class BuildConfigurationTest extends QuarkusGradleWrapperTestBase {

    private static final String WITHOUT_CONFIGURATION_PROJECT_NAME = "without-configuration";

    private static final String ROOT_PROJECT_NAME = "build-configuration";

    private static final String BUILD_GRADLE_PROJECT_NAME = "with-build-configuration";

    private static final String BUILD_GRADLE_OUTPUT_DIR = "build-gradle-output-dir";

    private static final String BUILD_GRADLE_UBER_JAR_FILE = "with-build-configuration-1.0.0-SNAPSHOT-runner.jar";

    private static final String APPLICATION_PROPERTIES_PROJECT_NAME = "with-application-properties";

    private static final String APPLICATION_PROPERTIES_OUTPUT_DIR = "application-properties-output-dir";
    private static final String APPLICATION_PROPERTIES_UBER_JAR_FILE = "with-application-properties-1.0.0-SNAPSHOT-runner.jar";

    @Test
    public void buildUseApplicationPropertiesUberJar() throws IOException, InterruptedException, URISyntaxException {
        final File projectDir = getProjectDir(ROOT_PROJECT_NAME);
        runGradleWrapper(projectDir, "clean", "quarkusBuild");
        final Path projectPath = projectDir.toPath();
        final Path buildDirPath = projectPath.resolve(APPLICATION_PROPERTIES_PROJECT_NAME).resolve("build");
        assertThat(buildDirPath.resolve(APPLICATION_PROPERTIES_OUTPUT_DIR)).exists();
        final Path quarkusAppPath = buildDirPath.resolve(APPLICATION_PROPERTIES_OUTPUT_DIR);
        final Path jar = quarkusAppPath.resolve(APPLICATION_PROPERTIES_UBER_JAR_FILE);
        assertThat(jar).exists();
    }

    @Test
    public void applicationPropertiesWithDPropsFastJar()
            throws IOException, InterruptedException, URISyntaxException {
        final File projectDir = getProjectDir(ROOT_PROJECT_NAME);
        runGradleWrapper(projectDir, "clean", "quarkusBuild", "-Dquarkus.package.type=fast-jar");
        final Path projectPath = projectDir.toPath();
        final Path buildDirPath = projectPath.resolve(APPLICATION_PROPERTIES_PROJECT_NAME).resolve("build");
        assertThat(buildDirPath.resolve(APPLICATION_PROPERTIES_OUTPUT_DIR)).exists();
        final Path quarkusAppPath = buildDirPath.resolve(APPLICATION_PROPERTIES_OUTPUT_DIR);
        assertThat(quarkusAppPath.resolve("quarkus-run.jar")).exists();
        assertThat(quarkusAppPath.resolve(APPLICATION_PROPERTIES_UBER_JAR_FILE)).doesNotExist();
        final Path deploymentDirPath = quarkusAppPath.resolve("lib").resolve("deployment");
        assertThat(deploymentDirPath).doesNotExist();
    }

    @Test
    public void applicationPropertiesWithDPropsUnmutableJar()
            throws IOException, InterruptedException, URISyntaxException {
        final File projectDir = getProjectDir(ROOT_PROJECT_NAME);
        runGradleWrapper(projectDir, "clean", "quarkusBuild", "-Dquarkus.package.type=mutable-jar");
        final Path projectPath = projectDir.toPath();
        final Path buildDirPath = projectPath.resolve(APPLICATION_PROPERTIES_PROJECT_NAME).resolve("build");
        assertThat(buildDirPath.resolve(APPLICATION_PROPERTIES_OUTPUT_DIR)).exists();
        final Path quarkusAppPath = buildDirPath.resolve(APPLICATION_PROPERTIES_OUTPUT_DIR);
        assertThat(quarkusAppPath.resolve("quarkus-run.jar")).exists();
        assertThat(quarkusAppPath.resolve(APPLICATION_PROPERTIES_UBER_JAR_FILE)).doesNotExist();
        final Path deploymentDirPath = quarkusAppPath.resolve("lib").resolve("deployment");
        assertThat(deploymentDirPath).exists();
    }

    @Test
    public void buildConfigUberJar() throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir(ROOT_PROJECT_NAME);
        runGradleWrapper(projectDir, "clean", "quarkusBuild");
        final Path projectPath = projectDir.toPath();
        final Path buildDirPath = projectPath.resolve(BUILD_GRADLE_PROJECT_NAME).resolve("build");
        assertThat(buildDirPath.resolve(BUILD_GRADLE_OUTPUT_DIR)).exists();
        final Path quarkusAppPath = buildDirPath.resolve(BUILD_GRADLE_OUTPUT_DIR);
        final Path jar = quarkusAppPath.resolve(BUILD_GRADLE_UBER_JAR_FILE);
        assertThat(jar).exists();
    }

    @Test
    public void buildConfigFastJarOverride() throws IOException, URISyntaxException, InterruptedException {
        final File projectDir = getProjectDir(ROOT_PROJECT_NAME);
        runGradleWrapper(projectDir, "clean", "quarkusBuild", "-Dquarkus.package.type=fast-jar");
        final Path projectPath = projectDir.toPath();
        final Path buildDirPath = projectPath.resolve(BUILD_GRADLE_PROJECT_NAME).resolve("build");
        assertThat(buildDirPath.resolve(BUILD_GRADLE_OUTPUT_DIR)).exists();
        final Path quarkusAppPath = buildDirPath.resolve(BUILD_GRADLE_OUTPUT_DIR);
        assertThat(quarkusAppPath.resolve("quarkus-run.jar")).exists();
        assertThat(quarkusAppPath.resolve(BUILD_GRADLE_UBER_JAR_FILE)).doesNotExist();
        final Path deploymentDirPath = quarkusAppPath.resolve("lib").resolve("deployment");
        assertThat(deploymentDirPath).doesNotExist();
    }

    @Test
    public void withoutConfigurationBuildsDefaults() throws IOException, InterruptedException, URISyntaxException {
        final File projectDir = getProjectDir(ROOT_PROJECT_NAME);
        runGradleWrapper(projectDir, "clean", "quarkusBuild");
        final Path projectPath = projectDir.toPath();
        final Path buildDirPath = projectPath.resolve(WITHOUT_CONFIGURATION_PROJECT_NAME).resolve("build");
        assertThat(buildDirPath.resolve("quarkus-app")).exists();
        final Path quarkusAppPath = buildDirPath.resolve("quarkus-app");
        final Path jar = quarkusAppPath.resolve("quarkus-run.jar");
        assertThat(jar).exists();
    }
}
