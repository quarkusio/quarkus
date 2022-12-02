package io.quarkus.hibernate.orm.deployment;

import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides configuration specific to a persistence unit and necessary to build the JPA model.
 */
public final class JpaModelPersistenceUnitContributionBuildItem extends MultiBuildItem {

    public final String persistenceUnitName;
    public final URL persistenceUnitRootURL;
    public final Set<String> explicitlyListedClassNames;
    public final Set<String> explicitlyListedMappingFiles;

    public JpaModelPersistenceUnitContributionBuildItem(String persistenceUnitName,
            URL persistenceUnitRootURL, Collection<String> explicitlyListedClassNames,
            Collection<String> explicitlyListedMappingFiles) {
        this.persistenceUnitName = persistenceUnitName;
        this.persistenceUnitRootURL = persistenceUnitRootURL;
        this.explicitlyListedClassNames = new TreeSet<>(explicitlyListedClassNames);
        this.explicitlyListedMappingFiles = new TreeSet<>(explicitlyListedMappingFiles);
    }

}
