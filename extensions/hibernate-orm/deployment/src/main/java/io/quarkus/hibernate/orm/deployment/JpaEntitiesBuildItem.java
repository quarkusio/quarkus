package io.quarkus.hibernate.orm.deployment;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Internal model to represent which objects are likely needing enhancement
 * via HibernateEntityEnhancer.
 */
public final class JpaEntitiesBuildItem extends SimpleBuildItem {

    private final Set<String> entityClassNames = new HashSet<String>();
    private final Set<String> allModelClassNames = new HashSet<String>();

    void addEntityClass(final String className) {
        entityClassNames.add(className);
        allModelClassNames.add(className);
    }

    void addModelClass(final String className) {
        allModelClassNames.add(className);
    }

    void registerAllForReflection(final BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        for (String className : allModelClassNames) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
        }
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
