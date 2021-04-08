package io.quarkus.deployment.builditem;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalIndexedClassesBuildItem extends MultiBuildItem {

    private final Set<String> classesToIndex;

    public AdditionalIndexedClassesBuildItem(String... classesToIndex) {
        Set<String> toIndex = new HashSet<>(classesToIndex.length);
        for (String s : classesToIndex) {
            toIndex.add(s);
        }
        this.classesToIndex = toIndex;
    }

    public AdditionalIndexedClassesBuildItem(String classToIndex) {
        this.classesToIndex = Collections.singleton(classToIndex);
    }

    public Set<String> getClassesToIndex() {
        return classesToIndex;
    }
}
