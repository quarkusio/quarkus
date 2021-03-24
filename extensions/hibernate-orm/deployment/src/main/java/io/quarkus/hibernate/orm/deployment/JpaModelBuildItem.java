package io.quarkus.hibernate.orm.deployment;

import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Entity;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Internal model to represent which objects are likely needing enhancement
 * via HibernateEntityEnhancer.
 */
public final class JpaModelBuildItem extends SimpleBuildItem {

    private final Set<String> allModelPackageNames = new TreeSet<>();
    private final Set<String> entityClassNames = new TreeSet<>();
    private final Set<String> allModelClassNames = new TreeSet<>();

    public JpaModelBuildItem(Set<String> allModelPackageNames, Set<String> entityClassNames,
            Set<String> allModelClassNames) {
        this.allModelPackageNames.addAll(allModelPackageNames);
        this.entityClassNames.addAll(entityClassNames);
        this.allModelClassNames.addAll(allModelClassNames);
    }

    /**
     * @return the list of packages annotated with a JPA annotation.
     */
    public Set<String> getAllModelPackageNames() {
        return allModelPackageNames;
    }

    /**
     * @return the list of entities (i.e. classes marked with {@link Entity})
     */
    public Set<String> getEntityClassNames() {
        return entityClassNames;
    }

    /**
     * @return the list of all model class names: entities, mapped super classes...
     */
    public Set<String> getAllModelClassNames() {
        return allModelClassNames;
    }
}
