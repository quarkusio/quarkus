package io.quarkus.gradle;

import java.io.File;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class BasicMultiModuleQuarkusBuildTest extends QuarkusGradleTestBase {

    @Test
    public void testBasicMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("basic-multi-module-project");
        
        BuildResult build = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments(arguments(":application:quarkusBuild"))
                .withProjectDir(projectDir)
                .build();
        
        final Path commonLibs = projectDir.toPath().resolve("common").resolve("build").resolve("libs");
        assertThat(commonLibs).exists();
        assertThat(commonLibs.resolve("common.jar")).exists();
        
        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("lib");
        assertThat(applicationLib).exists();
        assertThat(applicationLib.resolve("quarkus-basic-multi-module-build.common.jar")).exists();
    }
}
