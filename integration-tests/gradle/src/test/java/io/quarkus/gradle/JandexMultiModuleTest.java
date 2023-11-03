package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

public class JandexMultiModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuildKordamp() throws Exception {
        // Kordamp Jandex plugin's not compatible w/ the Gradle configuration cache
        gradleConfigurationCache(false);
        jandexTest("jandex-basic-multi-module-project-kordamp", ":common:jandex");
    }

    @Test
    public void testBasicMultiModuleBuildJandex() throws Exception {
        gradleConfigurationCache(true);
        jandexTest("jandex-basic-multi-module-project-vlsi", ":common:processJandexIndex");
    }

    private void jandexTest(String projectName, String taskName) throws Exception {
        File projectDir = getProjectDir(projectName);

        BuildResult build = runGradleWrapper(projectDir, "clean", ":application:quarkusBuild");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(taskName))).isTrue();
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":application:quarkusBuild"))).isTrue();
    }

}
