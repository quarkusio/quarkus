package io.quarkus.gradle;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class IntegrationTestBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunIntegrationTestAsPartOfBuild() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", "quarkusIntTest");

        assertThat(buildResult.getTasks().get(":test")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(buildResult.getTasks().get(":quarkusIntTest")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }

}
