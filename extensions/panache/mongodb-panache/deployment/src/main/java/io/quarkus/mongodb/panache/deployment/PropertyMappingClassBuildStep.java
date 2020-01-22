package io.quarkus.mongodb.panache.deployment;

import io.quarkus.builder.item.MultiBuildItem;

final class PropertyMappingClassBuildStep extends MultiBuildItem {
    private String className;
    private String aliasClassName;

    public PropertyMappingClassBuildStep(String className) {
        this.className = className;
    }

    public PropertyMappingClassBuildStep(String className, String aliasClassName) {
        this.className = className;
        this.aliasClassName = aliasClassName;
    }

    public String getClassName() {
        return this.className;
    }

    public String getAliasClassName() {
        return aliasClassName;
    }
}
