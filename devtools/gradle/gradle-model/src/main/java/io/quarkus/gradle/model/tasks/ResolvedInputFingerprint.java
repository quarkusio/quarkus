package io.quarkus.gradle.model.tasks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;

/**
 * Canonical scalar task inputs for Gradle resolution results consumed while assembling an application model.
 * <p>
 * The returned strings deliberately describe resolution identity rather than artifact contents; task types must declare
 * the corresponding files separately. Collections are sorted so their value is independent of Gradle set/graph
 * traversal order. This is plugin implementation API and the textual representation is not a persisted interchange
 * format.
 */
public final class ResolvedInputFingerprint {

    private ResolvedInputFingerprint() {
    }

    /**
     * Returns sorted fingerprints of component identity, normalized absolute file path, file name, and artifact type.
     *
     * @param artifacts resolved artifacts to normalize
     * @return immutable, sorted fingerprints
     */
    public static List<String> artifactMetadata(Set<ResolvedArtifactResult> artifacts) {
        return artifacts.stream()
                .map(ResolvedInputFingerprint::artifactMetadata)
                .sorted()
                .toList();
    }

    /**
     * Returns a sorted representation of every reachable component and dependency edge.
     * <p>
     * Resolved edges record the selected component; unresolved edges record the request and unresolved kind.
     *
     * @param root root of the Gradle resolution graph
     * @return immutable, sorted component and edge fingerprints
     */
    public static List<String> resolutionGraph(ResolvedComponentResult root) {
        List<String> graph = new ArrayList<>();
        Set<ComponentIdentifier> visited = new HashSet<>();
        ArrayDeque<ResolvedComponentResult> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            ResolvedComponentResult component = pending.removeFirst();
            if (!visited.add(component.getId())) {
                continue;
            }
            graph.add(componentIdentity(component.getId()));
            for (DependencyResult dependency : component.getDependencies()) {
                Map<String, String> values = new HashMap<>();
                values.put("from", componentIdentity(component.getId()));
                values.put("requested", dependency.getRequested().getDisplayName());
                if (dependency instanceof ResolvedDependencyResult resolved) {
                    values.put("kind", "resolved");
                    values.put("selected", componentIdentity(resolved.getSelected().getId()));
                    pending.add(resolved.getSelected());
                } else {
                    values.put("kind", "unresolved");
                }
                graph.add(TaskInputFingerprint.ofMap(values));
            }
        }
        graph.sort(String::compareTo);
        return List.copyOf(graph);
    }

    private static String artifactMetadata(ResolvedArtifactResult artifact) {
        Map<String, String> values = new HashMap<>();
        addComponentIdentity(values, "component.", artifact.getId().getComponentIdentifier());
        values.put("artifact.file", artifact.getFile().toPath().toAbsolutePath().normalize().toString());
        values.put("artifact.file-name", artifact.getFile().getName());
        String artifactType = artifact.getVariant().getAttributes()
                .getAttribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE);
        values.put("artifact.type", artifactType == null ? "" : artifactType);
        return TaskInputFingerprint.ofMap(values);
    }

    private static String componentIdentity(ComponentIdentifier component) {
        Map<String, String> values = new HashMap<>();
        addComponentIdentity(values, "", component);
        return TaskInputFingerprint.ofMap(values);
    }

    private static void addComponentIdentity(Map<String, String> values, String prefix, ComponentIdentifier component) {
        if (component instanceof ModuleComponentIdentifier module) {
            values.put(prefix + "kind", "module");
            values.put(prefix + "group", module.getGroup());
            values.put(prefix + "module", module.getModule());
            values.put(prefix + "version", module.getVersion());
        } else if (component instanceof ProjectComponentIdentifier project) {
            values.put(prefix + "kind", "project");
            values.put(prefix + "build-tree-path", project.getBuildTreePath());
        } else {
            values.put(prefix + "kind", "opaque");
            values.put(prefix + "display-name", component.getDisplayName());
        }
    }
}
