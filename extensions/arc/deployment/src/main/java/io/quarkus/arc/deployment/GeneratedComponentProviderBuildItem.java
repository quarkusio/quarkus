package io.quarkus.arc.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains the names of the generated implementations of {@link io.quarkus.arc.ComponentsProvider}
 */
public final class GeneratedComponentProviderBuildItem extends SimpleBuildItem {

    private final List<String> implNames;

    public GeneratedComponentProviderBuildItem(List<String> implNames) {
        this.implNames = implNames;
    }

    public List<String> getImplNames() {
        return implNames;
    }
}
