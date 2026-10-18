package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(SoftAssertionsExtension.class)
public class BuildConfigurationTest extends QuarkusGradleWrapperTestBase {
    @InjectSoftAssertions
    SoftAssertions soft;

    private static final String ROOT_PROJECT_NAME = "build-configuration";

    private static final String WITH_APPLICATION_PROPERTIES_PROJECT_NAME = "with-application-properties";
    private static final String WITH_BUILD_CONFIGURATION_PROJECT_NAME = "with-build-configuration";
    private static final String WITHOUT_CONFIGURATION_PROJECT_NAME = "without-configuration";

    private static final String DEFAULT_OUTPUT_DIR = "quarkus-app";
    private static final String BUILD_GRADLE_OUTPUT_DIR = "build-gradle-output-dir";
    private static final String APPLICATION_PROPERTIES_OUTPUT_DIR = "application-properties-output-dir";

    private static final String FAST_JAR_FILE = "quarkus-run.jar";

    final class PrjPaths {
        final Path projectDir;
        final Path buildDirPath;
        final Path quarkusAppPath;
        final Path uberJar;
        final Path fastJar;
        final Path libDeploymentDir;
        final String project;
        final String outputDir;

        PrjPaths(File rootDir, String project, String outputDir) {
            this.project = project;
            this.outputDir = outputDir;
            this.projectDir = rootDir.toPath().resolve(project);
            this.buildDirPath = projectDir.resolve("build");
            this.quarkusAppPath = buildDirPath.resolve(outputDir);
            this.uberJar = (DEFAULT_OUTPUT_DIR.equals(outputDir) ? buildDirPath : quarkusAppPath)
                    .resolve(project + "-1.0.0-SNAPSHOT-runner.jar");
            this.fastJar = quarkusAppPath.resolve(FAST_JAR_FILE);
            this.libDeploymentDir = quarkusAppPath.resolve("lib").resolve("deployment");
        }

        void verify(String packageType) {
            soft.assertThat(buildDirPath).describedAs("sub project '%s', package type '%s'", project, packageType)
                    .isDirectory();
            switch (packageType) {
                case "uber-jar":
                    if (!DEFAULT_OUTPUT_DIR.equals(outputDir)) {
                        soft.assertThat(quarkusAppPath).describedAs("sub project '%s', package type '%s'", project, packageType)
                                .isDirectory();
                    } else {
                        soft.assertThat(quarkusAppPath).describedAs("sub project '%s', package type '%s'", project, packageType)
                                .satisfiesAnyOf(p -> assertThat(p).doesNotExist(), p -> assertThat(p).isEmptyDirectory());
                    }
                    soft.assertThat(uberJar).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .isNotEmptyFile();
                    soft.assertThat(fastJar).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .doesNotExist();
                    soft.assertThat(libDeploymentDir).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .doesNotExist();
                    break;
                case "fast-jar":
                    soft.assertThat(quarkusAppPath).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .isDirectory();
                    soft.assertThat(uberJar).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .doesNotExist();
                    soft.assertThat(fastJar).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .isNotEmptyFile();
                    soft.assertThat(libDeploymentDir).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .doesNotExist();
                    break;
                case "mutable-jar":
                    soft.assertThat(quarkusAppPath).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .isDirectory();
                    soft.assertThat(uberJar).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .doesNotExist();
                    soft.assertThat(fastJar).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .isNotEmptyFile();
                    soft.assertThat(libDeploymentDir).describedAs("sub project '%s', package type '%s'", project, packageType)
                            .isDirectory();
                    break;
                default:
                    soft.fail("Unknown package type " + packageType);
                    break;
            }
        }
    }

    private void verifyBuild(String override) throws IOException, InterruptedException, URISyntaxException {
        File rootDir = getProjectDir(ROOT_PROJECT_NAME);
        BuildResult buildResult = runGradleWrapper(rootDir, "clean", "quarkusBuild",
                override != null ? "-Dquarkus.package.jar.type=" + override : "-Dfoo=bar");
        soft.assertThat(buildResult.unsuccessfulTasks()).isEmpty();

        // Sub project 'with-application-properties
        PrjPaths withApplicationProperties = new PrjPaths(rootDir, WITH_APPLICATION_PROPERTIES_PROJECT_NAME,
                APPLICATION_PROPERTIES_OUTPUT_DIR);
        withApplicationProperties.verify(override != null ? override : "uber-jar");

        // Sub project 'with-build-configuration'
        PrjPaths withBuildConfiguration = new PrjPaths(rootDir, WITH_BUILD_CONFIGURATION_PROJECT_NAME, BUILD_GRADLE_OUTPUT_DIR);
        withBuildConfiguration.verify(override != null ? override : "uber-jar");

        // Sub project 'without-configuration'
        PrjPaths withoutConfiguration = new PrjPaths(rootDir, WITHOUT_CONFIGURATION_PROJECT_NAME, DEFAULT_OUTPUT_DIR);
        withoutConfiguration.verify(override != null ? override : "fast-jar");

        try {
            soft.assertAll();
        } catch (AssertionError ex) {
            // Be nice and emit as the build result when the test fails
            System.err.println();
            System.err.println(buildResult.getOutput());
            System.err.println();
            buildResult.getTasks().entrySet().stream()
                    .map(e -> String.format("Task '%s' result '%s'", e.getKey(), e.getValue())).sorted()
                    .forEach(System.err::println);
            System.err.println();
            throw ex;
        }
    }

    @Test
    public void buildNoOverride() throws IOException, InterruptedException, URISyntaxException {
        verifyBuild(null);
    }

    @Test
    public void buildFastJarOverride() throws IOException, InterruptedException, URISyntaxException {
        verifyBuild("fast-jar");
    }

    @Test
    public void buildUberJarOverride() throws IOException, InterruptedException, URISyntaxException {
        verifyBuild("uber-jar");
    }

    @Test
    public void buildMutableJarOverride() throws IOException, InterruptedException, URISyntaxException {
        verifyBuild("mutable-jar");
    }
}
