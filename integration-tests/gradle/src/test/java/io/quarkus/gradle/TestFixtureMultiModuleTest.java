package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.util.BootstrapUtils;
import io.quarkus.maven.dependency.ArtifactKey;

public class TestFixtureMultiModuleTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testTaskShouldUseTestFixtures() throws Exception {
        final File projectDir = getProjectDir("test-fixtures-multi-module");
        final BuildResult result = runGradleWrapper(projectDir, "clean", "test");
        assertThat(BuildResult.isSuccessful(result.getTasks().get(":application:test"))).isTrue();

        final Path testModelDat = projectDir.toPath().resolve("application").resolve("build").resolve("quarkus")
                .resolve("application-model").resolve("quarkus-app-test-model.dat");
        assertThat(testModelDat).exists();
        final ApplicationModel model = deserializeAppModel(testModelDat);
        final Map<ArtifactKey, String> actualDepFlags = new HashMap<>();
        for (var dep : model.getDependencies()) {
            if (dep.getGroupId().equals("my-groupId")) {
                actualDepFlags.put(dep.getKey(), BootstrapUtils.toTextFlags(dep.getFlags()));
            }
        }
        assertThat(actualDepFlags).containsExactlyInAnyOrderEntriesOf(Map.of(
                ArtifactKey.fromString("my-groupId:library-1::jar"),
                "runtime-cp, deployment-cp, workspace-module, reloadable",
                ArtifactKey.fromString("my-groupId:library-1:test-fixtures:jar"),
                "runtime-cp, deployment-cp, workspace-module, reloadable",
                ArtifactKey.fromString("my-groupId:library-2::jar"),
                "direct, runtime-cp, deployment-cp, workspace-module, reloadable",
                ArtifactKey.fromString("my-groupId:library-2:test-fixtures:jar"),
                "direct, runtime-cp, deployment-cp, workspace-module, reloadable",
                ArtifactKey.fromString("my-groupId:static-init-library::jar"),
                "runtime-cp, deployment-cp, workspace-module, reloadable"));
    }

    /**
     * Copied from ToolingUtils
     *
     * @param path application model dat file
     * @return deserialized ApplicationModel
     * @throws IOException in case of a failure to read the model
     */
    private static ApplicationModel deserializeAppModel(Path path) throws IOException {
        try (ObjectInputStream out = new ObjectInputStream(Files.newInputStream(path))) {
            return (ApplicationModel) out.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
