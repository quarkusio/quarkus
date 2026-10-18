package io.quarkus.gradle;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class TestWithAppJunitPropertiesFileTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunTestsSuccessfully() throws Exception {

        final File projectDir = getProjectDir("with-junit-properties-file");

        BuildResult buildResult = runGradleWrapper(projectDir, "test");

        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":test"))).isTrue();

    }
}
