package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class IdeLaunchLocalExtensionConfigMappingTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void buildStepConsumesConfigMappingOfLocalExtension() throws Exception {
        final File projectDir = getProjectDir("ide-launch-local-extension-config-mapping");

        final BuildResult result = runGradleWrapper(projectDir, "clean", ":application:runFromMain");

        assertThat(result.getOutput()).contains("Application started with greeting: hello from the application");
    }
}
