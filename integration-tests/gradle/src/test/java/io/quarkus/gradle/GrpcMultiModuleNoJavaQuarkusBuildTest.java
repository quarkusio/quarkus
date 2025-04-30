package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class GrpcMultiModuleNoJavaQuarkusBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testGrpcMultiModuleBuild() throws Exception {

        final File projectDir = getProjectDir("grpc-multi-module-no-java");

        final BuildResult build = runGradleWrapper(projectDir, "clean", "build");
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":application:quarkusBuild"))).isTrue();
        assertThat(BuildResult.isSuccessful(build.getTasks().get(":application:quarkusAppPartsBuild"))).isTrue();

        final Path applicationLib = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus-app")
                .resolve("lib").resolve("main");
        assertThat(applicationLib).exists();
        assertThat(applicationLib.resolve("org.acme.module1-1.0.0-SNAPSHOT.jar")).exists();
    }

}
