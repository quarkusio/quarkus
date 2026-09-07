package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class GrpcCustomProtoDirectoryBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testGrpcServerWithCustomProtoDirectory() throws Exception {
        final File projectDir = getProjectDir("grpc-custom-proto-directory");

        final BuildResult buildResult = runGradleWrapper(projectDir, "clean", "test");

        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":test"))).isTrue();
        assertThat(buildResult.getOutput()).contains("grpc-server");

        final Path testGeneratedBean = projectDir.toPath()
                .resolve("build/classes/java/test/org/acme/protocol/GameServiceBean.class");
        assertThat(testGeneratedBean).doesNotExist();
    }
}
