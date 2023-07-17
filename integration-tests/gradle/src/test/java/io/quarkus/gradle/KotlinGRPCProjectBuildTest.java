package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class KotlinGRPCProjectBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {
        final File projectDir = getProjectDir("kotlin-grpc-project");
        final BuildResult build = runGradleWrapper(projectDir, "clean", "build");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":quarkusGenerateCode"))).isTrue();
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":compileKotlin"))).isTrue();
    }
}
