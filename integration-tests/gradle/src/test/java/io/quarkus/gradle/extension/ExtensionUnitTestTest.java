package io.quarkus.gradle.extension;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.quarkus.gradle.BuildResult;
import io.quarkus.gradle.QuarkusGradleWrapperTestBase;

public class ExtensionUnitTestTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunTestWithSuccess() throws Exception {
        File projectDir = getProjectDir("extensions/simple-extension");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", ":deployment:test", ":runtime:processResources",
                "--no-build-cache");
        BuildResult repeatedResources = runGradleWrapper(projectDir, ":runtime:processResources", "--no-build-cache");

        assertThat(buildResult.getTasks().get((":deployment:test"))).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(repeatedResources.getTasks().get(":runtime:extensionDescriptor"))
                .isEqualTo(BuildResult.UPTODATE_OUTCOME);
        assertThat(repeatedResources.getTasks().get(":runtime:processResources"))
                .isEqualTo(BuildResult.UPTODATE_OUTCOME);

        Path processedMetadata = projectDir.toPath().resolve("runtime/build/resources/main/META-INF");
        assertThat(processedMetadata.resolve("quarkus-extension.properties")).isRegularFile();
        assertThat(processedMetadata.resolve("quarkus-extension.yaml")).isRegularFile();
    }

}
