package io.quarkus.devui.deployment.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devui.deployment.InternalPageBuildItem;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.smallrye.config.DefaultValuesConfigSource;

public class DependenciesProcessor {

    private static final String NAMESPACE = "devui-dependencies";

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createAppDeps(BuildProducer<InternalPageBuildItem> menuProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {

        // Menu

        if (isEnabled()) {

            InternalPageBuildItem page = new InternalPageBuildItem("Dependencies", 70);

            page.addPage(Page.webComponentPageBuilder()
                    .namespace(NAMESPACE)
                    .icon("font-awesome-solid:diagram-project")
                    .title("Application Dependencies")
                    .componentLink("qwc-dependencies.js"));

            Root root = new Root();
            root.rootId = curateOutcomeBuildItem.getApplicationModel().getAppArtifact().toCompactCoords();
            Set<String> allGavs = new TreeSet<>();
            buildTree(curateOutcomeBuildItem.getApplicationModel(), root, Optional.of(allGavs), Optional.empty());

            page.addBuildTimeData("root", root);
            page.addBuildTimeData("allGavs", allGavs);

            menuProducer.produce(page);
        }
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void createBuildTimeActions(BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (isEnabled()) {
            BuildTimeActionBuildItem pathToTargetAction = new BuildTimeActionBuildItem(NAMESPACE);
            pathToTargetAction.addAction("pathToTarget", p -> {
                String target = p.get("target");
                Root root = new Root();
                root.rootId = curateOutcomeBuildItem.getApplicationModel().getAppArtifact().toCompactCoords();

                if (target == null || target.isBlank()) {
                    buildTree(curateOutcomeBuildItem.getApplicationModel(), root, Optional.empty(), Optional.empty());
                } else {
                    buildTree(curateOutcomeBuildItem.getApplicationModel(), root, Optional.empty(), Optional.of(target));
                }

                return root;
            });

            buildTimeActionProducer.produce(pathToTargetAction);
        }
    }

    private boolean isEnabled() {
        var value = ConfigProvider.getConfig().getConfigValue("quarkus.bootstrap.incubating-model-resolver");
        // if it's not false and if it's false it doesn't come from the default value
        return value == null || !"false".equals(value.getValue())
                || DefaultValuesConfigSource.NAME.equals(value.getSourceName());
    }

    private void buildTree(ApplicationModel model, Root root, Optional<Set<String>> allGavs, Optional<String> toTarget) {
        final Collection<ResolvedDependency> resolvedDeps = model.getDependencies();
        final List<Node> nodes = new ArrayList<>(resolvedDeps.size());
        final List<Link> links = new ArrayList<>();

        if (toTarget.isEmpty()) {

            addDependency(model.getAppArtifact(), root, nodes, links, allGavs);
            for (ResolvedDependency rd : resolvedDeps) {
                addDependency(rd, root, nodes, links, allGavs);
            }
        } else {
            DepNode targetDep = getTargetDepNode(model, ArtifactCoords.fromString(toTarget.get()));
            addDependency(targetDep, root, nodes, links, allGavs, new HashSet<>());
        }

        root.nodes = nodes;
        root.links = links;
    }

    private static void addDependency(ResolvedDependency rd, Root root, List<Node> nodes, List<Link> links,
            Optional<Set<String>> allGavs) {
        Node node = new Node();

        if (allGavs.isPresent()) {
            allGavs.get().add(rd.toCompactCoords());
        }

        node.id = rd.toCompactCoords();
        node.name = rd.getArtifactId();
        node.description = rd.toCompactCoords();
        nodes.add(node);

        String type = rd.isRuntimeCp() ? "runtime" : "deployment";

        for (ArtifactCoords dep : rd.getDependencies()) {
            // this needs to be improved, these artifacts shouldn't even be mentioned among the dependencies
            if ("quarkus-ide-launcher".equals(dep.getArtifactId())
                    || "javax.annotation-api".equals(dep.getArtifactId())) {
                continue;
            }
            Link link = new Link();
            link.source = node.id;
            link.target = dep.toCompactCoords();
            link.type = type;
            link.direct = (link.source == root.rootId);
            links.add(link);
        }
    }

    private static void addDependency(DepNode dep, Root root, List<Node> nodes, List<Link> links, Optional<Set<String>> allGavs,
            Set<String> visited) {
        String id = dep.resolvedDep.toCompactCoords();
        if (!visited.add(id)) {
            return;
        }
        var rd = dep.resolvedDep;

        if (allGavs.isPresent()) {
            allGavs.get().add(rd.toCompactCoords());
        }

        Node node = new Node();
        node.id = id;
        node.name = rd.getArtifactId();
        node.description = id;
        nodes.add(node);

        for (DepNode dependent : dep.dependents) {
            addDependency(dependent, root, nodes, links, allGavs, visited);
            Link link = new Link();
            link.source = dependent.resolvedDep.toCompactCoords();
            link.target = node.id;
            link.type = dependent.resolvedDep.isRuntimeCp() ? "runtime" : "deployment";
            link.direct = (link.source == root.rootId);
            links.add(link);
        }
    }

    static class Root {
        public String rootId;
        public List<Node> nodes;
        public List<Link> links;
    }

    static class Node {
        public String id;
        public String name;
        public int value = 1;
        public String description;

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + Objects.hashCode(this.id);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Node other = (Node) obj;
            return Objects.equals(this.id, other.id);
        }
    }

    static class Link {
        public String source;
        public String target;
        public String type;
        public boolean direct = false;

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.source);
            hash = 97 * hash + Objects.hashCode(this.target);
            hash = 97 * hash + Objects.hashCode(this.type);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Link other = (Link) obj;
            if (!Objects.equals(this.source, other.source)) {
                return false;
            }
            if (!Objects.equals(this.target, other.target)) {
                return false;
            }
            return Objects.equals(this.type, other.type);
        }
    }

    private static DepNode getTargetDepNode(ApplicationModel model, ArtifactCoords targetCoords) {
        DepNode targetDep = getTargetWithAllDependents(targetCoords, getCompleteMap(model));
        // It seems like all the possible paths look like a lot in many cases.
        // Common shortest paths is an attempt to reduce the number of paths displayed
        // by selecting all the unique shortest paths
        final boolean showShortestCommonPaths = false;
        if (showShortestCommonPaths) {
            final List<DepPath> paths = collectAllPaths(targetDep);
            filterShortestCommonPaths(paths);
            return getGraph(paths);
        }
        return targetDep;
    }

    private static DepNode getGraph(List<DepPath> paths) {
        final DepNode newTarget = new DepNode(paths.get(0).path.get(0).id, paths.get(0).path.get(0).resolvedDep);
        final Map<ArtifactKey, DepNode> nodes = new HashMap<>();
        for (var path : paths) {
            var currentNode = newTarget;
            for (int i = 1; i < path.path.size(); ++i) {
                var originalNode = path.path.get(i);
                var newNode = nodes.computeIfAbsent(originalNode.resolvedDep.getKey(),
                        k -> new DepNode(originalNode.id, originalNode.resolvedDep));
                currentNode.addDependent(newNode);
                currentNode = newNode;
            }
        }
        return newTarget;
    }

    private static void filterShortestCommonPaths(List<DepPath> paths) {
        int i = 0;
        while (i < paths.size()) {
            var currentPath = paths.get(i++);
            int j = i;
            while (j < paths.size()) {
                if (paths.get(j).ids.containsAll(currentPath.ids)) {
                    paths.remove(j);
                } else {
                    ++j;
                }
            }
        }
        logPaths(paths, "Shortest common paths");
    }

    private static List<DepPath> collectAllPaths(DepNode targetDep) {
        final List<DepPath> allPaths = new ArrayList<>();
        collectAllPaths(targetDep, new DepPath(), allPaths);
        Collections.sort(allPaths);
        logPaths(allPaths, "Paths");
        return allPaths;
    }

    private static void logPaths(List<DepPath> paths, String title) {
        System.out.println(title + " total: " + paths.size());
        int j = 1;
        for (var dp : paths) {
            System.out.println(j++ + ") " + dp);
        }
    }

    private static void collectAllPaths(DepNode node, DepPath currentPath, List<DepPath> allPaths) {
        if (!currentPath.addNode(node)) {
            return;
        }
        if (node.id == 0) {
            allPaths.add(currentPath);
            return;
        }
        if (node.dependents.isEmpty()) {
            System.out.println(node.resolvedDep.getArtifactId() + " has no dependents");
            return;
        }
        for (int i = 1; i < node.dependents.size(); ++i) {
            collectAllPaths(node.dependents.get(i), currentPath.clone(), allPaths);
        }
        collectAllPaths(node.dependents.get(0), currentPath, allPaths);
    }

    private static DepNode getTargetWithAllDependents(ArtifactCoords targetCoords, Map<ArtifactKey, DepNode> all) {
        DepNode targetDep = null;
        for (var d : all.values()) {
            d.initDependents(all);
            if (targetDep == null
                    && d.resolvedDep.getArtifactId().equals(targetCoords.getArtifactId())
                    && d.resolvedDep.getGroupId().equals(targetCoords.getGroupId())
                    && d.resolvedDep.getClassifier().equals(targetCoords.getClassifier())
                    && d.resolvedDep.getType().equals(targetCoords.getType())
                    && d.resolvedDep.getVersion().equals(targetCoords.getVersion())) {
                targetDep = d;
            }
        }
        if (targetDep == null) {
            throw new IllegalArgumentException(
                    "Failed to locate " + targetCoords.toCompactCoords() + " among the dependencies");
        }
        return targetDep;
    }

    private static Map<ArtifactKey, DepNode> getCompleteMap(ApplicationModel model) {
        var all = new HashMap<ArtifactKey, DepNode>();
        var root = new DepNode(0, model.getAppArtifact());
        all.put(model.getAppArtifact().getKey(), root);
        int i = 1;
        for (var d : model.getDependencies()) {
            all.put(d.getKey(), new DepNode(i++, d));
        }
        return all;
    }

    /**
     * Path from a target to the root
     */
    private static class DepPath implements Comparable<DepPath> {
        final Set<Integer> ids;
        final List<DepNode> path;

        DepPath() {
            ids = new HashSet<>();
            path = new ArrayList<>();
        }

        DepPath(DepPath original) {
            ids = new HashSet<>(original.ids);
            path = new ArrayList<>(original.path);
        }

        boolean addNode(DepNode node) {
            if (!ids.add(node.id)) {
                return false;
            }
            path.add(node);
            return true;
        }

        @Override
        public DepPath clone() {
            return new DepPath(this);
        }

        @Override
        public int compareTo(DepPath o) {
            return path.size() - o.path.size();
        }

        @Override
        public String toString() {
            var sb = new StringBuilder();
            var i = path.iterator();
            sb.append(i.next().resolvedDep.getArtifactId());
            while (i.hasNext()) {
                sb.append(" -> ").append(i.next().resolvedDep.getArtifactId());
            }
            return sb.toString();
        }
    }

    /**
     * Dependency node with links to its dependents
     */
    private static class DepNode {
        final int id;
        final ResolvedDependency resolvedDep;
        List<DepNode> dependents = List.of();

        private DepNode(int id, ResolvedDependency resolvedDep) {
            this.id = id;
            this.resolvedDep = resolvedDep;
        }

        void initDependents(Map<ArtifactKey, DepNode> allDeps) {
            for (var depCoords : resolvedDep.getDependencies()) {
                var dep = allDeps.get(depCoords.getKey());
                if (dep == null) {
                    // TODO error/warning
                } else {
                    dep.addDependent(this);
                }
            }
        }

        private void addDependent(DepNode dependent) {
            if (dependents.isEmpty()) {
                dependents = new ArrayList<>();
            }
            dependents.add(dependent);
        }
    }
}
