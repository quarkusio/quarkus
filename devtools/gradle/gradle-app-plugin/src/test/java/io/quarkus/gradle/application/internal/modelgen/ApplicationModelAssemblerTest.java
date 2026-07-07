package io.quarkus.gradle.application.internal.modelgen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ArtifactSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ClasspathSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ComponentKey;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ComponentSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.LocalOutputKey;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.LocalOutputSnapshot;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.ModuleCoordinates;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelInputs.WorkspaceSnapshot;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

class ApplicationModelAssemblerTest {

    private static final ModuleCoordinates APPLICATION = new ModuleCoordinates("org.acme", "application", "1");

    @TempDir
    Path projectDirectory;

    @Test
    void assemblesWorkspaceDependencyScopesAndArtifactlessHandoffs() throws IOException {
        Path classes = Files.createDirectories(projectDirectory.resolve("build/classes/java/main"));
        Path resources = Files.createDirectories(projectDirectory.resolve("build/resources/main"));
        Path dependencyClasses = Files.createDirectories(projectDirectory.resolve("dependency/build/classes/java/main"));

        ComponentKey runtime = new ComponentKey(1);
        ComponentKey transitive = new ComponentKey(2);
        ComponentKey handoff = new ComponentKey(3);
        ComponentKey deployment = new ComponentKey(4);
        ComponentKey compileOnly = new ComponentKey(5);
        Path runtimeJar = projectDirectory.resolve("lib-1.jar");
        Path transitiveJar = projectDirectory.resolve("transitive-1.jar");
        Path deploymentJar = projectDirectory.resolve("lib-deployment-1.jar");
        Path compileOnlyJar = projectDirectory.resolve("compile-only-1.jar");
        Path fileDependency = Files.createDirectories(projectDirectory.resolve("file-dependency"));

        ClasspathSnapshot applicationClasspath = classpath(
                List.of(runtime),
                Map.of(
                        runtime, component(runtime, "lib", List.of(transitive), runtimeJar),
                        transitive, component(transitive, "transitive", List.of(), transitiveJar)),
                List.of(runtimeJar, transitiveJar, fileDependency));
        ClasspathSnapshot deploymentClasspath = classpath(
                List.of(handoff),
                Map.of(
                        handoff, component(handoff, "handoff", List.of(deployment)),
                        deployment, component(deployment, "lib-deployment", List.of(), deploymentJar)),
                List.of(deploymentJar));
        ClasspathSnapshot compileOnlyClasspath = classpath(
                List.of(compileOnly),
                Map.of(compileOnly, component(compileOnly, "compile-only", List.of(), compileOnlyJar)),
                List.of(compileOnlyJar));
        LocalOutputSnapshot localOutputs = new LocalOutputSnapshot(
                Map.of(new LocalOutputKey(runtime, ArtifactCoords.DEFAULT_CLASSIFIER), List.of(dependencyClasses)));

        ApplicationModel model = new ApplicationModelAssembler()
                .assemble(inputs(classes, resources, applicationClasspath, deploymentClasspath, compileOnlyClasspath,
                        localOutputs, true))
                .build();

        ResolvedDependency runtimeDependency = dependency(model, "lib");
        assertThat(runtimeDependency.isDirect()).isTrue();
        assertThat(runtimeDependency.isRuntimeCp()).isTrue();
        assertThat(runtimeDependency.isDeploymentCp()).isTrue();
        assertThat(runtimeDependency.isReloadable()).isTrue();
        assertThat(runtimeDependency.getResolvedPaths()).containsExactly(dependencyClasses);

        ResolvedDependency transitiveDependency = dependency(model, "transitive");
        assertThat(transitiveDependency.isDirect()).isFalse();
        assertThat(transitiveDependency.isRuntimeCp()).isTrue();
        assertThat(transitiveDependency.isDeploymentCp()).isTrue();

        ResolvedDependency deploymentDependency = dependency(model, "lib-deployment");
        assertThat(deploymentDependency.isRuntimeCp()).isFalse();
        assertThat(deploymentDependency.isDeploymentCp()).isTrue();

        ResolvedDependency compileOnlyDependency = dependency(model, "compile-only");
        assertThat(compileOnlyDependency.isFlagSet(DependencyFlags.COMPILE_ONLY)).isTrue();
        assertThat(compileOnlyDependency.isRuntimeCp()).isFalse();

        assertThat(dependency(model, "file-dependency").getResolvedPaths()).containsExactly(fileDependency);
        assertThat(model.getReloadableWorkspaceDependencies()).contains(runtimeDependency.getKey());
        assertThat(model.getAppArtifact().getResolvedPaths()).containsExactly(classes, resources);
        assertThat(model.getApplicationModule().getMainSources().getSourceDirs())
                .extracting(sourceDir -> sourceDir.getDir())
                .containsExactly(projectDirectory.resolve("src/main/java"));
        assertThat(model.getApplicationModule().getMainSources().getResourceDirs())
                .extracting(sourceDir -> sourceDir.getDir())
                .containsExactly(projectDirectory.resolve("src/main/resources"));
    }

