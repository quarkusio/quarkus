package io.quarkus.gradle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collection;

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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Relocated dependencies (Gradle)")
public class RelocatedDependencyTest extends QuarkusGradleWrapperTestBase {

    private static final String CONSUMER_PROJECT_PATH = "relocated-deps/consumer-gradle";
    private static final String PRODUCER_PROJECT_PATH = "relocated-deps/producer-maven";

    private static final String NEW_GROUP_ID = "new.group";

    @Test
    @Order(1)
    @DisplayName("Publish Maven test artifacts to local repository")
    public void publishTestArtifacts() throws Exception {
        File producerProject = getProjectDir(PRODUCER_PROJECT_PATH);
        runGradleWrapper(producerProject, "publishToMavenLocal");
    }

    @Test
    @Order(2)
    @DisplayName("Test relocated dependency with constraint version enforcement")
    public void testRelocatedDependencyWithConstraintVersionEnforcement() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":app-managed:quarkusGenerateAppModel",
                "-PenableDeclaredDependencyCollector=true");

        ApplicationModel model = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("app-managed/build/quarkus/application-model/quarkus-app-model.dat"));

        ResolvedDependency appArtifact = model.getAppArtifact();
        assertThat(appArtifact.getDirectDependencies())
                .filteredOn(dep -> NEW_GROUP_ID.equals(dep.getGroupId()))
                .hasSize(1);
        for (Dependency dep : appArtifact.getDirectDependencies()) {
            if (NEW_GROUP_ID.equals(dep.getGroupId())) {
                assertThat(dep.getArtifactId()).isEqualTo("lib-chain");
                assertThat(dep.getVersion()).isEqualTo("2");
            }
        }

        var deps = model.getDependencies();
        for (ResolvedDependency dep : deps) {
            if ("lib-chain".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getVersion()).isEqualTo("3");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isTrue();
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("Test relocated optional dependency")
    public void testRelocatedOptionalDependency() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":app-opt:quarkusGenerateAppModel",
                "-PenableDeclaredDependencyCollector=true");

        ApplicationModel model = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("app-opt/build/quarkus/application-model/quarkus-app-model.dat"));

        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib-opt".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.isOptional()).isTrue();
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test relocated provided dependency")
    public void testRelocatedProvidedDependency() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":app-prov:quarkusGenerateAppModel",
                "-PenableDeclaredDependencyCollector=true");

        ApplicationModel model = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("app-prov/build/quarkus/application-model/quarkus-app-model.dat"));

        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib-prov".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getScope()).isEqualTo("provided");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test relocated required dependency")
    public void testRelocatedRequiredDependency() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":app-req:quarkusGenerateAppModel",
                "-PenableDeclaredDependencyCollector=true");

        ApplicationModel model = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("app-req/build/quarkus/application-model/quarkus-app-model.dat"));

        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib-req".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getScope()).isEqualTo("compile");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }

    @Test
    @Order(6)
    @DisplayName("Test relocation with version property")
    public void testRelocationWithVersionProperty() throws Exception {
        File projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        runGradleWrapper(projectDir, "clean", ":app-prop:quarkusGenerateAppModel",
                "-PenableDeclaredDependencyCollector=true");

        ApplicationModel model = ApplicationModelSerializer.deserialize(
                projectDir.toPath().resolve("app-prop/build/quarkus/application-model/quarkus-app-model.dat"));

        for (ResolvedDependency dep : model.getDependencies()) {
            if ("lib-prop".equals(dep.getArtifactId())) {
                Collection<Dependency> directDeps = dep.getDirectDependencies();
                assertThat(directDeps).hasSize(1);
                Dependency dd = directDeps.iterator().next();
                assertThat(dd.getGroupId()).isEqualTo(NEW_GROUP_ID);
                assertThat(dd.getArtifactId()).isEqualTo("new-api");
                assertThat(dd.getVersion()).isEqualTo("2");
                assertThat(dd.isFlagSet(DependencyFlags.MISSING_FROM_APPLICATION)).isFalse();
            }
        }
    }
}
