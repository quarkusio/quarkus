package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class BeanInTestSourcesTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {
        final File projectDir = getProjectDir("bean-in-testsources-project");
        final BuildResult build = runGradleWrapper(projectDir, "clean", "test");
        assertThat(build.getTasks().get(":test")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }
}
