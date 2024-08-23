package io.quarkus.hibernate.orm.panache.common.deployment;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

final class PanacheNamedQueryEntityClassBuildStep extends MultiBuildItem {
    private String className;
    private Map<String, String> namedQueries;

    public PanacheNamedQueryEntityClassBuildStep(String className, Map<String, String> namedQueries) {
        this.className = className;
        this.namedQueries = namedQueries;
    }

    public String getClassName() {
        return this.className;
    }

    public Map<String, String> getNamedQueries() {
        return namedQueries;
    }
}
