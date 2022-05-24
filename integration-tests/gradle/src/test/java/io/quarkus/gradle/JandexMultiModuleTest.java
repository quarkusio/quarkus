package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class JandexMultiModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("jandex-basic-multi-module-project");

        BuildResult build = runGradleWrapper(projectDir, "clean", ":application:quarkusBuild");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":common:jandex"))).isTrue();
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":application:quarkusBuild"))).isTrue();
    }

}
