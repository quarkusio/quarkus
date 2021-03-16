package io.quarkus.panache.common.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item to indicate that those classes are Panache-enhanced and will get
 * getters/setters generated for public fields, even if they're not visible in the index.
 */
public final class PanacheEntityClassesBuildItem extends MultiBuildItem {

    private final Set<String> entityClasses;

    public PanacheEntityClassesBuildItem(Set<String> entityClasses) {
        this.entityClasses = entityClasses;
    }

    public Set<String> getEntityClasses() {
        return entityClasses;
    }
}
