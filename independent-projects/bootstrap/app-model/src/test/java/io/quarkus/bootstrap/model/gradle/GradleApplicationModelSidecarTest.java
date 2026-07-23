package io.quarkus.bootstrap.model.gradle;

import static io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar.Mode.DEVELOPMENT;
import static io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException.Dimension.GRAPH;
import static io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException.Dimension.MODE;
import static io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException.Dimension.SCHEMA;
import static io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException.Dimension.TARGET;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Kind.CLASSES;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Materialization.UNKNOWN;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.ProducerCategory.STANDARD;
import static io.quarkus.bootstrap.model.gradle.GradleLogicalOutput.Scope.MAIN;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.ClasspathAssociation.RUNTIME;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.GraphRelationship.DIRECT;
import static io.quarkus.bootstrap.model.gradle.GradleProjectComponent.Role.WORKSPACE_DEPENDENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleLogicalOutput;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleModelAssociation;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleModelCorrelation;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleProjectComponent;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleProjectIdentity;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleSourceObservation;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathCollection;
import io.quarkus.paths.PathList;

class GradleApplicationModelSidecarTest {

    private static final List<String> GRAPH_FACTS = List.of(
            "dependency|org.acme:library::jar:1|runtime|/workspace/library/build/classes/java/main",
            "workspace-edge|org.acme:application::jar:1|org.acme:library::jar:1");

    @Test
    void preservesImmutableCompositeIdentitiesAndMultipleOutputsAcrossSerialization() throws Exception {
        final var rootIdentity = new DefaultGradleProjectIdentity(":", ":library", ":library");
        final var includedIdentity = new DefaultGradleProjectIdentity(":included", ":library", ":included:library");
        final var components = new ArrayList<GradleProjectComponent>();
        components.add(component(rootIdentity, "/workspace/library/build/classes/java/main",
                "/workspace/library/build/classes/kotlin/main"));
        components.add(component(includedIdentity, "/workspace/included/library/build/classes/java/main"));

        final var sidecar = sidecar(components, GRAPH_FACTS);
        components.clear();

        assertThat(sidecar.getProjectComponents()).hasSize(2);
        assertThat(sidecar.getProjectComponents()).extracting(c -> c.getProjectIdentity().getProjectPath())
                .containsOnly(":library");
        assertThat(sidecar.getProjectComponents()).extracting(c -> c.getProjectIdentity().getBuildTreePath())
                .containsExactly(":library", ":included:library");
        assertThat(sidecar.getProjectComponents().get(0).getLogicalOutputs()).hasSize(2);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> sidecar.getProjectComponents().clear());

