package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class DependencyConstraintsTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void shoudBuildProjectWithDependencyConstraint() throws Exception {
        File projectDir = getProjectDir("dependency-constraints-project");

        BuildResult buildResult = runGradleWrapper(projectDir, "clean", "quarkusBuild", "-Dquarkus.package.type=mutable-jar");

        assertThat(buildResult.getTasks().get(":quarkusBuild")).isEqualTo(BuildResult.SUCCESS_OUTCOME);

        final File buildDir = new File(projectDir, "build");
        final Path mainLib = buildDir.toPath().resolve("quarkus-app").resolve("lib").resolve("main");

        assertThat(mainLib.resolve("javax.json.bind.javax.json.bind-api-1.0.jar")).exists();
    }

}
