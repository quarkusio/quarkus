package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BuildForkOptionsAreIncludedInQuarkusBuildTaskTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBuildForkOptionsAreProcessed() throws Exception {
        var projectDir = getProjectDir("basic-java-application-with-fork-options");
        var buildResult = runGradleWrapper(projectDir, "clean", "quarkusBuild");
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":quarkusGenerateCode"))).isTrue();
        assertThat(buildResult.getOutput().contains("message!")).isTrue();
    }
}
