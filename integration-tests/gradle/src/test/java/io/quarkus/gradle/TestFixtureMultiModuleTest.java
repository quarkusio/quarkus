package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
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
        final ApplicationModel model = ApplicationModelSerializer.deserialize(testModelDat);
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
}
