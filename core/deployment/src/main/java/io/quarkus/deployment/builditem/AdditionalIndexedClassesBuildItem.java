package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows extensions to add classes to the index available via {@link CombinedIndexBuildItem}
 * The classes are loaded by the Deployment ClassLoader
 */
public final class AdditionalIndexedClassesBuildItem extends MultiBuildItem {

    private final Set<String> classesToIndex;

    public AdditionalIndexedClassesBuildItem(String... classesToIndex) {
        this.classesToIndex = Set.of(classesToIndex);
    }

    public AdditionalIndexedClassesBuildItem(String classToIndex) {
        this.classesToIndex = Collections.singleton(classToIndex);
    }

    public Set<String> getClassesToIndex() {
        return classesToIndex;
    }
}
