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

    private final Deque<List<DependencyNode>> stack = new ArrayDeque<>();
    private List<DependencyNode> currentList;
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
        currentList = List.of(root);
    }

    /**
     * Current dependency.
     *
     * @return current dependency
     */
    DependencyNode getCurrent() {
        ensureNonNegativeIndex();
        return currentList.get(currentIndex);
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
                || currentIndex + 1 < currentList.size()
                || !currentList.get(currentIndex).getChildren().isEmpty();
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
            var children = currentList.get(currentIndex).getChildren();
            if (!children.isEmpty()) {
                stack.addLast(children);
                totalOnNextDistance += children.size();
            }
            if (--totalOnCurrentDistance == 0) {
                ++currentDistance;
                totalOnCurrentDistance = totalOnNextDistance;
                totalOnNextDistance = 0;
            }
        }
        if (++currentIndex == currentList.size()) {
            currentList = stack.removeFirst();
            currentIndex = 0;
        }
        return currentList.get(currentIndex);
    }

    /**
     * Replaces the current dependency in the tree with the argument.
     *
     * @param newNode dependency node that should replace the current one in the tree
     */
    void replaceCurrent(DependencyNode newNode) {
        currentList.set(currentIndex, newNode);
    }
}
