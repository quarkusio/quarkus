package io.quarkus.gradle.application;

import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Kind.CLASSES;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Kind.PROCESSED_RESOURCES;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Scope.MAIN;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Scope.TEST;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.ClasspathAssociation.COMPILE_ONLY;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.GraphRelationship.DIRECT;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.GraphRelationship.TRANSITIVE;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.GraphRelationship.WORKSPACE_DIRECT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.events.task.TaskStartEvent;
import org.gradle.wrapper.GradleUserHomeLookup;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarValidator;
import io.quarkus.bootstrap.model.gradle.GradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.GradleProjectComponent;
import io.quarkus.bootstrap.model.gradle.GradleSourceObservation;
import io.quarkus.gradle.testing.BaseGradleTest;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependency;

class QuarkusApplicationToolingSemanticMatrixTest extends BaseGradleTest {

    private static final String SAME_DIRECT = "same-direct";
    private static final String SAME_TRANSITIVE = "same-transitive";
    private static final String SAME_COMPILE_ONLY = "same-compile-only";
    private static final String SHARED = "shared";
    private static final String INCLUDED_BRIDGE = "included-bridge";
    private static final String INCLUDED_TRANSITIVE = "included-transitive";
    private static final String CUSTOM_OUTPUT = "custom-output";
    private static final String OUTPUT_ONLY = "output-only";
    private static final String FIXTURE_PROVIDER = "fixture-provider";

    @Test
    void developmentModelPreservesCompositeGraphAndPublishedOutputs() throws Exception {
        writeSemanticFixture();

        RequestResult<ToolingPairedModels> first = request(new ToolingPairedModelsAction("DEVELOPMENT"));
        RequestResult<ToolingPairedModels> second = request(new ToolingPairedModelsAction("DEVELOPMENT"));
        ApplicationModel model = second.model().applicationModel();
        GradleApplicationModelSidecar sidecar = second.model().sidecar();

        assertStoredThenReused(first, second);
        assertNoTasks(first, second);
        assertThat(model.getDependencies()).extracting(ResolvedDependency::getArtifactId)
                .contains(
                        SAME_DIRECT,
                        SAME_TRANSITIVE,
                        SAME_COMPILE_ONLY,
                        SHARED,
                        INCLUDED_BRIDGE,
                        INCLUDED_TRANSITIVE,
                        CUSTOM_OUTPUT,
                        OUTPUT_ONLY);
        assertThat(dependency(model, SAME_COMPILE_ONLY).isFlagSet(DependencyFlags.COMPILE_ONLY)).isTrue();
        assertThat(dependency(model, SAME_DIRECT).isDirect()).isTrue();
        assertThat(dependency(model, SAME_TRANSITIVE).isDirect()).isFalse();
        assertThat(dependency(model, SAME_DIRECT).getWorkspaceModule()).isNotNull();
        assertThat(dependency(model, SAME_DIRECT).isReloadable()).isFalse();

        GradleProjectComponent sameDirect = component(sidecar, ":", ":same-direct");
        assertThat(sameDirect.getGraphRelationships()).contains(DIRECT, WORKSPACE_DIRECT);
        GradleProjectComponent sameTransitive = component(sidecar, ":", ":same-transitive");
        assertThat(sameTransitive.getGraphRelationships()).contains(TRANSITIVE).doesNotContain(DIRECT, WORKSPACE_DIRECT);
        assertThat(component(sidecar, ":", ":same-compile-only").getClasspathAssociations()).contains(COMPILE_ONLY);
        assertThat(component(sidecar, ":included", ":included-transitive").getGraphRelationships())
                .contains(TRANSITIVE)
                .doesNotContain(DIRECT, WORKSPACE_DIRECT);

        List<? extends GradleProjectComponent> duplicateProjectPaths = sidecar.getProjectComponents().stream()
                .filter(component -> component.getProjectIdentity().getProjectPath().equals(":shared"))
                .toList();
        assertThat(duplicateProjectPaths).hasSize(2);
        assertThat(duplicateProjectPaths)
                .extracting(component -> component.getProjectIdentity().getBuildPath())
                .containsExactlyInAnyOrder(":", ":included");
        assertThat(duplicateProjectPaths)
                .extracting(component -> component.getProjectIdentity().getBuildTreePath())
                .doesNotHaveDuplicates();

        GradleProjectComponent customOutput = component(sidecar, ":", ":custom-output");
        assertThat(customOutput.getSourceObservations())
                .extracting(GradleSourceObservation::getPath)
                .anyMatch(path -> endsWith(path, "custom-output", "custom-java"))
                .anyMatch(path -> endsWith(path, "custom-output", "custom-resources"))
                .anyMatch(path -> endsWith(path, "custom-output", "custom-build", "generated", "sources", "registered"))
                .noneMatch(
                        path -> endsWith(path, "custom-output", "custom-build", "generated", "sources", "unregistered"));
        assertThat(customOutput.getSourceObservations())
                .allSatisfy(source -> {
                    assertThat(source.getRole()).isEqualTo(GradleSourceObservation.Role.UNKNOWN);
                    assertThat(source.hasLogicalOutputAssociation()).isFalse();
                });
        assertThat(customOutput.getLogicalOutputs())
                .filteredOn(output -> output.getKind() == CLASSES)
                .extracting(GradleLogicalOutput::getPath)
                .anyMatch(path -> endsWith(path, "custom-output", "custom-build", "classes", "java", "main"))
                .anyMatch(path -> endsWith(path, "custom-output", "custom-build", "classes", "java", "secondary"));
        assertThat(customOutput.getLogicalOutputs())
                .filteredOn(output -> output.getKind() == PROCESSED_RESOURCES)
                .extracting(GradleLogicalOutput::getPath)
                .anyMatch(path -> endsWith(path, "custom-output", "custom-build", "resources", "main"));
        assertThat(customOutput.getLogicalOutputs())
                .filteredOn(output -> !endsWith(output.getPath(), "secondary"))
                .allSatisfy(output -> {
                    assertThat(output.getModelAssociation().isKnown()).isTrue();
                    assertThat(output.getModelAssociation().getResolvedPath()).isEqualTo(output.getPath());
                });
        assertThat(customOutput.getLogicalOutputs())
                .filteredOn(output -> endsWith(output.getPath(), "secondary"))
                .allSatisfy(output -> {
                    assertThat(output.getModelAssociation().isKnown()).isFalse();
                    assertThat(output.getModelAssociation().isEligibleForOverlayReplacement()).isFalse();
                });

        GradleProjectComponent outputOnly = component(sidecar, ":", ":output-only");
        assertThat(outputOnly.getSourceObservations()).isEmpty();
        assertThat(outputOnly.getLogicalOutputs()).isNotEmpty();
    }

