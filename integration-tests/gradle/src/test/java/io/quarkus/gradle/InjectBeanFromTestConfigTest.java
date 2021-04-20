package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class InjectBeanFromTestConfigTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("inject-bean-from-test-config");

        BuildResult build = runGradleWrapper(projectDir, "clean", ":application:test");
        assertThat(build.getTasks().get(":application:test")).isEqualTo(BuildResult.SUCCESS_OUTCOME);
    }
}
