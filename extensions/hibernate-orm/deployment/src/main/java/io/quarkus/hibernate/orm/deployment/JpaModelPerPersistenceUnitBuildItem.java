package io.quarkus.hibernate.orm.deployment;

import java.util.Map;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class JpaModelPerPersistenceUnitBuildItem extends SimpleBuildItem {
    private final Map<String, JpaPersistenceUnitModel> modelPerPersistenceUnit;
    private final Set<String> persistenceUnitsConfiguredThroughQuarkusConfiguration;
    private final Set<String> persistenceUnitsConfiguredThroughPackageLevelAnnotations;
    private final Set<String> persistenceUnitsConfiguredThroughClassLevelAnnotations;

    public JpaModelPerPersistenceUnitBuildItem(Map<String, JpaPersistenceUnitModel> modelPerPersistenceUnit,
            Set<String> persistenceUnitsConfiguredThroughQuarkusConfiguration,
            Set<String> persistenceUnitsConfiguredThroughPackageLevelAnnotations,
            Set<String> persistenceUnitsConfiguredThroughClassLevelAnnotations) {
        this.modelPerPersistenceUnit = modelPerPersistenceUnit;
        this.persistenceUnitsConfiguredThroughQuarkusConfiguration = persistenceUnitsConfiguredThroughQuarkusConfiguration;
        this.persistenceUnitsConfiguredThroughPackageLevelAnnotations = persistenceUnitsConfiguredThroughPackageLevelAnnotations;
        this.persistenceUnitsConfiguredThroughClassLevelAnnotations = persistenceUnitsConfiguredThroughClassLevelAnnotations;
    }

    public Map<String, JpaPersistenceUnitModel> getModelPerPersistenceUnit() {
        return modelPerPersistenceUnit;
    }

    public Set<String> getPersistenceUnitsConfiguredThroughQuarkusConfiguration() {
        return persistenceUnitsConfiguredThroughQuarkusConfiguration;
    }

    public Set<String> getPersistenceUnitsConfiguredThroughPackageLevelAnnotations() {
        return persistenceUnitsConfiguredThroughPackageLevelAnnotations;
    }

    public Set<String> getPersistenceUnitsConfiguredThroughClassLevelAnnotations() {
        return persistenceUnitsConfiguredThroughClassLevelAnnotations;
    }
}