    @Test
    void sidecarAndApplicationModelRequestsAreIndependentAndCorrelated() throws Exception {
        writeSemanticFixture();

        RequestResult<ApplicationModel> applicationFirst = request(new ToolingApplicationModelAction("DEVELOPMENT"));
        RequestResult<ApplicationModel> applicationSecond = request(new ToolingApplicationModelAction("DEVELOPMENT"));
        RequestResult<GradleApplicationModelSidecar> sidecarFirst = request(new ToolingSidecarAction("DEVELOPMENT"));
        RequestResult<GradleApplicationModelSidecar> sidecarSecond = request(new ToolingSidecarAction("DEVELOPMENT"));
        RequestResult<ToolingPairedModels> paired = request(new ToolingPairedModelsAction("DEVELOPMENT"));

        assertStoredThenReused(applicationFirst, applicationSecond);
        assertConfigurationCacheReused(sidecarSecond.output());
        assertNoTasks(applicationFirst, applicationSecond, sidecarFirst, sidecarSecond, paired);
        GradleApplicationModelSidecarValidator.validate(
                sidecarSecond.model(),
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION,
                GradleApplicationModelSidecar.Mode.DEVELOPMENT,
                ":",
                applicationSecond.model());
        GradleApplicationModelSidecarValidator.validate(
                paired.model().sidecar(),
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION,
                GradleApplicationModelSidecar.Mode.DEVELOPMENT,
                ":",
                paired.model().applicationModel());
        assertThat(sidecarSecond.model().getCorrelation().getCanonicalGraphFacts())
                .isEqualTo(paired.model().sidecar().getCorrelation().getCanonicalGraphFacts());
    }

