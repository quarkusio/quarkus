package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class GrpcDescriptorSetBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testGrpcDescriptorSet() throws Exception {
        var projectDir = getProjectDir("grpc-descriptor-set");
        var buildResult = runGradleWrapper(projectDir, "clean", "build");
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":quarkusGenerateCode"))).isTrue();

        var expectedOutputDir = projectDir.toPath().resolve("build").resolve("classes").resolve("java")
                .resolve("quarkus-generated-sources").resolve("grpc");

        assertThat(expectedOutputDir).exists();
        assertThat(expectedOutputDir.resolve("descriptor_set.dsc")).exists().isNotEmptyFile();
    }
}
