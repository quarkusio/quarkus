package io.quarkus.gradle.model.pom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;

import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.gradle.model.tasks.GradlePomClosureResolver;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GAV;

class StrictDependencyDataCollectorTest {

    // Gradle dependency resolution can keep files from the synthetic Maven repository locked on Windows.
    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempDir;

    @Test
    void shouldCollectDependenciesFromEffectiveMavenModel() throws IOException {
        Path repository = tempDir.resolve("repo");
        installPom(repository, "org.acme", "parent", "1.0", pom("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.acme</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0</version>
                  <packaging>pom</packaging>
                  <properties>
                    <optional.version>3.0</optional.version>
                  </properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.acme</groupId>
                        <artifactId>managed-lib</artifactId>
                        <version>2.0</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """));
        installPom(repository, "org.acme", "acme-bom", "1.0", pom("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.acme</groupId>
                  <artifactId>acme-bom</artifactId>
                  <version>1.0</version>
                  <packaging>pom</packaging>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.acme</groupId>
                        <artifactId>bom-managed-lib</artifactId>
                        <version>4.0</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """));
        installJar(repository, "org.acme", "app-lib", "1.0", pom("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.acme</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                  </parent>
                  <artifactId>app-lib</artifactId>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.acme</groupId>
                        <artifactId>acme-bom</artifactId>
                        <version>1.0</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.acme</groupId>
                      <artifactId>managed-lib</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.acme</groupId>
                      <artifactId>bom-managed-lib</artifactId>
                    </dependency>
                    <dependency>
                      <groupId>org.acme</groupId>
                      <artifactId>optional-lib</artifactId>
                      <version>${optional.version}</version>
                      <scope>runtime</scope>
                      <optional>true</optional>
                    </dependency>
                    <dependency>
                      <groupId>org.acme</groupId>
                      <artifactId>test-lib</artifactId>
                      <version>5.0</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """));
        installJar(repository, "org.acme", "managed-lib", "2.0", simplePom("org.acme", "managed-lib", "2.0"));
        installJar(repository, "org.acme", "bom-managed-lib", "4.0", simplePom("org.acme", "bom-managed-lib", "4.0"));

        Project project = createProject(repository);
        project.getDependencies().add("implementation", "org.acme:app-lib:1.0");

        Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collectDeclaredDependencies(project, "runtimeClasspath");

        assertThat(declaredDependencies)
                .containsKey(ArtifactKey.of("org.acme", "app-lib", "", "jar"));
        assertThat(declaredDependencies.get(ArtifactKey.of("org.acme", "app-lib", "", "jar"))
                .getDeclaredDependencies())
                .extracting(StrictDependencyDataCollectorTest::dependencySnapshot)
                .containsExactlyInAnyOrder(
                        "org.acme:managed-lib:2.0::jar:compile:false",
                        "org.acme:bom-managed-lib:4.0::jar:compile:false",
                        "org.acme:optional-lib:3.0::jar:runtime:true");
    }

    @Test
    void shouldCollectExternalDeclaredDependenciesFromModeledInputs() {
        StrictDependencyDataCollector collector = new StrictDependencyDataCollector(
                new InMemoryPomResolver(effectiveModelPoms()),
                () -> Map.of("external.version", "5.0"));

        Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(new ExternalModuleDeclaredDependencyInput(
                        ArtifactKey.of("org.acme", "app", "", "jar"),
                        new GAV("org.acme", "app", "1.0"))));

        assertThat(declaredDependencies)
                .containsOnlyKeys(ArtifactKey.of("org.acme", "app", "", "jar"));
        assertThat(declaredDependencies.get(ArtifactKey.of("org.acme", "app", "", "jar")).getDeclaredDependencies())
                .extracting(StrictDependencyDataCollectorTest::dependencySnapshot)
                .containsExactlyInAnyOrder(
                        "org.acme:managed-lib:2.0::jar:compile:false",
                        "org.acme:bom-managed-lib:4.0::jar:compile:false",
                        "org.acme:runtime-lib:3.0::jar:runtime:false",
                        "org.acme:system-property-lib:5.0::jar:compile:false");
    }

    @Test
    void shouldBatchPrefetchSharedParentPomDiscoveredDuringModelBuilding() {
        GAV firstApp = new GAV("org.acme", "first-app", "1.0");
        GAV secondApp = new GAV("org.acme", "second-app", "1.0");
        GAV parent = new GAV("org.acme", "parent", "1.0");
        BatchingInMemoryPomResolver pomResolver = new BatchingInMemoryPomResolver(Map.of(
                parent, pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.acme</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0</version>
                          <packaging>pom</packaging>
                          <properties>
                            <shared.version>2.0</shared.version>
                          </properties>
                        </project>
                        """),
                firstApp, childWithParentPom("first-app", """
                        <dependencies>
                          <dependency>
                            <groupId>org.acme</groupId>
                            <artifactId>shared-lib</artifactId>
                            <version>${shared.version}</version>
                          </dependency>
                        </dependencies>
                        """),
                secondApp, childWithParentPom("second-app", """
                        <dependencies>
                          <dependency>
                            <groupId>org.acme</groupId>
                            <artifactId>shared-lib</artifactId>
                            <version>${shared.version}</version>
                          </dependency>
                        </dependencies>
                        """)));
        StrictDependencyDataCollector collector = new StrictDependencyDataCollector(
                pomResolver,
                Map::of);

        Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(
                        new ExternalModuleDeclaredDependencyInput(
                                ArtifactKey.of("org.acme", "first-app", "", "jar"), firstApp),
                        new ExternalModuleDeclaredDependencyInput(
                                ArtifactKey.of("org.acme", "second-app", "", "jar"), secondApp)));

        assertThat(declaredDependencies.values())
                .allSatisfy(result -> assertThat(result.getDeclaredDependencies())
                        .extracting(StrictDependencyDataCollectorTest::dependencySnapshot)
                        .containsExactly("org.acme:shared-lib:2.0::jar:compile:false"));
        assertThat(pomResolver.prefetchBatches())
                .containsExactly(
                        List.of(firstApp, secondApp),
                        List.of(parent));
    }

    @Test
    void shouldBatchPrefetchNestedImportedPomRequestsAcrossIterations() {
        GAV app = new GAV("org.acme", "app", "1.0");
        GAV parent = new GAV("org.acme", "parent", "1.0");
        GAV bom = new GAV("org.acme", "acme-bom", "1.0");
        BatchingInMemoryPomResolver pomResolver = new BatchingInMemoryPomResolver(Map.of(
                app, childWithParentPom("app", """
                        <dependencies>
                          <dependency>
                            <groupId>org.acme</groupId>
                            <artifactId>managed-lib</artifactId>
                          </dependency>
                        </dependencies>
                        """),
                parent, pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.acme</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0</version>
                          <packaging>pom</packaging>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>acme-bom</artifactId>
                                <version>1.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                        </project>
                        """),
                bom, pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.acme</groupId>
                          <artifactId>acme-bom</artifactId>
                          <version>1.0</version>
                          <packaging>pom</packaging>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>managed-lib</artifactId>
                                <version>3.0</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                        </project>
                        """)));
        StrictDependencyDataCollector collector = new StrictDependencyDataCollector(
                pomResolver,
                Map::of);

        Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(new ExternalModuleDeclaredDependencyInput(
                        ArtifactKey.of("org.acme", "app", "", "jar"), app)));

        assertThat(declaredDependencies.get(ArtifactKey.of("org.acme", "app", "", "jar")).getDeclaredDependencies())
                .extracting(StrictDependencyDataCollectorTest::dependencySnapshot)
                .containsExactly("org.acme:managed-lib:3.0::jar:compile:false");
        assertThat(pomResolver.prefetchBatches())
                .containsExactly(
                        List.of(app),
                        List.of(parent),
                        List.of(bom));
    }

    @Test
    void shouldStopBatchPrefetchRetriesWhenDiscoveredParentPomIsMissing() {
        GAV app = new GAV("org.acme", "app", "1.0");
        GAV parent = new GAV("org.acme", "missing-parent", "1.0");
        BatchingInMemoryPomResolver pomResolver = new BatchingInMemoryPomResolver(Map.of(
                app, pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.acme</groupId>
                            <artifactId>missing-parent</artifactId>
                            <version>1.0</version>
                          </parent>
                          <artifactId>app</artifactId>
                        </project>
                        """)));
        StrictDependencyDataCollector collector = new StrictDependencyDataCollector(
                pomResolver,
                Map::of);

        Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(new ExternalModuleDeclaredDependencyInput(
                        ArtifactKey.of("org.acme", "app", "", "jar"), app)));

        assertThat(declaredDependencies.get(ArtifactKey.of("org.acme", "app", "", "jar")).isResolved()).isFalse();
        assertThat(pomResolver.prefetchBatches())
                .containsExactly(
                        List.of(app),
                        List.of(parent));
    }

    @Test
    void shouldKeepArtifactKeySeparateFromPomLookupGav() {
        StrictDependencyDataCollector collector = new StrictDependencyDataCollector(
                new InMemoryPomResolver(Map.of(new GAV("org.acme", "app", "1.0"),
                        simplePom("org.acme", "app", "1.0"))),
                Map::of);
        ArtifactKey classifiedKey = ArtifactKey.of("org.acme", "app", "tests", "jar");

        Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(new ExternalModuleDeclaredDependencyInput(classifiedKey, new GAV("org.acme", "app", "1.0"))));

        assertThat(declaredDependencies).containsOnlyKeys(classifiedKey);
        assertThat(declaredDependencies.get(classifiedKey).isResolved()).isTrue();
    }

    @Test
    void shouldCacheExternalDeclaredDependenciesByPomGav() {
        StrictDependencyDataCollector collector = new StrictDependencyDataCollector(
                new InMemoryPomResolver(Map.of(
                        new GAV("org.acme", "app", "1.0"), pom("""
                                <project>
                                  <modelVersion>4.0.0</modelVersion>
                                  <groupId>org.acme</groupId>
                                  <artifactId>app</artifactId>
                                  <version>1.0</version>
                                  <dependencies>
                                    <dependency>
                                      <groupId>org.acme</groupId>
                                      <artifactId>first-version-lib</artifactId>
                                      <version>1.0</version>
                                    </dependency>
                                  </dependencies>
                                </project>
                                """),
                        new GAV("org.acme", "app", "2.0"), pom("""
                                <project>
                                  <modelVersion>4.0.0</modelVersion>
                                  <groupId>org.acme</groupId>
                                  <artifactId>app</artifactId>
                                  <version>2.0</version>
                                  <dependencies>
                                    <dependency>
                                      <groupId>org.acme</groupId>
                                      <artifactId>second-version-lib</artifactId>
                                      <version>2.0</version>
                                    </dependency>
                                  </dependencies>
                                </project>
                                """))),
                Map::of);
        ArtifactKey key = ArtifactKey.of("org.acme", "app", "", "jar");

        collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(new ExternalModuleDeclaredDependencyInput(key, new GAV("org.acme", "app", "1.0"))));
        Map<ArtifactKey, DeclaredDepsResult> secondVersion = collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(new ExternalModuleDeclaredDependencyInput(key, new GAV("org.acme", "app", "2.0"))));

        assertThat(secondVersion.get(key).getDeclaredDependencies())
                .extracting(StrictDependencyDataCollectorTest::dependencySnapshot)
                .containsExactly("org.acme:second-version-lib:2.0::jar:compile:false");
    }

    @Test
    void shouldReturnUnresolvedExternalDeclaredDependenciesWhenPomIsMissing() {
        StrictDependencyDataCollector collector = new StrictDependencyDataCollector(
                new InMemoryPomResolver(Map.of()),
                Map::of);
        ArtifactKey key = ArtifactKey.of("org.acme", "missing", "", "jar");

        Map<ArtifactKey, DeclaredDepsResult> declaredDependencies = collector.collectExternalDeclaredDependencies(
                mock(Logger.class),
                List.of(new ExternalModuleDeclaredDependencyInput(key, new GAV("org.acme", "missing", "1.0"))));

        assertThat(declaredDependencies).containsOnlyKeys(key);
        assertThat(declaredDependencies.get(key).isResolved()).isFalse();
        assertThat(declaredDependencies.get(key).getDeclaredDependencies()).isEmpty();
    }

    private Map<ArtifactKey, DeclaredDepsResult> collectDeclaredDependencies(Project project, String configurationName) {
        Configuration configuration = project.getConfigurations().getByName(configurationName);
        return new StrictDependencyDataCollector(
                GradlePomClosureResolver
                        .withGradleArtifactResolution(Collections.emptyMap(), project.getDependencies(), new ArrayList<>()),
                () -> Map.copyOf(System.getProperties().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                entry -> String.valueOf(entry.getKey()),
                                entry -> String.valueOf(entry.getValue())))))
                .collectExternalDeclaredDependencies(mock(Logger.class),
                        StrictDependencyDataCollector.externalModuleDeclaredDependencyInputs(
                                configuration.getIncoming().getArtifacts().getResolvedArtifacts().get()));
    }

    private Project createProject(Path repository) throws IOException {
        Path projectDirectory = tempDir.resolve("project-" + System.nanoTime());
        Files.createDirectories(projectDirectory);
        Project project = ProjectBuilder.builder()
                .withName("app")
                .withProjectDir(projectDirectory.toFile())
                .build();
        project.setGroup("org.acme");
        project.setVersion("1.0");
        project.getPluginManager().apply(JavaLibraryPlugin.class);
        project.getRepositories().maven(repo -> repo.setUrl(repository.toUri()));
        return project;
    }

    private static String dependencySnapshot(DeclaredDependency dependency) {
        return dependency.getGroupId()
                + ":" + dependency.getArtifactId()
                + ":" + dependency.getVersion()
                + ":" + dependency.getClassifier()
                + ":" + dependency.getType()
                + ":" + dependency.getScope()
                + ":" + dependency.isOptional();
    }

    private static Map<GAV, String> effectiveModelPoms() {
        return Map.of(
                new GAV("org.acme", "parent", "1.0"), pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.acme</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0</version>
                          <packaging>pom</packaging>
                          <properties>
                            <runtime.version>3.0</runtime.version>
                          </properties>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>managed-lib</artifactId>
                                <version>2.0</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                        </project>
                        """),
                new GAV("org.acme", "acme-bom", "1.0"), pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <groupId>org.acme</groupId>
                          <artifactId>acme-bom</artifactId>
                          <version>1.0</version>
                          <packaging>pom</packaging>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>bom-managed-lib</artifactId>
                                <version>4.0</version>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                        </project>
                        """),
                new GAV("org.acme", "app", "1.0"), pom("""
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>org.acme</groupId>
                            <artifactId>parent</artifactId>
                            <version>1.0</version>
                          </parent>
                          <artifactId>app</artifactId>
                          <dependencyManagement>
                            <dependencies>
                              <dependency>
                                <groupId>org.acme</groupId>
                                <artifactId>acme-bom</artifactId>
                                <version>1.0</version>
                                <type>pom</type>
                                <scope>import</scope>
                              </dependency>
                            </dependencies>
                          </dependencyManagement>
                          <dependencies>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>managed-lib</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>bom-managed-lib</artifactId>
                            </dependency>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>runtime-lib</artifactId>
                              <version>${runtime.version}</version>
                              <scope>runtime</scope>
                            </dependency>
                            <dependency>
                              <groupId>org.acme</groupId>
                              <artifactId>system-property-lib</artifactId>
                              <version>${external.version}</version>
                            </dependency>
                          </dependencies>
                        </project>
                        """));
    }

    private static void installJar(Path repository, String groupId, String artifactId, String version, String pom)
            throws IOException {
        Path artifactDirectory = artifactDirectory(repository, groupId, artifactId, version);
        Files.createDirectories(artifactDirectory);
        Files.writeString(artifactDirectory.resolve(artifactId + "-" + version + ".pom"), pom, StandardCharsets.UTF_8);
        try (OutputStream output = Files.newOutputStream(artifactDirectory.resolve(artifactId + "-" + version + ".jar"));
                JarOutputStream ignore = new JarOutputStream(output)) {
        }
    }

    private static void installPom(Path repository, String groupId, String artifactId, String version, String pom)
            throws IOException {
        Path artifactDirectory = artifactDirectory(repository, groupId, artifactId, version);
        Files.createDirectories(artifactDirectory);
        Files.writeString(artifactDirectory.resolve(artifactId + "-" + version + ".pom"), pom, StandardCharsets.UTF_8);
    }

    private static Path artifactDirectory(Path repository, String groupId, String artifactId, String version) {
        return repository.resolve(groupId.replace('.', '/')).resolve(artifactId).resolve(version);
    }

    private static String simplePom(String groupId, String artifactId, String version) {
        return pom("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>%s</groupId>
                  <artifactId>%s</artifactId>
                  <version>%s</version>
                </project>
                """.formatted(groupId, artifactId, version));
    }

    private static String childWithParentPom(String artifactId, String body) {
        return pom("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.acme</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0</version>
                  </parent>
                  <artifactId>%s</artifactId>
                  %s
                </project>
                """.formatted(artifactId, body));
    }

    private static String pom(String body) {
        return body.replace("<project>", "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 "
                + "https://maven.apache.org/xsd/maven-4.0.0.xsd\">");
    }

    private record InMemoryPomResolver(Map<GAV, String> poms) implements PomResolver {

        @Override
        public ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException {
            String pom = poms.get(gav);
            if (pom == null) {
                throw new UnresolvableModelException("Could not resolve POM for " + gav,
                        gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
            }
            return new StringModelSource(gav, pom);
        }
    }

    private static final class BatchingInMemoryPomResolver implements PomResolver {

        private final Map<GAV, String> repository;
        private final Map<GAV, String> cache = new LinkedHashMap<>();
        private final Set<GAV> missing = new LinkedHashSet<>();
        private final List<List<GAV>> prefetchBatches = new ArrayList<>();

        private BatchingInMemoryPomResolver(Map<GAV, String> repository) {
            this.repository = repository;
        }

        @Override
        public void prefetchPoms(Collection<GAV> gavs) {
            List<GAV> uncached = gavs.stream()
                    .filter(gav -> !hasPomResult(gav))
                    .sorted((left, right) -> left.toString().compareTo(right.toString()))
                    .toList();
            if (uncached.isEmpty()) {
                return;
            }
            prefetchBatches.add(uncached);
            for (GAV gav : uncached) {
                String pom = repository.get(gav);
                if (pom == null) {
                    missing.add(gav);
                } else {
                    cache.put(gav, pom);
                }
            }
        }

        @Override
        public boolean hasPomResult(GAV gav) {
            return cache.containsKey(gav) || missing.contains(gav);
        }

        @Override
        public ModelSource2 resolvePom(GAV gav) throws UnresolvableModelException {
            String pom = cache.get(gav);
            if (pom == null) {
                throw new UnresolvableModelException("Could not resolve POM for " + gav,
                        gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
            }
            return new StringModelSource(gav, pom);
        }

        private List<List<GAV>> prefetchBatches() {
            return prefetchBatches;
        }
    }

    private record StringModelSource(GAV gav, String pom) implements ModelSource2 {

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(pom.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String getLocation() {
            return gav.toString();
        }

        @Override
        public ModelSource2 getRelatedSource(String relPath) {
            return null;
        }

        @Override
        public URI getLocationURI() {
            return URI.create("memory:/" + gav.getGroupId() + "/" + gav.getArtifactId() + "/" + gav.getVersion());
        }
    }
}