    @Test
    void normalWorkspaceDiscoveryIsOptIn() throws Exception {
        writeSemanticFixture();

        RequestResult<ToolingPairedModels> defaultFirst = request(new ToolingPairedModelsAction("NORMAL"));
        RequestResult<ToolingPairedModels> defaultSecond = request(new ToolingPairedModelsAction("NORMAL"));
        RequestResult<ToolingPairedModels> optInFirst = request(
                new ToolingPairedModelsAction("NORMAL"),
                "-Pquarkus.bootstrap.workspace-discovery=true");
        RequestResult<ToolingPairedModels> optInSecond = request(
                new ToolingPairedModelsAction("NORMAL"),
                "-Pquarkus.bootstrap.workspace-discovery=true");

        assertStoredThenReused(defaultFirst, defaultSecond);
        assertStoredThenReused(optInFirst, optInSecond);
        assertNoTasks(defaultFirst, defaultSecond, optInFirst, optInSecond);
        assertThat(dependency(defaultSecond.model().applicationModel(), SAME_DIRECT).getWorkspaceModule()).isNull();
        assertThat(dependency(optInSecond.model().applicationModel(), SAME_DIRECT).getWorkspaceModule()).isNotNull();
        assertThat(defaultSecond.model().sidecar().getProjectComponents())
                .extracting(component -> component.getProjectIdentity().getBuildTreePath())
                .contains(":same-direct", ":included:included-bridge");
        assertThat(optInSecond.model().sidecar().getProjectComponents())
                .extracting(component -> component.getProjectIdentity().getBuildTreePath())
                .contains(":same-direct", ":included:included-bridge");
    }

    @Test
    void testModeIncludesTestFixturesAndDistinctTargetOutputs() throws Exception {
        writeSemanticFixture();

        RequestResult<ToolingPairedModels> first = request(new ToolingPairedModelsAction("TEST"));
        RequestResult<ToolingPairedModels> second = request(new ToolingPairedModelsAction("TEST"));
        ApplicationModel model = second.model().applicationModel();
        GradleApplicationModelSidecar sidecar = second.model().sidecar();

        assertStoredThenReused(first, second);
        assertNoTasks(first, second);
        assertThat(model.getDependencies())
                .filteredOn(dependency -> dependency.getArtifactId().equals(FIXTURE_PROVIDER))
                .extracting(ResolvedDependency::getClassifier)
                .contains("test-fixtures");
        assertThat(model.getApplicationModule().hasTestSources()).isTrue();

        GradleProjectComponent target = component(sidecar, ":", ":");
        assertThat(target.getLogicalOutputs())
                .filteredOn(output -> output.getScope() == MAIN)
                .extracting(GradleLogicalOutput::getKind)
                .contains(CLASSES, PROCESSED_RESOURCES);
        assertThat(target.getLogicalOutputs())
                .filteredOn(output -> output.getScope() == TEST)
                .extracting(GradleLogicalOutput::getKind)
                .contains(CLASSES, PROCESSED_RESOURCES);
        assertThat(target.getLogicalOutputs())
                .filteredOn(output -> output.getKind() == CLASSES)
                .extracting(GradleLogicalOutput::getPath)
                .anyMatch(path -> endsWith(path, "application-build", "classes", "java", "main"))
                .anyMatch(path -> endsWith(path, "application-build", "classes", "java", "secondary"))
                .anyMatch(path -> endsWith(path, "application-build", "classes", "java", "test"));
        assertThat(target.getLogicalOutputs())
                .filteredOn(output -> endsWith(output.getPath(), "application-build", "classes", "java", "main"))
                .allSatisfy(output -> {
                    assertThat(output.getModelAssociation().isKnown()).isFalse();
                    assertThat(output.getModelAssociation().isEligibleForOverlayReplacement()).isFalse();
                });
        assertThat(target.getSourceObservations())
                .extracting(GradleSourceObservation::getPath)
                .anyMatch(path -> endsWith(path, "application-sources", "main", "java"))
                .anyMatch(path -> endsWith(path, "application-sources", "main", "resources"))
                .anyMatch(path -> endsWith(path, "application-sources", "test", "java"))
                .anyMatch(path -> endsWith(path, "application-sources", "test", "resources"));
    }

