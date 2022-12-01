package io.quarkus.hibernate.orm.panache.common.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

final class PanacheNamedQueryEntityClassBuildStep extends MultiBuildItem {
    private String className;
    private Set<String> namedQueries;

    public PanacheNamedQueryEntityClassBuildStep(String className, Set<String> namedQueries) {
        this.className = className;
        this.namedQueries = namedQueries;
    }

    public String getClassName() {
        return this.className;
    }

    public Set<String> getNamedQueries() {
        return namedQueries;
    }
}
