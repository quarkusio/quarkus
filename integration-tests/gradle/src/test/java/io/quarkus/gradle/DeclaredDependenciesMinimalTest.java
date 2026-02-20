package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

// @formatter:off
/**
 * runner
 * ├─ org.acme.deps:lib-e (test)
 * ├─ project :local-lib
 * │  └─ org.acme.deps:lib-c
 * └─ org.acme.deps:lib-a
 *    ├─ org.acme.deps:lib-b
 *    ├─ org.acme.deps:lib-c (optional)
 *    ├─ org.acme.deps:lib-d (optional)
 *    └─ org.acme.deps:lib-e (test)
 */
// @formatter:on
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Declared dependencies (Gradle)")
public class DeclaredDependenciesMinimalTest extends QuarkusGradleWrapperTestBase {

    private static final String CONSUMER_PROJECT_PATH = "declared-deps-minimal/consumer-gradle";
    private static final String PRODUCER_PROJECT_PATH = "declared-deps-minimal/producer-maven";

    private static final Set<String> EXPECTED_GROUPS = Set.of("org.acme.local", "org.acme.deps");

    private static final String GRADLE_SUBPROJECT = "org.acme.local:local-lib:1.0.0";
    private static final String LIB_A = "org.acme.deps:lib-a:1.0.0";
    private static final String LIB_B = "org.acme.deps:lib-b:1.0.0";
    private static final String LIB_C = "org.acme.deps:lib-c:1.0.0";
    private static final String LIB_D = "org.acme.deps:lib-d:1.0.0";
    private static final String LIB_E = "org.acme.deps:lib-e:1.0.0";

    @Test
    @Order(1)
    @DisplayName("Publish Maven test artifact to local repository")
    public void publishTestArtifacts() throws Exception {
        File producerProject = getProjectDir(PRODUCER_PROJECT_PATH);
        runGradleWrapper(producerProject, "publishToMavenLocal");
    }

    @Test
    @Order(2)
    @DisplayName("Generate app model and verify direct dependencies")
    public void generateAppModel() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":runner:quarkusGenerateAppModel");

        ApplicationModel appModel = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("runner/build/quarkus/application-model/quarkus-app-model.dat"));

        // App artifact declared deps
        Set<String> appArtifactDeclared = getDeclared(appModel.getAppArtifact()).keySet();
        assertThat(appArtifactDeclared).containsOnly(LIB_A, GRADLE_SUBPROJECT);

        // App artifact resolved deps
        Set<String> appArtifactResolved = getResolved(appModel.getAppArtifact());
        assertThat(appArtifactResolved).containsOnly(LIB_A, GRADLE_SUBPROJECT);

        // All resolved deps
        Set<String> allResolved = getAll(appModel).keySet();
        assertThat(allResolved).containsOnly(LIB_A, GRADLE_SUBPROJECT, LIB_B, LIB_C);

        // Lib A
        ResolvedDependency libA = findDependency(appModel, LIB_A);
        assertThat(libA).isNotNull();

        // Lib A declared deps
        Map<String, Dependency> libADeclaredDeps = getDeclared(libA);

        Dependency libB = libADeclaredDeps.get(LIB_B);
        assertThat(libB).isNotNull();
        assertThat(libB.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();

        Dependency libC = libADeclaredDeps.get(LIB_C);
        assertThat(libC).isNotNull();
        assertThat(libC.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();

        Dependency libD = libADeclaredDeps.get(LIB_D);
        assertThat(libD).isNotNull();
        assertThat(libD.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isTrue();

        assertThat(libADeclaredDeps).doesNotContainKey(LIB_E);

        // Lib A resolved deps
        Set<String> libAResolvedCoords = getResolved(libA);
        assertThat(libAResolvedCoords).containsOnly(LIB_B, LIB_C);
    }

    @Test
    @Order(3)
    @DisplayName("Generate test app model and verify test-scope root dependencies")
    public void generateTestAppModel() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":runner:quarkusGenerateTestAppModel");

        ApplicationModel appModel = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("runner/build/quarkus/application-model/quarkus-app-test-model.dat"));

        // Lib E is found on app artifact (since we are in test scope and it's app artifact)
        Dependency declaredLibE = getDeclared(appModel.getAppArtifact()).get(LIB_E);
        assertThat(declaredLibE).isNotNull();
        assertThat(declaredLibE.getScope()).isEqualTo("test");

        // Same for resolved
        var appArtifactResolved = getResolved(appModel.getAppArtifact());
        assertThat(appArtifactResolved).contains(LIB_E);

        // Lib E is NOT found in lib A though, since lib A is not root
        ResolvedDependency libA = findDependency(appModel, LIB_A);
        assertThat(libA).isNotNull();
        Map<String, Dependency> libADeclaredDeps = getDeclared(libA);
        assertThat(libADeclaredDeps).doesNotContainKey(LIB_E);
    }

    @Test
    @Order(4)
    @DisplayName("Normal model does not leak test deps after test model run")
    public void normalModelDoesNotLeakTestDeps() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":runner:quarkusGenerateAppModel", ":runner:quarkusGenerateTestAppModel");

        ApplicationModel normalModel = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("runner/build/quarkus/application-model/quarkus-app-model.dat"));
        ApplicationModel testModel = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("runner/build/quarkus/application-model/quarkus-app-test-model.dat"));

        Dependency normalLibE = getDeclared(normalModel.getAppArtifact()).get(LIB_E);
        Dependency testLibE = getDeclared(testModel.getAppArtifact()).get(LIB_E);

        assertThat(normalLibE).isNull();
        assertThat(testLibE).isNotNull();
        assertThat(testLibE.getScope()).isEqualTo("test");
    }

    private static Map<String, ResolvedDependency> getAll(ApplicationModel appModel) {
        return appModel.getDependencies().stream()
                .filter(rd -> EXPECTED_GROUPS.contains(rd.getGroupId()))
                .collect(Collectors.toMap(
                        rd -> toCoords(rd.getGroupId(), rd.getArtifactId(), rd.getVersion()),
                        rd -> rd));
    }

    private static ResolvedDependency findDependency(ApplicationModel model, String coords) {
        return getAll(model).get(coords);
    }

    private static Set<String> getResolved(ResolvedDependency resolvedDependency) {
        return resolvedDependency.getDependencies().stream()
                .filter(c -> EXPECTED_GROUPS.contains(c.getGroupId()))
                .map(c -> toCoords(c.getGroupId(), c.getArtifactId(), c.getVersion()))
                .collect(Collectors.toSet());
    }

    private static Map<String, Dependency> getDeclared(ResolvedDependency resolvedDependency) {
        return resolvedDependency.getDirectDependencies().stream()
                .filter(d -> EXPECTED_GROUPS.contains(d.getGroupId()))
                .collect(Collectors.toMap(
                        dep -> toCoords(dep.getGroupId(), dep.getArtifactId(), dep.getVersion()),
                        dep -> dep));
    }

    private static String toCoords(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }
}
