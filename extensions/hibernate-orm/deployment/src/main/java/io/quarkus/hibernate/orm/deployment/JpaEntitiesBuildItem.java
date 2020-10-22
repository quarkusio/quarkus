package io.quarkus.hibernate.orm.deployment;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;

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

    void registerAllForReflection(final BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod,
            final BuildProducer<ReflectiveClassBuildItem> reflectiveClass, IndexView indexView) {
        for (String className : allModelClassNames) {
            ClassInfo clazz = indexView.getClassByName(DotName.createSimple(className));
            if (clazz != null) {
                //we only want to register the methods we know about
                //the transformation adds lots of new methods
                //these don't need to be registered
                for (MethodInfo method : clazz.methods()) {
                    reflectiveMethod.produce(new ReflectiveMethodBuildItem(method));
                }
                reflectiveClass.produce(new ReflectiveClassBuildItem(false, true, className));
            } else {
                //if we can't do the index lookup register all methods
                //this should not happen
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, className));
            }
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