        final GradleApplicationModelSidecar copy = roundTrip(sidecar);
        assertThat(copy.getProjectComponents()).hasSize(2);
        assertThat(copy.getProjectComponents().get(0).getLogicalOutputs())
                .extracting(GradleLogicalOutput::getPath)
                .containsExactly("/workspace/library/build/classes/java/main",
                        "/workspace/library/build/classes/kotlin/main");
        assertThat(copy.getCorrelation().getCanonicalGraphFacts()).containsExactlyElementsOf(GRAPH_FACTS);
    }

    @Test
    void keepsUnknownSemanticsAndAmbiguousAssociationsExplicit() {
        final var identity = new DefaultGradleProjectIdentity(":", ":library", ":library");
        final var source = new DefaultGradleSourceObservation("/workspace/library/src/main",
                GradleSourceObservation.Role.UNKNOWN, null);
        final var output = output("classes-0", "/workspace/library/build/classes/java/main");
        final var component = new DefaultGradleProjectComponent(identity, "project :library",
                Set.of(WORKSPACE_DEPENDENCY), Set.of(RUNTIME), Set.of(DIRECT), List.of(source), List.of(output));

        assertThat(source.getRole()).isEqualTo(GradleSourceObservation.Role.UNKNOWN);
        assertThat(source.hasLogicalOutputAssociation()).isFalse();
        assertThat(output.getSourceSet()).isNull();
        assertThat(output.getJvmFeature()).isNull();
        assertThat(output.getClassifier()).isNull();
        assertThat(output.getMaterialization()).isEqualTo(UNKNOWN);
        assertThat(output.getModelAssociation().getKind()).isEqualTo(GradleModelAssociation.Kind.UNKNOWN);
        assertThat(output.getModelAssociation().isEligibleForOverlayReplacement()).isFalse();
        assertThat(component.getSourceObservations()).hasSize(1);
        assertThat(component.getLogicalOutputs()).hasSize(1);
    }

    @Test
    void canonicalizesCorrelationAndAcceptsMatchingDimensions() {
        final var reversedFacts = List.of(GRAPH_FACTS.get(1), GRAPH_FACTS.get(0));
        final var sidecar = sidecar(List.of(), reversedFacts);

        assertThat(sidecar.getCorrelation().getCanonicalGraphFacts()).containsExactlyElementsOf(GRAPH_FACTS);
        GradleApplicationModelSidecarValidator.validate(sidecar,
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION, DEVELOPMENT, ":application", reversedFacts);
    }

    @Test
    void reportsTheMismatchedCorrelationDimension() {
        final var sidecar = sidecar(List.of(), GRAPH_FACTS);

        assertMismatch(sidecar, 2, DEVELOPMENT, ":application", GRAPH_FACTS, SCHEMA);
        assertMismatch(sidecar, 1, GradleApplicationModelSidecar.Mode.TEST, ":application", GRAPH_FACTS, MODE);
        assertMismatch(sidecar, 1, DEVELOPMENT, ":other", GRAPH_FACTS, TARGET);
        assertMismatch(sidecar, 1, DEVELOPMENT, ":application", List.of("different"), GRAPH);
    }

    @Test
    void buildsDeterministicCanonicalFactsFromTheApplicationModel() {
        final Path applicationResources = Path.of("/workspace/application/build/resources/main");
        final Path applicationClasses = Path.of("/workspace/application/build/classes/java/main");
        final Path zLibrary = Path.of("/workspace/z-library.jar");
        final Path aLibraryClasses = Path.of("/workspace/a-library/build/classes/java/main");
        final var model = new ApplicationModelBuilder()
                .setAppArtifact(dependency("org.acme", "application", 0,
                        applicationResources,
                        applicationClasses))
                .addDependency(dependency("org.acme", "z-library",
                        DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP,
                        zLibrary))
                .addDependency(dependency("org.acme", "a-library",
                        DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP | DependencyFlags.DIRECT,
                        aLibraryClasses))
                .build();

        assertThat(GradleModelCorrelationSupport.canonicalGraphFacts(model)).containsExactly(
                "application|31:org.acme:application::jar:1.0.0|0|" + encodedPath(applicationResources),
                "application|31:org.acme:application::jar:1.0.0|0|" + encodedPath(applicationClasses),
                "dependency|29:org.acme:a-library::jar:1.0.0|"
                        + (DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP | DependencyFlags.DIRECT)
                        + "|" + encodedPath(aLibraryClasses),
                "dependency|29:org.acme:z-library::jar:1.0.0|"
                        + (DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP)
                        + "|" + encodedPath(zLibrary));
    }

    @Test
    void canonicalFactsIncludeWorkspaceDirectEdges() {
        final WorkspaceModule.Mutable applicationWorkspace = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "application", "1.0.0"))
                .addDependency(Dependency.of("org.acme", "library", "1.0.0"));
        final WorkspaceModule.Mutable libraryWorkspace = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "library", "1.0.0"))
                .addDependency(Dependency.of("org.acme", "transitive", "1.0.0"));
        final var model = new ApplicationModelBuilder()
                .setAppArtifact(dependency("org.acme", "application", 0)
                        .setWorkspaceModule(applicationWorkspace))
                .addDependency(dependency("org.acme", "library",
                        DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP)
                        .setWorkspaceModule(libraryWorkspace))
                .build();

        assertThat(GradleModelCorrelationSupport.canonicalGraphFacts(model))
                .contains(
                        "workspace-edge|26:org.acme:application:1.0.0|27:org.acme:library::jar:1.0.0",
                        "workspace-edge|22:org.acme:library:1.0.0|30:org.acme:transitive::jar:1.0.0");
    }

    @Test
    void validatesToolingProxyPathsWithoutIteration() {
        final Path applicationPath = Path.of("/workspace/application/build/classes/java/main");
        final Path libraryPath = Path.of("/workspace/library/build/classes/java/main");
        final var canonicalModel = correlatedModel(
                PathList.of(applicationPath), "library",
                DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP,
                PathList.of(libraryPath), "library");
        final var proxyModel = correlatedModel(
                new NonIterablePathCollection(applicationPath), "library",
                DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP,
                new NonIterablePathCollection(libraryPath), "library");
        final var sidecar = sidecar(List.of(), GradleModelCorrelationSupport.canonicalGraphFacts(canonicalModel));

        assertThatCode(() -> GradleApplicationModelSidecarValidator.validate(sidecar,
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION, DEVELOPMENT, ":application", proxyModel))
                .doesNotThrowAnyException();
    }

    @Test
    void proxySafeValidationDetectsCoordinateFlagPathAndWorkspaceEdgeMismatches() {
        final Path applicationPath = Path.of("/workspace/application/build/classes/java/main");
        final Path libraryPath = Path.of("/workspace/library/build/classes/java/main");
        final int flags = DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP;
        final var canonicalModel = correlatedModel(
                PathList.of(applicationPath), "library", flags, PathList.of(libraryPath), "library");
        final var sidecar = sidecar(List.of(), GradleModelCorrelationSupport.canonicalGraphFacts(canonicalModel));

        assertApplicationModelMismatch(sidecar,
                correlatedModel(new NonIterablePathCollection(applicationPath), "otherlib", flags,
                        new NonIterablePathCollection(libraryPath), "library"));
        assertApplicationModelMismatch(sidecar,
                correlatedModel(new NonIterablePathCollection(applicationPath), "library",
                        flags | DependencyFlags.DIRECT, new NonIterablePathCollection(libraryPath), "library"));
        assertApplicationModelMismatch(sidecar,
                correlatedModel(new NonIterablePathCollection(applicationPath), "library", flags,
                        new NonIterablePathCollection(Path.of("/workspace/library/build/other")), "library"));
        assertApplicationModelMismatch(sidecar,
                correlatedModel(new NonIterablePathCollection(applicationPath), "library", flags,
                        new NonIterablePathCollection(libraryPath), "otherlib"));
    }

    private static ApplicationModel correlatedModel(
            PathCollection applicationPaths, String libraryArtifactId, int libraryFlags,
            PathCollection libraryPaths, String applicationEdgeArtifactId) {
        final WorkspaceModule.Mutable applicationWorkspace = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", "application", "1.0.0"))
                .addDependency(Dependency.of("org.acme", applicationEdgeArtifactId, "1.0.0"));
        final WorkspaceModule.Mutable libraryWorkspace = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.acme", libraryArtifactId, "1.0.0"));
        return new ApplicationModelBuilder()
                .setAppArtifact(dependency("org.acme", "application", 0)
                        .setResolvedPaths(applicationPaths)
                        .setWorkspaceModule(applicationWorkspace))
                .addDependency(dependency("org.acme", libraryArtifactId, libraryFlags)
                        .setResolvedPaths(libraryPaths)
                        .setWorkspaceModule(libraryWorkspace))
                .build();
    }

    private static void assertApplicationModelMismatch(GradleApplicationModelSidecar sidecar,
            ApplicationModel model) {
        assertThatExceptionOfType(GradleApplicationModelSidecarMismatchException.class)
                .isThrownBy(() -> GradleApplicationModelSidecarValidator.validate(sidecar,
                        GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION, DEVELOPMENT, ":application", model))
                .satisfies(failure -> assertThat(failure.getDimension()).isEqualTo(GRAPH));
    }

    private static String encodedPath(Path path) {
        final String value = path.toString();
        return value.length() + ":" + value;
    }

    private static DefaultGradleApplicationModelSidecar sidecar(
            List<? extends GradleProjectComponent> components, List<String> graphFacts) {
        final var target = new DefaultGradleProjectIdentity(":", ":application", ":application");
        final var correlation = new DefaultGradleModelCorrelation(
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION, DEVELOPMENT, target.getBuildTreePath(), graphFacts);
        return new DefaultGradleApplicationModelSidecar(correlation, target, components);
    }

    private static DefaultGradleProjectComponent component(GradleProjectIdentity identity, String... outputPaths) {
        final var outputs = new ArrayList<GradleLogicalOutput>(outputPaths.length);
        for (int i = 0; i < outputPaths.length; i++) {
            outputs.add(output("classes-" + i, outputPaths[i]));
        }
        return new DefaultGradleProjectComponent(identity, "project " + identity.getBuildTreePath(),
                Set.of(WORKSPACE_DEPENDENCY), Set.of(RUNTIME), Set.of(DIRECT), List.of(), outputs);
    }

    private static DefaultGradleLogicalOutput output(String identity, String path) {
        return new DefaultGradleLogicalOutput(identity, CLASSES, MAIN, path, "classes-directory",
                null, null, null, null, UNKNOWN, STANDARD, DefaultGradleModelAssociation.unknown());
    }

    private static ResolvedDependencyBuilder dependency(String groupId, String artifactId, int flags, Path... paths) {
        return ResolvedDependencyBuilder.newInstance()
                .setCoords(ArtifactCoords.jar(groupId, artifactId, "1.0.0"))
                .setFlags(flags)
                .setResolvedPaths(PathList.of(paths));
    }

    private static void assertMismatch(GradleApplicationModelSidecar sidecar, int schema,
            GradleApplicationModelSidecar.Mode mode, String target, List<String> facts,
            GradleApplicationModelSidecarMismatchException.Dimension dimension) {
        assertThatExceptionOfType(GradleApplicationModelSidecarMismatchException.class)
                .isThrownBy(() -> GradleApplicationModelSidecarValidator.validate(sidecar, schema, mode, target, facts))
                .satisfies(failure -> {
                    assertThat(failure.getDimension()).isEqualTo(dimension);
                    assertThat(failure.getMessage()).contains(dimension.name().toLowerCase(), "expected", "but was");
                });
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws IOException, ClassNotFoundException {
        final byte[] serialized;
        try (var bytes = new ByteArrayOutputStream(); var output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
            serialized = bytes.toByteArray();
        }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            return (T) input.readObject();
        }
    }

    private static final class NonIterablePathCollection implements PathCollection {

        private final List<Path> paths;

        private NonIterablePathCollection(Path... paths) {
            this.paths = List.of(paths);
        }

        @Override
        public boolean isEmpty() {
            return paths.isEmpty();
        }

        @Override
        public int size() {
            return paths.size();
        }

        @Override
        public boolean isSinglePath() {
            return paths.size() == 1;
        }

        @Override
        public boolean contains(Path path) {
            return paths.contains(path);
        }

        @Override
        public PathCollection add(Path... paths) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PathCollection addFirst(Path... paths) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PathCollection addAllFirst(Iterable<Path> paths) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path resolveExistingOrNull(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Path> iterator() {
            throw new UnsupportedOperationException("Tooling API proxy does not support PathCollection iteration");
        }
    }
}
