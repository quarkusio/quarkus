package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Forced dependencies for gradle still don't work")
public class ImageBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shouldRunIntegrationTestAsPartOfBuild() throws Exception {
        File projectDir = getProjectDir("it-test-basic-project");
        BuildResult buildResult = runGradleWrapper(projectDir, "imageBuild", "--builder=jib");
        assertThat(BuildResult.isSuccessful(buildResult.getTasks().get(":imageBuild"))).isTrue();

        final File buildDir = new File(projectDir, "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        boolean hasContainerImageExtension = Arrays.stream(mainLib.toFile().list())
                .filter(l -> l.startsWith("io.quarkus")).anyMatch(l -> l.contains("container-image-jib"));
        assertTrue(hasContainerImageExtension);
    }
}