    @Test
    void workspaceDependencyReloadabilityIsAnExplicitPolicy() throws IOException {
        Path classes = Files.createDirectories(projectDirectory.resolve("build/classes/java/main"));
        Path dependencyClasses = Files.createDirectories(projectDirectory.resolve("dependency/build/classes/java/main"));
        ComponentKey runtime = new ComponentKey(1);
        Path runtimeJar = projectDirectory.resolve("lib-1.jar");
        ClasspathSnapshot applicationClasspath = classpath(
                List.of(runtime),
                Map.of(runtime, component(runtime, "lib", List.of(), runtimeJar)),
                List.of(runtimeJar));
        LocalOutputSnapshot localOutputs = new LocalOutputSnapshot(
                Map.of(new LocalOutputKey(runtime, ArtifactCoords.DEFAULT_CLASSIFIER), List.of(dependencyClasses)));

        ApplicationModel model = new ApplicationModelAssembler()
                .assemble(inputs(classes, projectDirectory.resolve("missing-resources"), applicationClasspath,
                        emptyClasspath(), emptyClasspath(), localOutputs, false))
                .build();

        ResolvedDependency dependency = dependency(model, "lib");
        assertThat(dependency.getWorkspaceModule()).isNotNull();
        assertThat(dependency.isReloadable()).isFalse();
        assertThat(model.getReloadableWorkspaceDependencies()).doesNotContain(dependency.getKey());
    }

    @Test
    void directDependencySemanticsDoNotDependOnFirstTraversalPath() throws IOException {
        Path classes = Files.createDirectories(projectDirectory.resolve("build/classes/java/main"));
        Path firstClasses = Files.createDirectories(projectDirectory.resolve("first/build/classes/java/main"));
        Path directClasses = Files.createDirectories(projectDirectory.resolve("direct/build/classes/java/main"));
        ComponentKey first = new ComponentKey(1);
        ComponentKey directAndTransitive = new ComponentKey(2);
        Path firstJar = projectDirectory.resolve("first-1.jar");
        Path directJar = projectDirectory.resolve("direct-1.jar");
        Map<ComponentKey, ComponentSnapshot> components = Map.of(
                first, component(first, "first", List.of(directAndTransitive), firstJar),
                directAndTransitive, component(directAndTransitive, "direct", List.of(), directJar));
        LocalOutputSnapshot localOutputs = new LocalOutputSnapshot(
                Map.of(
                        new LocalOutputKey(first, ArtifactCoords.DEFAULT_CLASSIFIER), List.of(firstClasses),
                        new LocalOutputKey(directAndTransitive, ArtifactCoords.DEFAULT_CLASSIFIER), List.of(directClasses)));

        for (List<ComponentKey> roots : List.of(
                List.of(first, directAndTransitive),
                List.of(directAndTransitive, first))) {
            ClasspathSnapshot applicationClasspath = classpath(
                    roots, components, List.of(firstJar, directJar));

            ApplicationModel model = new ApplicationModelAssembler()
                    .assemble(inputs(classes, projectDirectory.resolve("missing-resources"), applicationClasspath,
                            emptyClasspath(), emptyClasspath(), localOutputs, true))
                    .build();

            ResolvedDependency direct = dependency(model, "direct");
            assertThat(direct.isDirect()).isTrue();
            assertThat(direct.isReloadable()).isTrue();
            assertThat(model.getReloadableWorkspaceDependencies()).contains(direct.getKey());
            assertThat(model.getApplicationModule().getDirectDependencies())
                    .extracting(dependency -> dependency.getArtifactId())
                    .contains("direct");
            assertThat(dependency(model, "first").getWorkspaceModule().getDirectDependencies())
                    .extracting(dependency -> dependency.getArtifactId())
                    .containsExactly("direct");
        }
    }

