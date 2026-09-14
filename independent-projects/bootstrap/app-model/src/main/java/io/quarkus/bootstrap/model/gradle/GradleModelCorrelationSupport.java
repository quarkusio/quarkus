package io.quarkus.bootstrap.model.gradle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.paths.PathCollection;

/**
 * Produces the neutral application-model projection used to correlate a
 * Gradle sidecar with its paired model.
 */
public final class GradleModelCorrelationSupport {

    private GradleModelCorrelationSupport() {
    }

    /**
     * Returns deterministic facts for the application and every dependency,
     * including coordinates, flags, every exact resolved-path occurrence, and
     * workspace-module direct edges.
     */
    public static List<String> canonicalGraphFacts(ApplicationModel model) {
        final List<String> facts = new ArrayList<>();
        addFacts(facts, "application", model.getAppArtifact());
        addWorkspaceEdges(facts, model.getApplicationModule());
        for (ResolvedDependency dependency : model.getDependencies()) {
            addFacts(facts, "dependency", dependency);
            addWorkspaceEdges(facts, dependency.getWorkspaceModule());
        }
        facts.sort(Comparator.naturalOrder());
        return List.copyOf(facts);
    }

    private static void addFacts(List<String> facts, String entryKind, ResolvedDependency dependency) {
        final PathCollection resolvedPaths = dependency.getResolvedPaths();
        if (resolvedPaths == null || resolvedPaths.isEmpty()) {
            facts.add(fact(entryKind, dependency, ""));
            return;
        }
        for (Path path : resolvedPaths) {
            facts.add(fact(entryKind, dependency, path.toString()));
        }
    }

    private static String fact(String entryKind, ResolvedDependency dependency, String resolvedPath) {
        final String coordinates = dependency.toGACTVString();
        return entryKind + '|' + coordinates.length() + ':' + coordinates + '|' + dependency.getFlags() + '|'
                + resolvedPath.length() + ':' + resolvedPath;
    }

    private static void addWorkspaceEdges(List<String> facts, WorkspaceModule module) {
        if (module == null) {
            return;
        }
        final String source = module.getId().toString();
        for (Dependency dependency : module.getDirectDependencies()) {
            final String target = dependency.toGACTVString();
            facts.add("workspace-edge|" + source.length() + ':' + source + '|' + target.length() + ':' + target);
        }
    }
}
