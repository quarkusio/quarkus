package io.quarkus.hibernate.orm.panache.deployment;

import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;

final class NamedQueryEntityClassBuildStep extends MultiBuildItem {
    private String className;
    private Set<String> namedQueries;

    public NamedQueryEntityClassBuildStep(String className, Set<String> namedQueries) {
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
