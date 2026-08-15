package io.quarkus.hibernate.orm.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class JpaModelPerPersistenceUnitBuildItem extends SimpleBuildItem {
    private final Map<String, JpaPersistenceUnitModel> modelPerPersistenceUnit;

    public JpaModelPerPersistenceUnitBuildItem(Map<String, JpaPersistenceUnitModel> modelPerPersistenceUnit) {
        this.modelPerPersistenceUnit = modelPerPersistenceUnit;
    }

    public Map<String, JpaPersistenceUnitModel> getModelPerPersistenceUnit() {
        return modelPerPersistenceUnit;
    }
}
