package io.quarkus.bootstrap.resolver.maven;

import static io.quarkus.bootstrap.util.DependencyUtils.getKey;
import static io.quarkus.bootstrap.util.DependencyUtils.hasWinner;

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
     * Resolves dependency version conflicts in the given dependency tree.
     *
     * @param root the root of the dependency tree
     */
    static void resolveConflicts(DependencyNode root) {
        new DependencyTreeConflictResolver(root).run();
    }

    final OrderedDependencyVisitor visitor;

    private DependencyTreeConflictResolver(DependencyNode root) {
        visitor = new OrderedDependencyVisitor(root);
    }

    private void run() {
        visitor.next();// skip the root
        final Map<ArtifactKey, VisitedDependency> visited = new HashMap<>();
        while (visitor.hasNext()) {
            var node = visitor.next();
            if (!hasWinner(node)) {
                visited.compute(getKey(node.getArtifact()), this::resolveConflict);
            }
        }
    }

    private VisitedDependency resolveConflict(ArtifactKey key, VisitedDependency prev) {
        if (prev == null) {
            return new VisitedDependency(visitor);
        }
        prev.resolveConflict(visitor);
        return prev;
    }

    private static class VisitedDependency {
        final DependencyNode node;
        final int subtreeIndex;

        private VisitedDependency(OrderedDependencyVisitor visitor) {
            this.node = visitor.getCurrent();
            this.subtreeIndex = visitor.getSubtreeIndex();
        }

        private void resolveConflict(OrderedDependencyVisitor visitor) {
            var otherNode = visitor.getCurrent();
            if (subtreeIndex != visitor.getSubtreeIndex()) {
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
            otherNode.setChildren(List.of());
            otherNode.setData(ConflictResolver.NODE_DATA_WINNER, new DefaultDependencyNode(node.getDependency()));
        }
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
