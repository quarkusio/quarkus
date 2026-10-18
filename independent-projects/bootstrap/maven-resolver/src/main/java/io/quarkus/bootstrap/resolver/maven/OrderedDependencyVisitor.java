package io.quarkus.bootstrap.resolver.maven;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.aether.graph.DependencyNode;

/**
 * Walks a dependency tree by visiting dependencies in the order of their priorities
 * from the perspective of version conflict resolution.
 */
class OrderedDependencyVisitor {

    private final Deque<DependencyList> stack = new ArrayDeque<>();
    private DependencyList currentList;
    private int currentIndex = -1;
    private int currentDistance;
    private int totalOnCurrentDistance = 1;
    private int totalOnNextDistance;

    /**
     * The root of the dependency tree
     *
     * @param root the root of the dependency tree
     */
    OrderedDependencyVisitor(DependencyNode root) {
        currentList = new DependencyList(0, List.of(root));
    }

    /**
     * Current dependency.
     *
     * @return current dependency
     */
    DependencyNode getCurrent() {
        ensureNonNegativeIndex();
        return currentList.deps.get(currentIndex);
    }

    /**
     * Returns the current distance (depth) from the root to the level on which the current node is.
     *
     * @return current depth
     */
    int getCurrentDistance() {
        ensureNonNegativeIndex();
        return currentDistance;
    }

    private void ensureNonNegativeIndex() {
        if (currentIndex < 0) {
            throw new RuntimeException("The visitor has not been positioned on the first dependency node yet");
        }
    }

    /**
     * Whether there are still not visited dependencies.
     *
     * @return true if there are still not visited dependencies, otherwise - false
     */
    boolean hasNext() {
        return !stack.isEmpty()
                || currentIndex + 1 < currentList.deps.size()
                || !currentList.deps.get(currentIndex).getChildren().isEmpty();
    }

    /**
     * Returns the next dependency.
     *
     * @return the next dependency
     */
    DependencyNode next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        if (currentIndex >= 0) {
            var children = currentList.deps.get(currentIndex).getChildren();
            if (!children.isEmpty()) {
                stack.addLast(new DependencyList(getSubtreeIndexForChildren(), children));
                totalOnNextDistance += children.size();
            }
            if (--totalOnCurrentDistance == 0) {
                ++currentDistance;
                totalOnCurrentDistance = totalOnNextDistance;
                totalOnNextDistance = 0;
            }
        }
        if (++currentIndex == currentList.deps.size()) {
            currentList = stack.removeFirst();
            currentIndex = 0;
        }
        return currentList.deps.get(currentIndex);
    }

    private int getSubtreeIndexForChildren() {
        return currentDistance < 2 ? currentIndex + 1 : currentList.subtreeIndex;
    }

    /**
     * A dependency subtree index the current dependency belongs to.
     *
     * <p>
     * A dependency subtree index is an index of a direct dependency of the root of the dependency tree
     * from which the dependency subtree originates. All the dependencies from a subtree that originates
     * from a direct dependency of the root of the dependency tree will share the same subtree index.
     *
     * <p>
     * A dependency subtree index starts from {@code 1}. An exception is the root of the dependency tree,
     * which will have the subtree index of {@code 0}.
     *
     * @return dependency subtree index the current dependency belongs to
     */
    int getSubtreeIndex() {
        return currentDistance == 0 ? 0 : (currentDistance < 2 ? currentIndex + 1 : currentList.subtreeIndex);
    }

    /**
     * Replaces the current dependency in the tree with the argument.
     *
     * @param newNode dependency node that should replace the current one in the tree
     */
    void replaceCurrent(DependencyNode newNode) {
        currentList.deps.set(currentIndex, newNode);
    }

    /**
     * A list of dependencies that are children of a {@link DependencyNode}
     * that are associated with a dependency subtree index.
     */
    private static class DependencyList {

        private final int subtreeIndex;
        private final List<DependencyNode> deps;

        public DependencyList(int branchIndex, List<DependencyNode> deps) {
            this.subtreeIndex = branchIndex;
            this.deps = deps;
        }
    }
}
