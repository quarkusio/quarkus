package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class GrpcDescriptorSetAlternateOutputDirBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testGrpcDescriptorSetAlternateOutputDir() throws Exception {
        var projectDir = getProjectDir("grpc-descriptor-set-alternate-output-dir");
        var buildResult = runGradleWrapper(projectDir, "clean", "build");
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":quarkusGenerateCode"))).isTrue();

        var expectedOutputDir = projectDir.toPath().resolve("build").resolve("proto");

        assertThat(expectedOutputDir).exists();
        assertThat(expectedOutputDir.resolve("hello.dsc")).exists().isNotEmptyFile();
    }
}
