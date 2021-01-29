package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class GrpcMultiModuleQuarkusBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testGrpcMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("grpc-multi-module-project");

        runGradleWrapper(projectDir, ":application:quarkusBuild", ":application:test");

        final Path commonLibs = projectDir.toPath().resolve("common").resolve("build").resolve("libs");
        assertThat(commonLibs).exists();
        assertThat(commonLibs.resolve("common.jar")).exists();

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app")
                .resolve("lib").resolve("main");
        assertThat(applicationLib).exists();
        assertThat(applicationLib.resolve("quarkus-grpc-multi-module-build.common.jar")).exists();
    }

    @Test
    public void testProtocErrorOutput() throws Exception {
        final File projectDir = getProjectDir("grpc-multi-module-project");

        final Path protoDirectory = new File(projectDir, "application/src/main/proto/").toPath();
        Files.copy(projectDir.toPath().resolve("invalid.proto"), protoDirectory.resolve("invalid.proto"));
        try {
            final BuildResult buildResult = runGradleWrapper(projectDir, ":application:quarkusBuild", "--info");
            assertTrue(buildResult.getOutput().contains("invalid.proto:5:1: Missing field number."));
        } finally {
            Files.delete(protoDirectory.resolve("invalid.proto"));
        }

    }
}
