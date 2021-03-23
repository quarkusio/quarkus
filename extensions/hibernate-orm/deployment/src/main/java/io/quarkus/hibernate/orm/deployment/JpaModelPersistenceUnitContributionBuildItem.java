package io.quarkus.hibernate.orm.deployment;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides configuration specific to a persistence unit and necessary to build the JPA model.
 */
public final class JpaModelPersistenceUnitContributionBuildItem extends MultiBuildItem {

    public final String persistenceUnitName;
    public final Set<String> explicitlyListedClassNames;
    public final Set<String> explicitlyListedMappingFiles;

    public JpaModelPersistenceUnitContributionBuildItem(String persistenceUnitName,
            Collection<String> explicitlyListedClassNames,
            Collection<String> explicitlyListedMappingFiles) {
        this.persistenceUnitName = persistenceUnitName;
        this.explicitlyListedClassNames = new TreeSet<>(explicitlyListedClassNames);
        this.explicitlyListedMappingFiles = new TreeSet<>(explicitlyListedMappingFiles);
    }

}
