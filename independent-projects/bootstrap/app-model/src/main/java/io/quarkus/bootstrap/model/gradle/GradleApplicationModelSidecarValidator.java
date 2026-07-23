package io.quarkus.bootstrap.model.gradle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException.Dimension;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;

/**
 * Validates the externally known dimensions of a paired application model and
 * Gradle sidecar.
 */
public final class GradleApplicationModelSidecarValidator {

    private GradleApplicationModelSidecarValidator() {
    }

    public static void validate(GradleApplicationModelSidecar sidecar, int expectedSchemaVersion,
            GradleApplicationModelSidecar.Mode expectedMode, String expectedTargetBuildTreePath,
            Collection<String> expectedCanonicalGraphFacts) {
        Objects.requireNonNull(expectedCanonicalGraphFacts, "expectedCanonicalGraphFacts");
        final GradleModelCorrelation correlation = validateDimensions(sidecar, expectedSchemaVersion, expectedMode,
                expectedTargetBuildTreePath);
        final List<String> expectedGraph = canonicalGraph(expectedCanonicalGraphFacts);
        if (!expectedGraph.equals(correlation.getCanonicalGraphFacts())) {
            throw mismatch(Dimension.GRAPH, expectedGraph, correlation.getCanonicalGraphFacts());
        }
    }

    /**
     * Validates a sidecar against an application model obtained through the
     * Tooling API.
     * <p>
     * Gradle adapts custom model interfaces to protocol proxies. In
     * particular, {@link PathCollection#iterator()} and
     * {@link PathCollection#spliterator()} are not supported by that adapter.
     * This validation path therefore compares every sidecar path with
     * {@link PathCollection#contains(Path)} and verifies the collection size,
     * without attempting to iterate it.
     */
    public static void validate(GradleApplicationModelSidecar sidecar, int expectedSchemaVersion,
            GradleApplicationModelSidecar.Mode expectedMode, String expectedTargetBuildTreePath,
            ApplicationModel expectedApplicationModel) {
        Objects.requireNonNull(expectedApplicationModel, "expectedApplicationModel");
        final GradleModelCorrelation correlation = validateDimensions(sidecar, expectedSchemaVersion, expectedMode,
                expectedTargetBuildTreePath);
        validateApplicationModelGraph(correlation.getCanonicalGraphFacts(), expectedApplicationModel);
    }

    private static GradleModelCorrelation validateDimensions(GradleApplicationModelSidecar sidecar,
            int expectedSchemaVersion, GradleApplicationModelSidecar.Mode expectedMode,
            String expectedTargetBuildTreePath) {
        Objects.requireNonNull(sidecar, "sidecar");
        Objects.requireNonNull(expectedMode, "expectedMode");
        Objects.requireNonNull(expectedTargetBuildTreePath, "expectedTargetBuildTreePath");

        final GradleModelCorrelation correlation = Objects.requireNonNull(sidecar.getCorrelation(), "sidecar.correlation");
        if (correlation.getSchemaVersion() != expectedSchemaVersion) {
            throw mismatch(Dimension.SCHEMA, expectedSchemaVersion, correlation.getSchemaVersion());
        }
        if (correlation.getMode() != expectedMode) {
            throw mismatch(Dimension.MODE, expectedMode, correlation.getMode());
        }
        if (!expectedTargetBuildTreePath.equals(correlation.getTargetBuildTreePath())) {
            throw mismatch(Dimension.TARGET, expectedTargetBuildTreePath, correlation.getTargetBuildTreePath());
        }
        if (!correlation.getTargetBuildTreePath().equals(sidecar.getTargetProject().getBuildTreePath())) {
            throw mismatch(Dimension.TARGET, sidecar.getTargetProject().getBuildTreePath(),
                    correlation.getTargetBuildTreePath());
        }
        return correlation;
    }

    private static void validateApplicationModelGraph(List<String> actualFacts, ApplicationModel model) {
        final List<String> canonicalActualFacts = canonicalGraph(actualFacts);
        if (!canonicalActualFacts.equals(actualFacts)) {
            throw mismatch(Dimension.GRAPH, canonicalActualFacts, actualFacts);
        }

        final Map<EntryKey, List<String>> entries = new LinkedHashMap<>();
        final List<WorkspaceEdge> edges = new ArrayList<>();
        try {
            for (String fact : actualFacts) {
                if (fact.startsWith("workspace-edge|")) {
                    edges.add(parseWorkspaceEdge(fact));
                } else {
                    final EntryFact entry = parseEntryFact(fact);
                    entries.computeIfAbsent(entry.key(), ignored -> new ArrayList<>()).add(entry.resolvedPath());
                }
            }
        } catch (RuntimeException e) {
            throw mismatch(Dimension.GRAPH, "well-formed canonical graph facts", actualFacts);
        }

        validateEntry(entries, "application", model.getAppArtifact());
        validateWorkspaceEdges(edges, model.getAppArtifact().getWorkspaceModule());
        for (ResolvedDependency dependency : model.getDependencies()) {
            validateEntry(entries, "dependency", dependency);
            validateWorkspaceEdges(edges, dependency.getWorkspaceModule());
        }
        if (!entries.isEmpty() || !edges.isEmpty()) {
            throw mismatch(Dimension.GRAPH, "no unmatched graph facts",
                    new GraphRemainder(entries, edges));
        }
    }

