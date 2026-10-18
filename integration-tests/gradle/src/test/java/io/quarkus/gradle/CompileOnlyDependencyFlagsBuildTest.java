package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;

/**
 * Tests that compile-only dependencies are properly flagged in the ApplicationModel
 * produced by {@code QuarkusApplicationModelTask} (the task-based path used by {@code quarkusBuild}).
 * <p>
 * This complements {@link CompileOnlyDependencyFlagsTest} which tests the tooling model builder path.
 */
public class CompileOnlyDependencyFlagsBuildTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void compileOnlyFlagsInProdBuild() throws Exception {
        final File projectDir = getProjectDir("compile-only-dependency-flags-build");

        runGradleWrapper(projectDir, "clean", "quarkusBuild");

        final Path modelDat = projectDir.toPath().resolve("build").resolve("quarkus")
                .resolve("application-model").resolve("quarkus-app-model.dat");
        assertThat(modelDat).exists();

        final ApplicationModel model = ApplicationModelSerializer.deserialize(modelDat);
        final Map<ArtifactKey, String> compileOnlyDeps = new HashMap<>();
        for (var dep : model.getDependencies(DependencyFlags.COMPILE_ONLY)) {
            compileOnlyDeps.put(dep.getKey(), DependencyFlags.toNames(dep.getFlags()));
        }

        assertThat(compileOnlyDeps)
                .as("Compile-only dependencies should be present in the prod ApplicationModel built by QuarkusApplicationModelTask")
                .isNotEmpty();
        assertThat(compileOnlyDeps).containsKey(ArtifactKey.fromString("io.quarkus:quarkus-bootstrap-core::jar"));
    }
}
