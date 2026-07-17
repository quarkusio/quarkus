package io.quarkus.deployment.pkg.builditem;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.ResolvedDependency;

/**
 * Build item that holds the results of dependency usage analysis for tree shaking.
 * Contains whether class-level tree shaking was performed, the set of reachable class names (dot-separated),
 * and the sorted list of removed class resource paths per dependency.
 */
public final class JarTreeShakeBuildItem extends SimpleBuildItem {

    private final boolean classesShaken;
    private final Set<String> reachableClassNames;
    private final Map<ArtifactKey, List<String>> removedClasses;
    private final Set<String> referencedJdkPackages;

    public JarTreeShakeBuildItem(boolean classesShaken, Set<String> reachableClassNames,
            Map<ArtifactKey, List<String>> removedClasses, Set<String> referencedJdkPackages) {
        this.classesShaken = classesShaken;
        this.reachableClassNames = reachableClassNames;
        this.removedClasses = removedClasses;
        this.referencedJdkPackages = referencedJdkPackages;
    }

    public boolean isClassesShaken() {
        return classesShaken;
    }

    /**
     * @return dot-separated class names of all reachable classes
     */
    public Set<String> getReachableClassNames() {
        return reachableClassNames;
    }

    /**
     * @return sorted list of removed class resource paths (e.g. "com/example/Foo.class") per dependency
     */
    public Map<ArtifactKey, List<String>> getRemovedClasses() {
        return removedClasses;
    }

    /**
     * Collects JAR entry paths of unreachable classes from a dependency into the given set.
     * Handles multi-release JAR entries and preserves inner classes whose outer class is reachable.
     *
     * @param dep the dependency to scan
     * @param removedEntries the set to add removed entry paths to
     */
    public void collectUnreachableEntries(ResolvedDependency dep, Set<String> removedEntries) throws IOException {
        if (!classesShaken) {
            return;
        }
        try (var pathTree = dep.getContentTree().open()) {
            pathTree.walkRaw(visit -> {
                String rel = visit.getRelativePath("/");
                String classRel = rel;
                if (rel.startsWith("META-INF/versions/")) {
                    String afterVersions = rel.substring("META-INF/versions/".length());
                    int slash = afterVersions.indexOf('/');
                    if (slash > 0) {
                        classRel = afterVersions.substring(slash + 1);
                    } else {
                        return;
                    }
                }
                if (ClassLoaderHelper.isClassEntry(classRel)) {
                    String className = classRel.substring(0, classRel.length() - 6).replace('/', '.');
                    if (!reachableClassNames.contains(className)) {
                        int dollarIdx = className.indexOf('$');
                        if (dollarIdx < 0
                                || !reachableClassNames.contains(className.substring(0, dollarIdx))) {
                            removedEntries.add(rel);
                        }
                    }
                }
            });
        }
    }

    /**
     * @return dot-separated JDK package names (java.*, javax.*, jdk.*) referenced by reachable code
     */
    public Set<String> getReferencedJdkPackages() {
        return referencedJdkPackages;
    }

    /**
     * Computes a pedigree string for the given dependency describing what was removed.
     *
     * @return pedigree text or {@code null} if nothing was removed
     */
    public String computePedigree(ArtifactKey depKey) {
        if (!classesShaken) {
            return null;
        }
        List<String> removed = removedClasses.getOrDefault(depKey, Collections.emptyList());
        if (removed.isEmpty()) {
            return null;
        }
        var sb = new StringBuilder("Removed ");
        sb.append(removed.get(0));
        for (int i = 1; i < removed.size(); ++i) {
            sb.append(",").append(removed.get(i));
        }
        return sb.toString();
    }
}