    @Test
    void snapshotsDefensivelyCopyCollections() {
        ComponentKey componentKey = new ComponentKey(1);
        List<ComponentKey> rootDependencies = new ArrayList<>(List.of(componentKey));
        Map<ComponentKey, ComponentSnapshot> components = new LinkedHashMap<>();
        components.put(componentKey, component(componentKey, "lib", List.of(), projectDirectory.resolve("lib-1.jar")));
        ClasspathSnapshot snapshot = classpath(rootDependencies, components, List.of());

        rootDependencies.clear();
        components.clear();

        assertThat(snapshot.rootDependencies()).containsExactly(componentKey);
        assertThat(snapshot.components()).containsOnlyKeys(componentKey);
    }

    private ApplicationModelInputs inputs(Path classes, Path resources,
            ClasspathSnapshot applicationClasspath,
            ClasspathSnapshot deploymentClasspath,
            ClasspathSnapshot compileOnlyClasspath,
            LocalOutputSnapshot localOutputs,
            boolean reloadableWorkspaceDependencies) {
        WorkspaceSnapshot workspace = new WorkspaceSnapshot(
                projectDirectory,
                projectDirectory.resolve("build"),
                projectDirectory.resolve("build.gradle"),
                List.of(classes),
                List.of(resources),
                List.of(projectDirectory.resolve("src/main/java")),
                List.of(projectDirectory.resolve("src/main/resources")),
                List.of(),
                List.of(),
                List.of(),
                List.of());
        return new ApplicationModelInputs(workspace, applicationClasspath, deploymentClasspath, compileOnlyClasspath,
                List.of(), localOutputs, reloadableWorkspaceDependencies);
    }

    private static ClasspathSnapshot emptyClasspath() {
        return classpath(List.of(), Map.of(), List.of());
    }

    private static ClasspathSnapshot classpath(
            List<ComponentKey> roots,
            Map<ComponentKey, ComponentSnapshot> components,
            List<Path> files) {
        return new ClasspathSnapshot(APPLICATION, roots, components, files);
    }

    private ComponentSnapshot component(ComponentKey key, String artifactId, List<ComponentKey> dependencies,
            Path... artifacts) {
        return new ComponentSnapshot(
                key,
                new ModuleCoordinates("org.acme", artifactId, "1"),
                dependencies,
                List.of(artifacts).stream().map(path -> new ArtifactSnapshot(path, ArtifactCoords.TYPE_JAR)).toList());
    }

    private static ResolvedDependency dependency(ApplicationModel model, String artifactId) {
        for (ResolvedDependency dependency : model
                .getDependenciesWithAnyFlag(DependencyFlags.DEPLOYMENT_CP | DependencyFlags.COMPILE_ONLY)) {
            if (dependency.getArtifactId().equals(artifactId)) {
                return dependency;
            }
        }
        throw new IllegalArgumentException("No dependency with artifact ID " + artifactId);
    }
}