    private static void validateEntry(Map<EntryKey, List<String>> entries, String kind,
            ResolvedDependency dependency) {
        final EntryKey key = new EntryKey(kind, coordinates(dependency), dependency.getFlags());
        final List<String> paths = entries.remove(key);
        if (paths == null) {
            throw mismatch(Dimension.GRAPH, key, "missing");
        }

        final PathCollection resolvedPaths = dependency.getResolvedPaths();
        final int resolvedPathCount = resolvedPaths == null ? 0 : resolvedPaths.size();
        if (resolvedPathCount == 0) {
            if (!paths.equals(List.of(""))) {
                throw mismatch(Dimension.GRAPH, List.of(""), paths);
            }
            return;
        }
        if (paths.size() != resolvedPathCount || paths.stream().anyMatch(String::isEmpty)
                || paths.stream().distinct().count() != paths.size()) {
            throw mismatch(Dimension.GRAPH, resolvedPathCount + " distinct non-empty resolved paths", paths);
        }
        for (String path : paths) {
            if (!resolvedPaths.contains(Path.of(path))) {
                throw mismatch(Dimension.GRAPH, "application-model path", path);
            }
        }
    }

    private static void validateWorkspaceEdges(List<WorkspaceEdge> edges, WorkspaceModule module) {
        if (module == null) {
            return;
        }
        final String source = moduleId(module);
        for (Dependency dependency : module.getDirectDependencies()) {
            final WorkspaceEdge edge = new WorkspaceEdge(source, coordinates(dependency));
            if (!edges.remove(edge)) {
                throw mismatch(Dimension.GRAPH, edge, "missing");
            }
        }
    }

    private static EntryFact parseEntryFact(String fact) {
        int kindEnd = fact.indexOf('|');
        if (kindEnd <= 0) {
            throw new IllegalArgumentException("Missing entry kind");
        }
        final String kind = fact.substring(0, kindEnd);
        if (!kind.equals("application") && !kind.equals("dependency")) {
            throw new IllegalArgumentException("Unknown entry kind");
        }
        final LengthPrefixed coordinates = lengthPrefixed(fact, kindEnd + 1);
        if (coordinates.end() >= fact.length() || fact.charAt(coordinates.end()) != '|') {
            throw new IllegalArgumentException("Missing flags");
        }
        final int flagsEnd = fact.indexOf('|', coordinates.end() + 1);
        if (flagsEnd < 0) {
            throw new IllegalArgumentException("Missing resolved path");
        }
        final int flags = Integer.parseInt(fact.substring(coordinates.end() + 1, flagsEnd));
        final LengthPrefixed path = lengthPrefixed(fact, flagsEnd + 1);
        if (path.end() != fact.length()) {
            throw new IllegalArgumentException("Unexpected entry suffix");
        }
        return new EntryFact(new EntryKey(kind, coordinates.value(), flags), path.value());
    }

    private static WorkspaceEdge parseWorkspaceEdge(String fact) {
        final int prefixLength = "workspace-edge|".length();
        final LengthPrefixed source = lengthPrefixed(fact, prefixLength);
        if (source.end() >= fact.length() || fact.charAt(source.end()) != '|') {
            throw new IllegalArgumentException("Missing workspace edge target");
        }
        final LengthPrefixed target = lengthPrefixed(fact, source.end() + 1);
        if (target.end() != fact.length()) {
            throw new IllegalArgumentException("Unexpected workspace edge suffix");
        }
        return new WorkspaceEdge(source.value(), target.value());
    }

    private static LengthPrefixed lengthPrefixed(String value, int offset) {
        final int colon = value.indexOf(':', offset);
        if (colon < 0) {
            throw new IllegalArgumentException("Missing length separator");
        }
        final int length = Integer.parseInt(value.substring(offset, colon));
        final int start = colon + 1;
        final int end = start + length;
        if (length < 0 || end > value.length()) {
            throw new IllegalArgumentException("Invalid length");
        }
        return new LengthPrefixed(value.substring(start, end), end);
    }

    private static String coordinates(ArtifactCoords dependency) {
        return dependency.getGroupId() + ':' + dependency.getArtifactId() + ':' + dependency.getClassifier() + ':'
                + dependency.getType() + ':' + dependency.getVersion();
    }

    private static String moduleId(WorkspaceModule module) {
        return module.getId().getGroupId() + ':' + module.getId().getArtifactId() + ':' + module.getId().getVersion();
    }

    private static List<String> canonicalGraph(Collection<String> facts) {
        final List<String> canonical = new ArrayList<>(facts);
        canonical.sort(Comparator.naturalOrder());
        return List.copyOf(canonical);
    }

    private static GradleApplicationModelSidecarMismatchException mismatch(Dimension dimension, Object expected,
            Object actual) {
        return new GradleApplicationModelSidecarMismatchException(dimension, expected, actual);
    }

    private record EntryKey(String kind, String coordinates, int flags) {
    }

    private record EntryFact(EntryKey key, String resolvedPath) {
    }

    private record WorkspaceEdge(String source, String target) {
    }

    private record LengthPrefixed(String value, int end) {
    }

    private record GraphRemainder(Map<EntryKey, List<String>> entries, List<WorkspaceEdge> edges) {
    }
}
