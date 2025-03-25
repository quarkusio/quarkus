package io.quarkus.gradle;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class IntegrationTestBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunIntegrationTestAsPartOfBuild() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", "quarkusIntTest");

        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":quarkusIntTest"))).isTrue();
    }

}
