package io.quarkus.gradle;

import static io.quarkus.gradle.util.AppModelDeserializer.deserializeAppModel;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.maven.dependency.Dependency;

/**
 * Integration tests verifying enforcement of platform constraints for conditional development dependencies
 * in Gradle projects. The test sequence intentionally relies on method ordering in order to first publish
 * the synthetic extensions and libraries to the local Maven repository and then exercise different
 * consumer scenarios:
 * <ol>
 * <li>Publishing test extensions and libraries</li>
 * <li>Baseline consumer without any enforced constraints</li>
 * <li>Consumer with enforced (pinned) version constraints</li>
 * <li>Consumer that excludes an optional dev-mode only dependency</li>
 * </ol>
 *
 * We avoid a {@code @BeforeAll} lifecycle hook (which would need to be static) to preserve access to
 * instance helpers from {@link QuarkusGradleWrapperTestBase}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Conditional dependency platform enforcement (Gradle)")
public class EnforcingPlatformForConditionalDepsTest extends QuarkusGradleWrapperTestBase {

    private static final String CONSUMER_PROJECT_PATH = "enforcing-platform-for-conditional-deps/consumer-project";
    private static final String PRODUCER_PROJECT_PATH = "enforcing-platform-for-conditional-deps/other-deps";
    private static final Set<String> GROUP_IDS_TO_TEST = Set.of("org.enforcing.deps", "org.enforcing.conditional");

    /**
     * Publishes the test extensions, libraries and BOM used by the subsequent test scenarios to the local Maven repo.
     */
    @Test
    @Order(1)
    @DisplayName("Publish synthetic test artifacts to local Maven repository")
    public void publishTestArtifacts() throws IOException, InterruptedException {
        File dependencyProject = getProjectDir(PRODUCER_PROJECT_PATH);
        runGradleWrapper(dependencyProject, ":simple-dep:publishToMavenLocal");
        runGradleWrapper(dependencyProject,
                ":ext-a:runtime:publishToMavenLocal",
                ":ext-a:deployment:publishToMavenLocal",
                ":dev-mode-only-lib-94:publishToMavenLocal",
                ":dev-mode-only-lib-99:publishToMavenLocal",
                ":test-bom:publishToMavenLocal",
                ":test-bom-with-exclusion:publishToMavenLocal");
    }

    /**
     * Verifies that, without any platform constraint enforcement, the newer dev-mode only library version (9.9.0)
     * is selected alongside the test extension artifacts.
     */
    @Test
    @Order(2)
    @DisplayName("Resolve conditional dev dependencies without enforced constraints (selects newest version)")
    public void conditionalDevDependenciesWithoutConstraints() throws Exception {
        assertConditionalDependencies(
                "runner",
                "ext-a:1.0-SNAPSHOT",
                "ext-a-deployment:1.0-SNAPSHOT",
                "dev-mode-only-lib:9.9.0",
                "simple-dependency:1.0.0");
    }

    /**
     * Verifies that when constraints are enforced, the pinned dev-mode only library version (9.4.0) is respected.
     */
    @Test
    @Order(3)
    @DisplayName("Enforce pinned dev-mode-only dependency version (9.4.0)")
    public void enforcePinnedVersion() throws Exception {
        assertConditionalDependencies(
                "runner-enforced",
                "ext-a:1.0-SNAPSHOT",
                "ext-a-deployment:1.0-SNAPSHOT",
                "dev-mode-only-lib:9.4.0",
                "simple-dependency:1.0.0");
    }

    /**
     * Verifies that the conditional dev-mode only library can be excluded entirely via consumer-level configuration.
     * The exclusion is configured directly in the consumer project's build configuration.
     */
    @Test
    @Order(4)
    @DisplayName("Exclude conditional dev-mode-only dependency via consumer configuration")
    public void excludeConditionalDependencyFromConsumer() throws Exception {
        assertConditionalDependencies(
                "runner-excluded-1",
                "ext-a:1.0-SNAPSHOT",
                "ext-a-deployment:1.0-SNAPSHOT");
    }

    /**
     * Verifies that the conditional dev-mode only library can be excluded entirely via BOM-level configuration.
     * The exclusion is specified in the Bill of Materials (BOM) that governs dependency management.
     */
    @Test
    @Order(5)
    @DisplayName("Exclude conditional dev-mode-only dependency via BOM configuration")
    public void excludeConditionalDependencyFromBom() throws Exception {
        assertConditionalDependencies(
                "runner-excluded-2",
                "ext-a:1.0-SNAPSHOT",
                "ext-a-deployment:1.0-SNAPSHOT",
                "dev-mode-only-lib:9.4.0");
    }

    // -------------------------------------------------------------------------------------
    // Helper method
    // -------------------------------------------------------------------------------------

    /**
     * Runs the given Gradle task that produces the dev application model for the specified module variant, then
     * asserts that the resolved set of artifacts belonging to {@link #GROUP_IDS_TO_TEST} matches exactly the
     * expected coordinates (artifactId:version) in any order.
     *
     * @param moduleDirName the directory name of the module (used to compute the build output path)
     * @param expectedArtifacts the expected set of artifactId:version coordinates
     */
    private void assertConditionalDependencies(String moduleDirName, String... expectedArtifacts)
            throws Exception {
        var projectDir = getProjectDir(CONSUMER_PROJECT_PATH);
        var fullTaskName = ":%s:quarkusGenerateDevAppModel".formatted(moduleDirName);
        runGradleWrapper(projectDir, "clean", fullTaskName);
        var appModel = deserializeAppModel(
                projectDir.toPath().resolve(moduleDirName + "/build/quarkus/application-model/quarkus-app-dev-model.dat"));
        var conditionalArtifacts = ((Collection<? extends Dependency>) appModel.getDependencies()).stream()
                .filter(d -> GROUP_IDS_TO_TEST.contains(d.getGroupId()))
                .map(d -> d.getArtifactId() + ":" + d.getVersion())
                .collect(Collectors.toSet());
        assertThat(conditionalArtifacts).containsExactlyInAnyOrder(expectedArtifacts);
    }
}
