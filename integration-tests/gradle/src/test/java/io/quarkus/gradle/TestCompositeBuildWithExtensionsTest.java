package io.quarkus.gradle;

import static io.quarkus.gradle.util.AppModelDeserializer.deserializeAppModel;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.DependencyFlags;

public class TestCompositeBuildWithExtensionsTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void compositeBuildWithExtensions() throws Exception {
        final File projectDir = getProjectDir("multi-composite-build-extensions-project");

        final BuildResult result = runGradleWrapper(projectDir, ":application:clean", ":application:test");

        assertThat(BuildResult.isSuccessful(result.getTasks().get(":application:test"))).isTrue();

        final Path testAppModelDat = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus")
                .resolve("application-model").resolve("quarkus-app-test-model.dat");
        assertThat(testAppModelDat).exists();

        final ApplicationModel testModel = deserializeAppModel(testAppModelDat);
        for (var d : testModel.getDependencies(DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT)) {
            assertFlagSet(d.getFlags(), DependencyFlags.RUNTIME_CP);
            assertFlagSet(d.getFlags(), DependencyFlags.DEPLOYMENT_CP);
            assertFlagSet(d.getFlags(), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
            assertFlagSet(d.getFlags(), DependencyFlags.TOP_LEVEL_RUNTIME_EXTENSION_ARTIFACT);
            assertFlagNotSet(d.getFlags(), DependencyFlags.RELOADABLE);
        }
    }

    private static void assertFlagSet(int actualFlags, int expectedFlag) {
        assertThat(actualFlags & expectedFlag).isEqualTo(expectedFlag);
    }

    private static void assertFlagNotSet(int actualFlags, int expectedFlag) {
        assertThat(actualFlags & expectedFlag).isZero();
    }

    private File testProjectDir;

    @Override
    protected File getProjectDir(String projectName) {
        if (testProjectDir == null) {
            File projectDir = super.getProjectDir(projectName);
            final File appProperties = new File(projectDir, "application/gradle.properties");
            final File libsProperties = new File(projectDir, "libraries/gradle.properties");
            final File extensionProperties = new File(projectDir, "extensions/example-extension/gradle.properties");
            final File anotherExtensionProperties = new File(projectDir,
                    "extensions/another-example-extension/gradle.properties");
            final Path projectProperties = projectDir.toPath().resolve("gradle.properties");

            try {
                Files.copy(projectProperties, appProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(projectProperties, libsProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(projectProperties, extensionProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(projectProperties, anotherExtensionProperties.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to copy gradle.properties file", e);
            }
            this.testProjectDir = projectDir;
        }
        return testProjectDir;
    }
}