    @Test
    void declaredDependencyCollectorOptInFailsActionablyAndDeterministically() throws Exception {
        writeSemanticFixture();

        Supplier<Object> request = () -> request(
                new ToolingApplicationModelAction("DEVELOPMENT"),
                "-PenableDeclaredDependencyCollector=true");

        assertThatThrownBy(request::get)
                .hasStackTraceContaining("enableDeclaredDependencyCollector=true")
                .hasStackTraceContaining("does not support")
                .satisfies(failure -> assertThat(stackTrace(failure))
                        .doesNotContain("configuration cache problems were found")
                        .doesNotContain("isolated projects cannot be enabled"));
        assertThatThrownBy(request::get)
                .hasStackTraceContaining("enableDeclaredDependencyCollector=true")
                .hasStackTraceContaining("legacy 'io.quarkus'")
                .satisfies(failure -> assertThat(stackTrace(failure))
                        .doesNotContain("configuration cache problems were found")
                        .doesNotContain("isolated projects cannot be enabled"));
    }

    private void writeSemanticFixture() throws Exception {
        ToolingSemanticFixture.write(testProjectDir);
    }

    private <T> RequestResult<T> request(BuildAction<T> action, String... extraArguments) {
        ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        List<String> taskPaths = new CopyOnWriteArrayList<>();
        try (ProjectConnection connection = connection()) {
            BuildActionExecuter<T> request = connection.action(action)
                    .withArguments(arguments(extraArguments))
                    .setStandardOutput(standardOutput)
                    .setStandardError(errorOutput);
            request.addProgressListener(event -> {
                if (event instanceof TaskStartEvent taskStart) {
                    taskPaths.add(taskStart.getDescriptor().getTaskPath());
                }
            }, OperationType.TASK);
            T model = request.run();
            return new RequestResult<>(
                    model,
                    standardOutput.toString(UTF_8) + System.lineSeparator() + errorOutput.toString(UTF_8),
                    List.copyOf(taskPaths));
        }
    }

    private ProjectConnection connection() {
        GradleConnector connector = GradleConnector.newConnector()
                .forProjectDirectory(testProjectDir.toFile())
                .useGradleUserHomeDir(GradleUserHomeLookup.gradleUserHome());
        String requestedVersion = System.getProperty("quarkus-test-gradle-wrapper-version");
        if (requestedVersion != null) {
            connector.useGradleVersion(requestedVersion);
        }
        return connector.connect();
    }

    private static String[] arguments(String... extraArguments) {
        return Stream.concat(
                Stream.of(
                        CONFIGURATION_CACHE,
                        ISOLATED_PROJECTS,
                        STACKTRACE,
                        "--info",
                        "-Dorg.gradle.console=plain",
                        "-Dquarkus.analytics.disabled=true"),
                Arrays.stream(extraArguments))
                .toArray(String[]::new);
    }

    private static void assertStoredThenReused(RequestResult<?> first, RequestResult<?> second) {
        assertThat(first.output()).contains("Configuration cache entry stored");
        assertConfigurationCacheReused(second.output());
    }

    private static void assertNoTasks(RequestResult<?>... results) {
        assertThat(results)
                .extracting(result -> result.taskPaths())
                .allSatisfy(taskPaths -> assertThat(taskPaths).as("executed Gradle task paths").isEmpty());
    }

    private static ResolvedDependency dependency(ApplicationModel model, String artifactId) {
        return model.getDependencies().stream()
                .filter(candidate -> candidate.getArtifactId().equals(artifactId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing application-model dependency " + artifactId));
    }

    private static GradleProjectComponent component(GradleApplicationModelSidecar sidecar, String buildPath,
            String projectPath) {
        return sidecar.getProjectComponents().stream()
                .filter(candidate -> candidate.getProjectIdentity().getBuildPath().equals(buildPath))
                .filter(candidate -> candidate.getProjectIdentity().getProjectPath().equals(projectPath))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing sidecar component " + buildPath + '|' + projectPath));
    }

    private static String stackTrace(Throwable failure) {
        StringWriter output = new StringWriter();
        failure.printStackTrace(new PrintWriter(output));
        return output.toString();
    }

    private static boolean endsWith(String path, String first, String... more) {
        return Path.of(path).endsWith(Path.of(first, more));
    }

    private record RequestResult<T>(T model, String output, List<String> taskPaths) {
    }
}
