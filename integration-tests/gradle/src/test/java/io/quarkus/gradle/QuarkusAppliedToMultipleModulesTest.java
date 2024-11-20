package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class QuarkusAppliedToMultipleModulesTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {
        final File projectDir = getProjectDir("quarkus-plugin-in-multiple-modules");
        final BuildResult build = runGradleWrapper(projectDir, "clean", "quarkusBuild");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":modA:quarkusBuild"))).isTrue();
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":modB:quarkusBuild"))).isTrue();
    }
}
