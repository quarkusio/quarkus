package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class GrpcMultiModuleQuarkusBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testGrpcMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("grpc-multi-module-project");

        runGradleWrapper(projectDir, ":application:quarkusBuild", ":application:test", "--stacktrace", "--info");

        final Path commonLibs = projectDir.toPath().resolve("common").resolve("build").resolve("libs");
        assertThat(commonLibs).exists();
        assertThat(commonLibs.resolve("common.jar")).exists();

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("lib");
        assertThat(applicationLib).exists();
        assertThat(applicationLib.resolve("quarkus-grpc-multi-module-build.common.jar")).exists();
    }
}
