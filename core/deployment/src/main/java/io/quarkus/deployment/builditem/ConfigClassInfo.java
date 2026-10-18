package io.quarkus.deployment.builditem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

/**
 * Common interface for build items representing a discovered configuration class, including
 * its Jandex {@link ClassInfo}, configuration prefix, and the set of bean types (the class itself
 * and all its super-interfaces) collected from the index.
 *
 * @see ConfigMappingBuildItem
 * @see ConfigPropertiesBuildItem
 */
public interface ConfigClassInfo {
    ClassInfo getConfigClass();

    default String getConfigClassName() {
        return getConfigClass().name().toString();
    }

    String getPrefix();

    Set<Type> getTypes();

    static Set<Type> collectTypes(ClassInfo configClass, IndexView index) {
        if (configClass.interfaceNames().isEmpty()) {
            return Set.of(Type.create(configClass.name(), Type.Kind.CLASS));
        }

        Set<DotName> interfaceNames = new HashSet<>();
        interfaceNames.add(configClass.name());
        collectTypes(configClass, interfaceNames, index);
        Set<Type> result = new HashSet<>(interfaceNames.size());
        for (DotName interfaceName : interfaceNames) {
            result.add(Type.create(interfaceName, Type.Kind.CLASS));
        }
        return result;
    }

    private static void collectTypes(ClassInfo current, Set<DotName> result, IndexView index) {
        List<DotName> interfaceNames = current.interfaceNames();
        if (interfaceNames.isEmpty()) {
            return;
        }
        for (DotName interfaceName : interfaceNames) {
            ClassInfo classByName = index.getClassByName(interfaceName);
            if (classByName == null) {
                continue; // just ignore this type
            }
            result.add(interfaceName);
            collectTypes(classByName, result, index);
        }
    }
}
