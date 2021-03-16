package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class MultiModuleKotlinProjectBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {
        final File projectDir = getProjectDir("multi-module-kotlin-project");
        final BuildResult build = runGradleWrapper(projectDir, "clean", "build");
        assertThat(build.getTasks().get(":quarkusGenerateCode")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
        assertThat(build.getTasks().get(":compileKotlin")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }
}
