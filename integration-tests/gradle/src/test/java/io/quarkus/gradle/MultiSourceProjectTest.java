package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MultiSourceProjectTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunTest() throws Exception {
        final File projectDir = getProjectDir("multi-source-project");
        final BuildResult buildResult = runGradleWrapper(projectDir, ":clean", ":test");

        assertThat(buildResult.getTasks().get(":test")).isEqualTo(BuildResult.SUCCESS_OUTCOME);

    }

}
