package io.quarkus.deployment.builditem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

public final class AdditionalIndexedClassesBuildItem extends MultiBuildItem {

    private final Set<String> classesToIndex = new HashSet<>();

    public AdditionalIndexedClassesBuildItem(String... classesToIndex) {
        this.classesToIndex.addAll(Arrays.asList(classesToIndex));
    }

    public Set<String> getClassesToIndex() {
        return classesToIndex;
    }
}
