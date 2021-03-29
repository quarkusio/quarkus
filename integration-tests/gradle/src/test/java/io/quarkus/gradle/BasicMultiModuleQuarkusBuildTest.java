package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class BasicMultiModuleQuarkusBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("basic-multi-module-project");

        runGradleWrapper(projectDir, ":application:quarkusBuild");

        final Path commonLibs = projectDir.toPath().resolve("common").resolve("build").resolve("libs");
        assertThat(commonLibs).exists();
        assertThat(commonLibs.resolve("common.jar")).exists();

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app")
                .resolve("lib").resolve("main");
        assertThat(applicationLib).exists();
        assertThat(applicationLib.resolve("quarkus-basic-multi-module-build.common.jar")).exists();
    }
}
