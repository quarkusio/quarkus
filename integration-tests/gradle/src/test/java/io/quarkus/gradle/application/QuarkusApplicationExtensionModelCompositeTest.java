package io.quarkus.gradle.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.BuildResult;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

class QuarkusApplicationExtensionModelCompositeTest extends QuarkusApplicationGradleTestBase {

    private static final String GROUP = "org.acme.model";

    @Test
    void generatedTestModelSelectsDistinctCompositeAndClassifierOutputs() throws Exception {
        File projectDir = getProjectDir("application-plugin/extension-model-composite");

        BuildResult result = runApplicationGradleWrapper(projectDir, "clean",
                ":main-extension-deployment:quarkusGenerateTestAppModel");

        assertThat(result.unsuccessfulTasks()).isEmpty();
        assertThat(result.getTasks()).containsKey(":main-extension-deployment:quarkusGenerateTestAppModel");
        assertThat(result.getTasks()).containsKey(":helper:processJandexIndex");

        Path modelPath = projectDir.toPath()
                .resolve("deployment/build/quarkus/application-model/quarkus-app-test-model.dat");
        assertThat(modelPath).isRegularFile();
        ApplicationModel model = ApplicationModelSerializer.deserialize(modelPath);
        assertThat(model.getAppArtifact().getKey()).isEqualTo(key("main-extension-deployment"));

        Map<ArtifactKey, ResolvedDependency> dependencies = model.getDependencies().stream()
                .filter(dependency -> GROUP.equals(dependency.getGroupId()))
                .collect(Collectors.toMap(ResolvedDependency::getKey, Function.identity()));
        ArtifactKey mainRuntimeKey = key("main-extension");
        ArtifactKey mainFixturesKey = key("main-extension", "test-fixtures");
        ArtifactKey helperKey = key("helper");
        ArtifactKey helperFixturesKey = key("helper", "test-fixtures");
        ArtifactKey supportRuntimeKey = key("support-extension");
        ArtifactKey supportDeploymentKey = key("support-extension-deployment");
        assertThat(dependencies).containsOnlyKeys(
                mainRuntimeKey,
                mainFixturesKey,
                helperKey,
                helperFixturesKey,
                supportRuntimeKey,
                supportDeploymentKey);

        assertClasspathWorkspaceFlags(dependencies.get(mainRuntimeKey), true, true, false);
        assertClasspathWorkspaceFlags(dependencies.get(mainFixturesKey), true, true, false);
        assertClasspathWorkspaceFlags(dependencies.get(helperKey), true, true, false);
        assertClasspathWorkspaceFlags(dependencies.get(helperFixturesKey), true, true, false);
        assertClasspathWorkspaceFlags(dependencies.get(supportRuntimeKey), true, true, false);
        assertClasspathWorkspaceFlags(dependencies.get(supportDeploymentKey), true, true, true);
        assertFlag(dependencies.get(mainRuntimeKey), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT, true);
        assertFlag(dependencies.get(supportRuntimeKey), DependencyFlags.RUNTIME_EXTENSION_ARTIFACT, true);

        assertPathEndsWith(dependencies.get(mainRuntimeKey), "runtime/build/classes/java/main");
        assertPathEndsWith(dependencies.get(mainFixturesKey), "runtime/build/classes/java/testFixtures");
        assertPathDoesNotEndWith(dependencies.get(mainFixturesKey), "runtime/build/classes/java/main");
        assertPathEndsWith(dependencies.get(helperKey), "helper/build/classes/java/main");
        assertPathEndsWith(dependencies.get(helperFixturesKey), "helper/build/classes/java/testFixtures");
        assertPathDoesNotEndWith(dependencies.get(helperFixturesKey), "helper/build/classes/java/main");
        assertPathEndsWith(dependencies.get(supportRuntimeKey),
                "support-extension/runtime/build/classes/java/main");
        assertPathEndsWith(dependencies.get(supportDeploymentKey),
                "support-extension/deployment/build/classes/java/main");
    }

    private static ArtifactKey key(String artifactId) {
        return key(artifactId, "");
    }

    private static ArtifactKey key(String artifactId, String classifier) {
        return ArtifactKey.of(GROUP, artifactId, classifier, "jar");
    }

    private static void assertClasspathWorkspaceFlags(ResolvedDependency dependency, boolean runtime,
            boolean deployment, boolean reloadable) {
        assertThat(dependency).isNotNull();
        assertFlag(dependency, DependencyFlags.RUNTIME_CP, runtime);
        assertFlag(dependency, DependencyFlags.DEPLOYMENT_CP, deployment);
        assertFlag(dependency, DependencyFlags.WORKSPACE_MODULE, true);
        assertFlag(dependency, DependencyFlags.RELOADABLE, reloadable);
    }

    private static void assertFlag(ResolvedDependency dependency, int flag, boolean expected) {
        assertThat((dependency.getFlags() & flag) != 0)
                .as("%s on %s; actual flags: %s", flag, dependency.getKey(),
                        DependencyFlags.toNames(dependency.getFlags()))
                .isEqualTo(expected);
    }

    private static void assertPathEndsWith(ResolvedDependency dependency, String expected) {
        assertThat(normalizedPaths(dependency)).anyMatch(path -> path.endsWith(expected));
    }

    private static void assertPathDoesNotEndWith(ResolvedDependency dependency, String unexpected) {
        assertThat(normalizedPaths(dependency)).noneMatch(path -> path.endsWith(unexpected));
    }

    private static List<String> normalizedPaths(ResolvedDependency dependency) {
        return dependency.getResolvedPaths().stream()
                .map(path -> path.toString().replace('\\', '/'))
                .toList();
    }
}
