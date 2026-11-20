package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.hasWinner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.collection.DependencyGraphTransformationContext;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

import io.quarkus.maven.dependency.ArtifactKey;

/**
 * Dependency tree conflict resolver.
 * <p>
 * The idea is to have a more efficient implementation than the
 * {@link org.eclipse.aether.util.graph.transformer.ConflictIdSorter#transformGraph(DependencyNode, DependencyGraphTransformationContext)}
 * for the use-cases the Quarkus deployment dependency resolver is designed for.
 * <p>
 * Specifically, this conflict resolver does not properly handle version ranges, that are not expected to be present in the
 * phase it used.
 */
class DependencyTreeConflictResolver {

    /**
     * Dependency node data key marking nodes of activated conditional dependencies.
     */
    private static final String CONDITIONAL_DEPENDENCY = "quarkus.conditional";

    /**
     * Marks a dependency node as a node of a conditional dependency. If the parent of a conditional dependency node
     * loses version conflict resolution, the conditional dependency node will be moved to the winning parent node
     * instead of being dropped with the rest of the children of the losing parent node.
     *
     * @param node conditional dependency node
     */
    static void markConditionalDependency(DependencyNode node) {
        node.setData(CONDITIONAL_DEPENDENCY, Boolean.TRUE);
    }

    private static boolean isConditionalDependency(DependencyNode node) {
        return node.getData().get(CONDITIONAL_DEPENDENCY) != null;
    }

    /**
     * Resolves dependency version conflicts in the given dependency tree.
     *
     * @param root the root of the dependency tree
     */
    static void resolveConflicts(DependencyNode root) {
        new DependencyTreeConflictResolver(root).run();
    }

    final OrderedDependencyVisitor visitor;
    private final Map<ArtifactKey, VisitedDependency> visited = new HashMap<>();
    /**
     * Conditional dependencies of losing nodes that should be moved to the winning nodes
     */
    private final Deque<RelocatedConditionalDependencies> relocated = new ArrayDeque<>();

    private DependencyTreeConflictResolver(DependencyNode root) {
        visitor = new OrderedDependencyVisitor(root);
    }

    private void run() {
        visitor.next();// skip the root
        while (visitor.hasNext()) {
            var node = visitor.next();
            if (!hasWinner(node)) {
                resolveConflict(node, visitor.getSubtreeIndex());
            }
        }
        // move conditional dependencies of the losing nodes to the winning ones and resolve conflicts in their subtrees
        // giving priority to the nodes that have already been visited
        while (!relocated.isEmpty()) {
            var r = relocated.removeFirst();
            // children lists may be shared between nodes, in which case a conditional dependency of a losing node
            // could already be present in the tree, it's relocated only if it hasn't been visited
            var deps = new ArrayList<DependencyNode>(r.deps.size());
            for (var dep : r.deps) {
                if (!visited.containsKey(getKey(dep.getArtifact()))) {
                    deps.add(dep);
                }
            }
            if (deps.isEmpty()) {
                continue;
            }
            var winnerChildren = r.winner.node.getChildren();
            var children = new ArrayList<DependencyNode>(winnerChildren.size() + deps.size());
            children.addAll(winnerChildren);
            children.addAll(deps);
            r.winner.node.setChildren(children);
            for (var dep : deps) {
                var subtreeVisitor = new OrderedDependencyVisitor(dep);
                while (subtreeVisitor.hasNext()) {
                    var node = subtreeVisitor.next();
                    if (!hasWinner(node)) {
                        resolveConflict(node, r.winner.subtreeIndex);
                    }
                }
            }
        }
    }

    private void resolveConflict(DependencyNode node, int subtreeIndex) {
        final ArtifactKey key = getKey(node.getArtifact());
        var prev = visited.get(key);
        if (prev == null) {
            visited.put(key, new VisitedDependency(node, subtreeIndex));
        } else if (prev.node != node) {
            prev.resolveConflict(node, subtreeIndex);
        }
    }

    private class VisitedDependency {
        final DependencyNode node;
        final int subtreeIndex;

        private VisitedDependency(DependencyNode node, int subtreeIndex) {
            this.node = node;
            this.subtreeIndex = subtreeIndex;
        }

        private void resolveConflict(DependencyNode otherNode, int otherSubtreeIndex) {
            if (subtreeIndex != otherSubtreeIndex) {
                final Dependency currentDep = node.getDependency();
                final Dependency otherDep = otherNode.getDependency();
                if (!currentDep.getScope().equals(otherDep.getScope())
                        && getScopePriority(currentDep.getScope()) > getScopePriority(otherDep.getScope())) {
                    node.setScope(otherDep.getScope());
                }
                if (currentDep.isOptional() && !otherDep.isOptional()) {
                    node.setOptional(false);
                }
            }
            List<DependencyNode> conditionalDeps = null;
            for (var child : otherNode.getChildren()) {
                if (isConditionalDependency(child)) {
                    if (conditionalDeps == null) {
                        conditionalDeps = new ArrayList<>(1);
                    }
                    conditionalDeps.add(child);
                }
            }
            if (conditionalDeps != null) {
                relocated.add(new RelocatedConditionalDependencies(this, conditionalDeps));
            }
            otherNode.setChildren(List.of());
            otherNode.setData(ConflictResolver.NODE_DATA_WINNER, new DefaultDependencyNode(node.getDependency()));
        }
    }

    private record RelocatedConditionalDependencies(VisitedDependency winner, List<DependencyNode> deps) {
    }

    private static int getScopePriority(String scope) {
        return switch (scope) {
            case JavaScopes.COMPILE -> 0;
            case JavaScopes.RUNTIME -> 1;
            case JavaScopes.PROVIDED -> 2;
            case JavaScopes.TEST -> 3;
            default -> 4;
        };
    }
}
