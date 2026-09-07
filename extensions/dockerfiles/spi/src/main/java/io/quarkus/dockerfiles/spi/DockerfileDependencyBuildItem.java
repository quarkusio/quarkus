package io.quarkus.dockerfiles.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that allows extensions to specify OS packages that need to be installed in generated Dockerfiles.
 */
public final class DockerfileDependencyBuildItem extends MultiBuildItem {

    public static class DistributionMapping {
        private final Map<Distribution, String> packageNames = new HashMap<>();
        private Set<DockerfileKind> kinds = Set.of(DockerfileKind.JVM, DockerfileKind.NATIVE);

        public DistributionMapping forDistribution(Distribution distribution, String packageName) {
            this.packageNames.put(distribution, packageName);
            return this;
        }

        public DistributionMapping forKinds(DockerfileKind... kinds) {
            this.kinds = Set.of(kinds);
            return this;
        }

        public DistributionMapping forKinds(Set<DockerfileKind> kinds) {
            this.kinds = Objects.requireNonNull(kinds, "kinds must not be null");
            return this;
        }

        public DockerfileDependencyBuildItem build() {
            return new DockerfileDependencyBuildItem(packageNames, kinds);
        }
    }

    private final Map<Distribution, String> packageNames;
    private final Set<DockerfileKind> kinds;

    // Backward compatible constructors
    public DockerfileDependencyBuildItem(String packageName) {
        this(packageName, Set.of(DockerfileKind.JVM, DockerfileKind.NATIVE));
    }

    public DockerfileDependencyBuildItem(String packageName, DockerfileKind kind) {
        this(packageName, Set.of(kind));
    }

    public DockerfileDependencyBuildItem(String packageName, DockerfileKind... kinds) {
        this(packageName, Set.of(kinds));
    }

    public DockerfileDependencyBuildItem(String packageName, Set<DockerfileKind> kinds) {
        // Universal package name - create mapping for all distributions
        Objects.requireNonNull(packageName, "packageName must not be null");
        this.packageNames = createUniversalMapping(packageName);
        this.kinds = Objects.requireNonNull(kinds, "kinds must not be null");
        if (kinds.isEmpty()) {
            throw new IllegalArgumentException("kinds must not be empty");
        }
    }

    // New constructor for distribution-specific packages
    private DockerfileDependencyBuildItem(Map<Distribution, String> packageNames, Set<DockerfileKind> kinds) {
        this.packageNames = Map.copyOf(packageNames);
        this.kinds = Objects.requireNonNull(kinds, "kinds must not be null");
        if (kinds.isEmpty()) {
            throw new IllegalArgumentException("kinds must not be empty");
        }
        if (packageNames.isEmpty()) {
            throw new IllegalArgumentException("packageNames must not be empty");
        }
    }

    public static DistributionMapping forDistribution(Distribution distribution, String packageName) {
        return new DistributionMapping().forDistribution(distribution, packageName);
    }

    private static Map<Distribution, String> createUniversalMapping(String packageName) {
        Map<Distribution, String> mapping = new HashMap<>();
        for (Distribution distribution : Distribution.values()) {
            if (distribution != Distribution.UNKNOWN) {
                mapping.put(distribution, packageName);
            }
        }
        return mapping;
    }

    public Optional<String> getPackageNameFor(Distribution distribution) {
        return Optional.ofNullable(packageNames.get(distribution));
    }

    public Set<DockerfileKind> getKinds() {
        return kinds;
    }

    public Map<Distribution, String> getPackageNames() {
        return packageNames;
    }

    /**
     * Check if this dependency applies to the given kind.
     *
     * @param kind the kind to check
     * @return true if this dependency applies to the given kind
     */
    public boolean appliesTo(DockerfileKind kind) {
        return this.kinds.contains(kind);
    }

    /**
     * Check if this dependency applies to the given distribution.
     *
     * @param distribution the distribution to check
     * @return true if this dependency has a package name for the given distribution
     */
    public boolean appliesTo(Distribution distribution) {
        return packageNames.containsKey(distribution);
    }

    /**
     * Check if this dependency applies to the given kind and distribution.
     *
     * @param kind the kind to check
     * @param distribution the distribution to check
     * @return true if this dependency applies to both the kind and distribution
     */
    public boolean appliesTo(DockerfileKind kind, Distribution distribution) {
        return appliesTo(kind) && appliesTo(distribution);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DockerfileDependencyBuildItem that = (DockerfileDependencyBuildItem) o;
        return Objects.equals(packageNames, that.packageNames) &&
                Objects.equals(kinds, that.kinds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageNames, kinds);
    }

    @Override
    public String toString() {
        return "DockerfileDependencyBuildItem{packageNames=" + packageNames + ", kinds=" + kinds + '}';
    }
}