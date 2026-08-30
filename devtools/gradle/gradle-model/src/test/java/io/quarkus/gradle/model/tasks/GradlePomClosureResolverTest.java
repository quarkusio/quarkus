package io.quarkus.gradle.model.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.query.ArtifactResolutionQuery;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatchers;

import io.quarkus.maven.dependency.GAV;

class GradlePomClosureResolverTest {

    @TempDir
    Path tempDir;

    @Test
    @SuppressWarnings("unchecked")
    void shouldPrefetchPomsForMultipleModulesInOneQuery() throws Exception {
        Path firstPom = writePom("first");
        Path secondPom = writePom("second");
        var firstModule = moduleId("org.acme", "first", "1.0");
        var secondModule = moduleId("org.acme", "second", "1.0");
        ArtifactResolutionQuery query = mock(ArtifactResolutionQuery.class);
        AtomicReference<List<ModuleComponentIdentifier>> requestedModuleIds = new AtomicReference<>();
        when(query.forComponents(ArgumentMatchers.<Iterable<? extends ComponentIdentifier>> any()))
                .thenAnswer(invocation -> {
                    Iterable<?> requested = invocation.getArgument(0);
                    List<ModuleComponentIdentifier> snapshot = new ArrayList<>();
                    for (Object moduleId : requested) {
                        snapshot.add((ModuleComponentIdentifier) moduleId);
                    }
                    requestedModuleIds.set(snapshot);
                    return query;
                });
        when(query.withArtifacts(MavenModule.class, MavenPomArtifact.class)).thenReturn(query);
        Set<ComponentArtifactsResult> components = Set.of(
                component(firstModule, firstPom),
                component(secondModule, secondPom));
        ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
        when(result.getResolvedComponents()).thenReturn(components);
        when(query.execute()).thenReturn(result);
        DependencyHandler dependencies = mock(DependencyHandler.class);
        when(dependencies.createArtifactResolutionQuery()).thenReturn(query);
        Project project = mock(Project.class);
        when(project.getDependencies()).thenReturn(dependencies);
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(Collections.emptyMap(), project.getDependencies(), new ArrayList<>());

        resolver.prefetchPoms(Stream.of(firstModule, secondModule));

        assertThat(resolver.resolvePom(new GAV("org.acme", "first", "1.0")).getLocation())
                .isEqualTo(firstPom.toAbsolutePath().toString());
        assertThat(resolver.resolvePom(new GAV("org.acme", "second", "1.0")).getLocation())
                .isEqualTo(secondPom.toAbsolutePath().toString());
        assertThat(requestedModuleIds.get())
                .extracting(ModuleComponentIdentifier::getDisplayName)
                .containsExactlyInAnyOrder("org.acme:first:1.0", "org.acme:second:1.0");
        verify(query, never()).forModule(any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPrefetchPomsForMultipleGavsWithModuleQueries() throws Exception {
        Path firstPom = writePom("first");
        Path secondPom = writePom("second");
        var firstModule = moduleId("org.acme", "first", "1.0");
        var secondModule = moduleId("org.acme", "second", "1.0");
        ArtifactResolutionQuery query = mock(ArtifactResolutionQuery.class);
        when(query.forModule("org.acme", "first", "1.0")).thenReturn(query);
        when(query.forModule("org.acme", "second", "1.0")).thenReturn(query);
        when(query.withArtifacts(MavenModule.class, MavenPomArtifact.class)).thenReturn(query);
        ComponentArtifactsResult firstComponent = component(firstModule, firstPom);
        ComponentArtifactsResult secondComponent = component(secondModule, secondPom);
        ArtifactResolutionResult firstResult = mock(ArtifactResolutionResult.class);
        when(firstResult.getResolvedComponents()).thenReturn(Set.of(firstComponent));
        ArtifactResolutionResult secondResult = mock(ArtifactResolutionResult.class);
        when(secondResult.getResolvedComponents()).thenReturn(Set.of(secondComponent));
        when(query.execute()).thenReturn(firstResult, secondResult);
        DependencyHandler dependencies = mock(DependencyHandler.class);
        when(dependencies.createArtifactResolutionQuery()).thenReturn(query);
        Project project = mock(Project.class);
        when(project.getDependencies()).thenReturn(dependencies);
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(Collections.emptyMap(), project.getDependencies(), new ArrayList<>());

        resolver.prefetchPoms(List.of(
                new GAV("org.acme", "first", "1.0"),
                new GAV("org.acme", "second", "1.0")));

        assertThat(resolver.resolvePom(new GAV("org.acme", "first", "1.0")).getLocation())
                .isEqualTo(firstPom.toAbsolutePath().toString());
        assertThat(resolver.resolvePom(new GAV("org.acme", "second", "1.0")).getLocation())
                .isEqualTo(secondPom.toAbsolutePath().toString());
        verify(query, never()).forComponents(ArgumentMatchers.<Iterable<? extends ComponentIdentifier>> any());
        verify(query).forModule("org.acme", "first", "1.0");
        verify(query).forModule("org.acme", "second", "1.0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExposeMissingPomAsCachedBatchResult() throws Exception {
        ArtifactResolutionQuery query = mock(ArtifactResolutionQuery.class);
        when(query.forModule("org.acme", "missing", "1.0")).thenReturn(query);
        when(query.withArtifacts(MavenModule.class, MavenPomArtifact.class)).thenReturn(query);
        ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
        when(result.getResolvedComponents()).thenReturn(Set.of());
        when(query.execute()).thenReturn(result);
        DependencyHandler dependencies = mock(DependencyHandler.class);
        when(dependencies.createArtifactResolutionQuery()).thenReturn(query);
        Project project = mock(Project.class);
        when(project.getDependencies()).thenReturn(dependencies);
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(Collections.emptyMap(), project.getDependencies(), new ArrayList<>());
        GAV missing = new GAV("org.acme", "missing", "1.0");

        resolver.prefetchPoms(List.of(missing));

        assertThat(resolver.hasPomResult(missing)).isTrue();
        assertThatThrownBy(() -> resolver.resolvePom(missing))
                .isInstanceOf(UnresolvableModelException.class)
                .hasMessageContaining("Could not resolve POM for org.acme:missing:1.0");
        verify(query, never()).forComponents(ArgumentMatchers.<Iterable<? extends ComponentIdentifier>> any());
        verify(query, times(1)).forModule("org.acme", "missing", "1.0");
        verify(query, times(1)).execute();
    }

    @Test
    void shouldPrefetchGavPomFromGradleRepository() throws Exception {
        Path repository = tempDir.resolve("repo");
        installPom(repository, "org.acme", "sample", "1.0");
        Project project = ProjectBuilder.builder()
                .withProjectDir(tempDir.resolve("project").toFile())
                .build();
        project.getRepositories().maven(repo -> repo.setUrl(repository.toUri()));
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(Collections.emptyMap(), project.getDependencies(), new ArrayList<>());
        GAV sample = new GAV("org.acme", "sample", "1.0");

        resolver.prefetchPoms(List.of(sample));

        assertThat(resolver.hasPomResult(sample)).isTrue();
        assertThat(resolver.resolvePom(sample).getLocation().replace('\\', '/'))
                .endsWith("org/acme/sample/1.0/sample-1.0.pom");
    }

    @Test
    void shouldResolveKnownAndSiblingRepositoryPomsWithoutProjectServices() throws Exception {
        Path repository = tempDir.resolve("repo");
        Path samplePom = installPom(repository, "org.acme", "sample", "1.0");
        Path parentPom = installPom(repository, "org.acme", "parent", "1.0");
        GAV sample = new GAV("org.acme", "sample", "1.0");
        GAV parent = new GAV("org.acme", "parent", "1.0");
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(Map.of(sample, samplePom.toFile()), null, Collections.emptyList());

        resolver.prefetchPoms(List.of(sample, parent));

        assertThat(resolver.resolvePom(sample).getLocation()).isEqualTo(samplePom.toAbsolutePath().toString());
        assertThat(resolver.resolvePom(parent).getLocation()).isEqualTo(parentPom.toAbsolutePath().toString());
    }

    @Test
    void shouldFailClosedForAmbiguousGradleCachePoms() throws Exception {
        Path repository = tempDir.resolve("repo");
        installGradleCachePom(repository, "first-hash", "org.acme", "sample", "1.0");
        installGradleCachePom(repository, "second-hash", "org.acme", "sample", "1.0");
        GAV sample = new GAV("org.acme", "sample", "1.0");
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(Map.of(), null, List.of(repository.toFile()));

        resolver.prefetchPoms(List.of(sample));

        assertThatThrownBy(() -> resolver.resolvePom(sample))
                .isInstanceOf(UnresolvableModelException.class)
                .hasMessageContaining("Could not resolve POM for org.acme:sample:1.0");
    }

    @Test
    void shouldPreferModeledPomOverAmbiguousGradleCachePoms() throws Exception {
        Path repository = tempDir.resolve("repo");
        installGradleCachePom(repository, "first-hash", "org.acme", "sample", "1.0");
        installGradleCachePom(repository, "second-hash", "org.acme", "sample", "1.0");
        Path selectedPom = installPom(tempDir.resolve("selected-repository"), "org.acme", "sample", "1.0");
        GAV sample = new GAV("org.acme", "sample", "1.0");
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(
                        Map.of(sample, selectedPom.toFile()), null, List.of(repository.toFile()));

        resolver.prefetchPoms(List.of(sample));

        assertThat(resolver.resolvePom(sample).getLocation()).isEqualTo(selectedPom.toAbsolutePath().toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldResolvePomWithoutPrefetchAndCacheMisses() throws Exception {
        ArtifactResolutionQuery query = mock(ArtifactResolutionQuery.class);
        when(query.forModule("org.acme", "missing", "1.0")).thenReturn(query);
        when(query.withArtifacts(MavenModule.class, MavenPomArtifact.class)).thenReturn(query);
        ArtifactResolutionResult result = mock(ArtifactResolutionResult.class);
        when(result.getResolvedComponents()).thenReturn(Set.of());
        when(query.execute()).thenReturn(result);
        DependencyHandler dependencies = mock(DependencyHandler.class);
        when(dependencies.createArtifactResolutionQuery()).thenReturn(query);
        Project project = mock(Project.class);
        when(project.getDependencies()).thenReturn(dependencies);
        GradlePomClosureResolver resolver = GradlePomClosureResolver
                .withGradleArtifactResolution(Collections.emptyMap(), project.getDependencies(), new ArrayList<>());
        GAV missing = new GAV("org.acme", "missing", "1.0");

        assertThatThrownBy(() -> resolver.resolvePom(missing))
                .isInstanceOf(UnresolvableModelException.class)
                .hasMessageContaining("Could not resolve POM for org.acme:missing:1.0");
        assertThatThrownBy(() -> resolver.resolvePom(missing))
                .isInstanceOf(UnresolvableModelException.class)
                .hasMessageContaining("Could not resolve POM for org.acme:missing:1.0");

        verify(dependencies, times(1)).createArtifactResolutionQuery();
        verify(query, times(1)).execute();
    }

    private Path writePom(String name) throws IOException {
        Path pom = tempDir.resolve(name + ".pom");
        Files.writeString(pom, "<project/>", StandardCharsets.UTF_8);
        return pom;
    }

    private static Path installPom(Path repository, String groupId, String artifactId, String version) throws IOException {
        Path artifactDirectory = repository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
        Files.createDirectories(artifactDirectory);
        Path pom = artifactDirectory.resolve(artifactId + "-" + version + ".pom");
        Files.writeString(pom,
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>%s</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                        </project>
                        """.formatted(groupId, artifactId, version),
                StandardCharsets.UTF_8);
        return pom;
    }

    private static void installGradleCachePom(Path repository, String hash, String groupId, String artifactId,
            String version) throws IOException {
        Path artifactDirectory = repository.resolve(groupId).resolve(artifactId).resolve(version).resolve(hash);
        Files.createDirectories(artifactDirectory);
        Files.writeString(artifactDirectory.resolve(artifactId + "-" + version + ".pom"),
                """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>%s</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                          <description>%s</description>
                        </project>
                        """.formatted(groupId, artifactId, version, hash),
                StandardCharsets.UTF_8);
    }

    private static ModuleComponentIdentifier moduleId(String groupId, String artifactId, String version) {
        var moduleId = mock(ModuleComponentIdentifier.class);
        when(moduleId.getGroup()).thenReturn(groupId);
        when(moduleId.getModule()).thenReturn(artifactId);
        when(moduleId.getVersion()).thenReturn(version);
        when(moduleId.getDisplayName()).thenReturn(groupId + ":" + artifactId + ":" + version);
        return moduleId;
    }

    private static ComponentArtifactsResult component(
            ModuleComponentIdentifier moduleId, Path pomFile) {
        ResolvedArtifactResult artifact = mock(ResolvedArtifactResult.class);
        when(artifact.getFile()).thenReturn(pomFile.toFile());
        ComponentArtifactsResult component = mock(ComponentArtifactsResult.class);
        when(component.getId()).thenReturn(moduleId);
        when(component.getArtifacts(MavenPomArtifact.class)).thenReturn(Set.of(artifact));
        return component;
    }
}
