package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class ApplicationConfigurationTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldSuccessfullyInjectApplicationConfigInTest() throws Exception {
        File projectDir = getProjectDir("basic-java-library-module");

        BuildResult testResult = runGradleWrapper(projectDir, "clean", ":application:test");

        assertThat(BuildResult.isSuccessful(testResult.getTasks().get(":application:test"))).isTrue();
    }

}
