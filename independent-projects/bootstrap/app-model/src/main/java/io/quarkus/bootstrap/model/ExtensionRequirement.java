package io.quarkus.bootstrap.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Represents a capability requirement from an extension in the dependency graph.
 *
 * <p>
 * Each requirement records the capability name, the artifact key of the extension
 * that requires it, and the extension's position in the dependency graph. The position
 * is represented as a graph path: the sequence of local child indices from the root
 * to the requiring extension. This path uniquely identifies the node's position and
 * determines priority when multiple unsatisfied requirements compete for resolution.
 *
 * <p>
 * The graph path is computed lazily — only when the requirement is compared against
 * another requirement during priority sorting.
 *
 * <p>
 * This class implements {@link Comparable} with ordering by depth (path length) first,
 * then by path content. A shorter path (shallower node) always precedes a longer one.
 * Among paths of the same length, the one that is lexicographically smaller comes first,
 * matching breadth-first visit order.
 */
class ExtensionRequirement implements Comparable<ExtensionRequirement> {

    private final String capabilityName;
    private final String extensionArtifactKey;
    private Supplier<int[]> bfsPathSupplier;
    private int[] bfsPath;

    ExtensionRequirement(String capabilityName, String extensionArtifactKey,
            Supplier<int[]> bfsPathSupplier) {
        this.capabilityName = Objects.requireNonNull(capabilityName, "capabilityName");
        this.extensionArtifactKey = Objects.requireNonNull(extensionArtifactKey, "extensionArtifactKey");
        this.bfsPathSupplier = Objects.requireNonNull(bfsPathSupplier, "bfsPathSupplier");
    }

    String getCapabilityName() {
        return capabilityName;
    }

    String getExtensionArtifactKey() {
        return extensionArtifactKey;
    }

    private int[] getGraphPath() {
        if (bfsPath == null) {
            bfsPath = bfsPathSupplier.get();
            bfsPathSupplier = null;
        }
        return bfsPath;
    }

    /**
     * Compares by depth (path length) first, then by path content for same-depth nodes.
     * Shallower nodes always have higher priority. Among same-depth nodes, the one
     * visited earlier in breadth-first order wins.
     */
    @Override
    public int compareTo(ExtensionRequirement other) {
        final int[] thisPath = this.getGraphPath();
        final int[] otherPath = other.getGraphPath();
        int cmp = Integer.compare(thisPath.length, otherPath.length);
        if (cmp != 0) {
            return cmp;
        }
        for (int i = 0; i < thisPath.length; i++) {
            cmp = Integer.compare(thisPath[i], otherPath[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ExtensionRequirement{capability=" + capabilityName
                + ", extension=" + extensionArtifactKey
                + ", bfsPath=" + Arrays.toString(getGraphPath()) + "}";
    }
}
