package io.quarkus.gradle.application.internal.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EffectiveConfigPlannerTest {

    private final EffectiveConfigPlanner planner = new EffectiveConfigPlanner();

    @TempDir
    Path directory;

    @Test
    void outputBuildPropertiesOverrideCommonAndProjectProperties() {
        var plan = planner.plan(request(Map.of("quarkus.foo", "common"),
                Map.of("quarkus.foo", "output"),
                Map.of(),
                Map.of("quarkus.foo", "project"),
                Map.of()));

        assertThat(plan.fullValues()).containsEntry("quarkus.foo", "output");
        assertThat(plan.quarkusWorkerValues()).containsEntry("quarkus.foo", "output");
    }

    @Test
    void taskManifestPropertiesOverrideOutputBuildProperties() {
        String manifestKey = "quarkus.package.jar.manifest.attributes.\"Built-By\"";
        var request = new EffectiveConfigRequest(
                Map.of(),
                "app",
                "1.0",
                Set.of(directory.toFile()),
                Map.of(),
                Map.of(manifestKey, "raw-build-property"),
                Map.of(),
                Map.of(manifestKey, "manifest-dsl"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                "prod");

        var plan = planner.plan(request);

        assertThat(plan.fullValues()).containsEntry(manifestKey, "manifest-dsl");
        assertThat(plan.quarkusWorkerValues()).containsEntry(manifestKey, "manifest-dsl");
        assertThat(plan.buildSystemProperties()).containsEntry(manifestKey, "manifest-dsl");
        assertThat(plan.diagnostics())
                .filteredOn(diagnostic -> diagnostic.key().equals(manifestKey))
                .singleElement()
                .satisfies(diagnostic -> {
                    assertThat(diagnostic.value()).isEqualTo("manifest-dsl");
                    assertThat(diagnostic.source()).isEqualTo("task configuration");
                });
    }

    @Test
    void forcedShapeOverridesApplicationProperties() throws Exception {
        Files.writeString(directory.resolve("application.properties"), "quarkus.package.jar.type=uber-jar\n");

        var plan = planner.plan(request(Map.of(),
                Map.of(),
                Map.of("quarkus.package.jar.type", "fast-jar"),
                Map.of(),
                Map.of()));

        assertThat(plan.fullValues()).containsEntry("quarkus.package.jar.type", "fast-jar");
        assertThat(plan.descriptorShapeValues()).containsEntry("quarkus.package.jar.type", "fast-jar");
    }

    @Test
    void forcedOperationPropertiesOverrideExternalWorkerSystemProperties() {
        var plan = planner.plan(request(Map.of("quarkus.container-image.build", "false"),
                Map.of(),
                Map.of(
                        "quarkus.container-image.build", "true",
                        "quarkus.container-image.push", "false"),
                Map.of("quarkus.container-image.build", "false"),
                Map.of("quarkus.container-image.push", "true")));

        assertThat(plan.quarkusWorkerValues())
                .containsEntry("quarkus.container-image.build", "true")
                .containsEntry("quarkus.container-image.push", "false");
        assertThat(plan.buildSystemProperties())
                .containsEntry("quarkus.container-image.build", "true")
                .containsEntry("quarkus.container-image.push", "false");
    }

    @Test
    void testPropertiesPropagateFromApplicationProperties() throws Exception {
        Files.writeString(directory.resolve("application.properties"), "quarkus.test.profile=from-file\n");

        var plan = planner.plan(request(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));

        assertThat(plan.quarkusWorkerValues()).containsEntry("quarkus.test.profile", "from-file");
    }

    @Test
    void environmentValuesDoNotPropagateToWorkers() {
        var plan = planner.plan(request(Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of("quarkus.foo", "from-env"),
                Map.of()));

        assertThat(plan.fullValues()).containsEntry("quarkus.foo", "from-env");
        assertThat(plan.quarkusWorkerValues()).doesNotContainKey("quarkus.foo");
    }

    @Test
    void fallbackPlatformPropertyDoesNotPropagateToWorkers() {
        var plan = planner.plan(request(Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));

        assertThat(plan.quarkusWorkerValues()).doesNotContainKey("platform.quarkus.native.builder-image");
        assertThat(plan.buildSystemProperties()).doesNotContainKey("platform.quarkus.native.builder-image");
    }

    @Test
    void resolvedPlatformPropertyPropagatesToWorkers() {
        var request = new EffectiveConfigRequest(
                Map.of("platform.quarkus.native.builder-image", "configured-builder-image"),
                "app",
                "1.0",
                Set.of(directory.toFile()),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                "prod");

        var plan = planner.plan(request);

        assertThat(plan.quarkusWorkerValues())
                .containsEntry("platform.quarkus.native.builder-image", "configured-builder-image");
        assertThat(plan.buildSystemProperties())
                .containsEntry("platform.quarkus.native.builder-image", "configured-builder-image");
    }

    @Test
    void diagnosticValuesOmitSystemPropertiesAndEnvironmentVariables() {
        var plan = planner.plan(request(Map.of(),
                Map.of(),
                Map.of(),
                Map.of("application.visible", "visible"),
                Map.of("application.environment-secret", "environment-secret"),
                Map.of("application.system-secret", "system-secret")));

        assertThat(plan.fullValues())
                .containsEntry("application.visible", "visible")
                .containsEntry("application.environment-secret", "environment-secret")
                .containsEntry("application.system-secret", "system-secret");
        assertThat(plan.diagnostics())
                .extracting(EffectiveConfigDiagnostic::key)
                .contains("application.visible")
                .doesNotContain("application.environment-secret", "application.system-secret");
        assertThat(plan.externallyProvidedValuesOmitted()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void validatesDescriptorShapeValues() {
        var validator = new ShapeValidator();
        var expectation = new ShapeExpectation("app", "quarkusAppImageBuild",
                Map.of("quarkus.package.jar.type", "fast-jar"));

        assertThatThrownBy(() -> validator.validate(expectation, Map.of("quarkus.package.jar.type", "uber-jar")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app")
                .hasMessageContaining("quarkusAppImageBuild")
                .hasMessageContaining("quarkus.package.jar.type=fast-jar")
                .hasMessageContaining("quarkus.package.jar.type=uber-jar");
    }

    private EffectiveConfigRequest request(Map<String, String> commonBuildProperties,
            Map<String, String> outputBuildProperties, Map<String, String> forcedProperties,
            Map<String, ?> projectProperties, Map<String, String> systemProperties) {
        return new EffectiveConfigRequest(
                Map.of(),
                "app",
                "1.0",
                Set.of(directory.toFile()),
                commonBuildProperties,
                outputBuildProperties,
                forcedProperties,
                Map.of(),
                projectProperties,
                Map.of(),
                systemProperties,
                Map.of(),
                "prod");
    }

    private EffectiveConfigRequest request(Map<String, String> commonBuildProperties,
            Map<String, String> outputBuildProperties, Map<String, String> forcedProperties,
            Map<String, ?> projectProperties, Map<String, String> environment, Map<String, String> systemProperties) {
        return new EffectiveConfigRequest(
                Map.of(),
                "app",
                "1.0",
                Set.of(directory.toFile()),
                commonBuildProperties,
                outputBuildProperties,
                forcedProperties,
                Map.of(),
                projectProperties,
                environment,
                systemProperties,
                Map.of(),
                "prod");
    }
}
